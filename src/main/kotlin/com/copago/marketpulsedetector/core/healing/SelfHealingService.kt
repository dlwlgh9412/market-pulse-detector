package com.copago.marketpulsedetector.core.healing

import com.copago.marketpulsedetector.core.component.HtmlFetcher
import com.copago.marketpulsedetector.core.healing.enums.SelectorObjective
import com.copago.marketpulsedetector.domain.entity.CrawlExtractionRuleChangeHistoryEntity
import com.copago.marketpulsedetector.domain.entity.CrawlExtractionRuleEntity
import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import com.copago.marketpulsedetector.domain.repository.CrawlExtractionRuleChangeHistoryRepository
import com.copago.marketpulsedetector.domain.repository.CrawlExtractionRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlPageRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SelfHealingService(
    private val targetRepository: CrawlTargetRepository,
    private val pageRuleRepository: CrawlPageRuleRepository,
    private val extractionRuleRepository: CrawlExtractionRuleRepository,
    private val historyRepository: CrawlExtractionRuleChangeHistoryRepository,
    private val htmlFetcher: HtmlFetcher,
    private val htmlSanitizer: HtmlSanitizer,
    private val ollamaClient: OllamaClient
) {
    private val logger = LoggerFactory.getLogger(SelfHealingService::class.java)

    /**
     * BROKEN 상태인 타겟을 분석하여 규칙을 복구
     */
    @Transactional
    suspend fun healBrokenTask(targetId: Long) = withContext(Dispatchers.IO) {
        val target = targetRepository.findWithMetadataById(targetId)
            ?: throw RuntimeException("Target not found: $targetId")

        logger.info("🚑 Starting Self-Healing for Target: ${target.targetUrl}")

        try {
            val doc = try {
                htmlFetcher.fetch(target)
            } catch (e: Exception) {
                logger.warn("Could not fetch HTML for healing. Skipping.", e)
                return@withContext
            }

            val sanitizedHtml = htmlSanitizer.sanitize(doc.html())

            var healedCount = when (target.pageType) {
                "LIST" -> healListScope(target, sanitizedHtml, doc)
                "CONTENT" -> healExtractionRules(target, sanitizedHtml, doc)
                else -> 0
            }

            if (healedCount > 0) {
                target.status = "PENDING"
                target.retryCount = 0
                logger.info("✅ Healed $healedCount rules. Target rescheduled for retry.")
            } else {
                logger.warn("❌ Failed to heal target. Keeping status as BROKEN.")
            }
            targetRepository.save(target)

        } catch (e: Exception) {
            logger.error("Error during self-healing", e)
        }
    }

    private fun healListScope(target: CrawlTargetEntity, sanitizedHtml: String, doc: Document): Int {
        val pageRule = target.pageRule ?: return 0
        val oldSelector = pageRule.linkSearchScope ?: "body"

        // 현재 선택자(PageRule의 linkSearchScope)로 링크가 조회되는지 확인 (0개면 구조 변경)
        val currentLinks = doc.select(oldSelector).select("a[href]")
        if (currentLinks.isNotEmpty()) {
            logger.info("ℹ️ LIST Scope seems fine (Found ${currentLinks.size} links). Maybe network issue?")
            return 0
        }

        logger.info("🔧 Healing LIST Scope for rule: ${pageRule.ruleName}")

        val description = "The main container element that wraps the list of news articles."

        val recommendation = try {
            ollamaClient.recommendSelector(
                htmlSource = sanitizedHtml,
                targetDescription = description,
                objective = SelectorObjective.DISCOVERY_SCOPE
            )
        } catch (e: Exception) {
            logger.error("LLM call failed", e)
            return 0
        }

        val newSelector = recommendation.selector

        if (!newSelector.isNullOrBlank()) {
            val newScope = doc.select(newSelector)
            val linkCount = newScope.select("a[href]").size

            if (linkCount > 0) {
                val updatedPageRule = pageRule.copy(linkSearchScope = newSelector)
                pageRuleRepository.save(updatedPageRule)
                saveHistory(
                    ruleId = pageRule.id!!,
                    targetId = target.id!!,
                    oldVal = oldSelector,
                    newVal = newSelector,
                    reason = "LLM Fix List Scope: Found $linkCount links"
                )
                return 1
            } else {
                logger.warn("❌ LLM suggested '$newSelector' but still 0 links found.")
            }
        }

        return 0
    }

    private fun healExtractionRules(target: CrawlTargetEntity, sanitizedHtml: String, doc: Document): Int {
        val pageRule = target.pageRule ?: return 0
        val rules = extractionRuleRepository.findAllByPageRule(pageRule)

        var successCount = 0

        for (rule in rules) {
            // 데이터가 추출 되는지?
            val currentElement = doc.selectFirst(rule.cssSelector)
            val isMissing = if (currentElement == null) {
                true
            } else {
                if (!rule.extractAttributes.isNullOrBlank()) {
                    val attrName = rule.extractAttributes!!.split(",")[0].trim()
                    currentElement.attr(attrName).isBlank()
                } else {
                    currentElement.text().isBlank()
                }
            }

            // 필수 항목인데 비어있다면 치유 시도
            if (isMissing && rule.isRequired) {
                logger.warn("🚨 Rule Broken: [${rule.jsonKey}] selector '${rule.cssSelector}' failed.")
                val targetDescription = generateTargetDescription(rule)

                val recommendation = try {
                    ollamaClient.recommendSelector(
                        htmlSource = sanitizedHtml,
                        targetDescription = targetDescription,
                        objective = SelectorObjective.DATA_EXTRACTION
                    )
                } catch (e: Exception) {
                    logger.error("LLM call failed for ${rule.jsonKey}", e)
                    continue
                }

                val newSelector = recommendation.selector

                // 새 선택자로 데이터가 추출되는지?
                if (!newSelector.isNullOrBlank()) {
                    val verificationElement = doc.selectFirst(newSelector)
                    val isValid = if (verificationElement != null) {
                        if (!rule.extractAttributes.isNullOrBlank()) {
                            val attrName = rule.extractAttributes!!.split(",")[0].trim()
                            verificationElement.attr(attrName).isNotBlank()
                        } else {
                            verificationElement.text().isNotBlank()
                        }
                    } else false

                    if (isValid) {
                        val updatedRule = rule.copy(
                            cssSelector = newSelector,
                            status = "ACTIVE"
                        )
                        extractionRuleRepository.save(updatedRule)

                        saveHistory(
                            ruleId = rule.id!!,
                            targetId = target.id!!,
                            oldVal = rule.cssSelector,
                            newVal = newSelector,
                            reason = "LLM Fix: ${recommendation.reason}"
                        )

                        logger.info("✅ Healed [${rule.jsonKey}]! $newSelector")
                        successCount++
                    } else {
                        logger.warn("❌ LLM suggested '$newSelector' for [${rule.jsonKey}] but verification failed.")
                    }
                }
            } else if (isMissing && !rule.isRequired) {
                logger.info("ℹ️ Optional Rule [${rule.jsonKey}] missing. Skipping.")
            }
        }

        return successCount
    }

    private fun generateTargetDescription(rule: CrawlExtractionRuleEntity): String {
        val keyName = rule.jsonKey
        return when {
            rule.extractAttributes.isNullOrBlank() ->
                "The specific element containing the text content for '$keyName'."

            rule.extractAttributes?.contains("src") == true ->
                "The <img> tag representing the '$keyName' (focus on src or data-src)."

            rule.extractAttributes?.contains("href") == true ->
                "The <a> tag containing the link URL for '$keyName'."

            else ->
                "The element containing the '${rule.extractAttributes}' attribute for '$keyName'."
        }
    }

    /**
     * [Helper] 변경 이력 저장
     */
    private fun saveHistory(ruleId: Long, targetId: Long, oldVal: String, newVal: String, reason: String?) {
        historyRepository.save(
            CrawlExtractionRuleChangeHistoryEntity(
                ruleId = ruleId,
                targetId = targetId,
                oldSelector = oldVal,
                newSelector = newVal,
                changeReason = reason ?: "Self Healing",
                isVerified = true
            )
        )
    }
}