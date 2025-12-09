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
import org.jsoup.nodes.Element
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

    private fun healExtractionRules(target: CrawlTargetEntity, rawHtml: String, doc: Document): Int {
        val pageRule = target.pageRule ?: return 0
        val rules = extractionRuleRepository.findAllByPageRule(pageRule)

        var successCount = 0

        for (rule in rules) {
            // 1. 현재 규칙으로 추출 시도
            val currentElement = doc.selectFirst(rule.cssSelector)

            // 데이터가 비어있거나(null or blank) 검증 실패 시 Broken으로 판단
            val isBroken = !isValidExtraction(currentElement, rule)

            if (isBroken && rule.isRequired) {
                logger.warn("🚨 Rule Broken: [${rule.jsonKey}]")
                val targetDescription = generateTargetDescription(rule)

                // 2. LLM 호출 (rawHtml을 넘기면 내부에서 sanitizeForStructure 수행)
                val recommendation = try {
                    ollamaClient.recommendSelector(
                        htmlSource = rawHtml,
                        targetDescription = targetDescription,
                        objective = SelectorObjective.DATA_EXTRACTION
                    )
                } catch (e: Exception) {
                    logger.error("LLM call failed", e)
                    continue
                }

                val newSelector = recommendation.selector

                // 3. [검증 로직 강화] 새 선택자로 추출한 데이터가 유효한지 검사
                if (!newSelector.isNullOrBlank()) {
                    val verificationElement = doc.selectFirst(newSelector)

                    if (isValidExtraction(verificationElement, rule)) {
                        // 성공 시 업데이트 및 이력 저장 (기존 코드와 동일)
                        val updatedRule = rule.copy(cssSelector = newSelector, status = "ACTIVE")
                        extractionRuleRepository.save(updatedRule)
                        saveHistory(rule.id!!, target.id!!, rule.cssSelector, newSelector, recommendation.reason)

                        logger.info("✅ Healed [${rule.jsonKey}]! $newSelector")
                        successCount++
                    } else {
                        logger.warn("❌ LLM suggested '$newSelector' but validation failed.")
                    }
                }
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

    private fun isValidExtraction(element: Element?, rule: CrawlExtractionRuleEntity): Boolean {
        if (element == null) return false

        // 속성 추출인 경우 (예: src, href)
        if (!rule.extractAttributes.isNullOrBlank()) {
            val attrValue = element.attr(rule.extractAttributes!!.trim())
            return attrValue.isNotBlank()
            // 추가 검증: 이미지라면 http로 시작하는지 등
            // && (!rule.jsonKey.contains("image") || attrValue.startsWith("http"))
        }

        // 텍스트 추출인 경우
        val text = element.text().trim()
        if (text.isBlank()) return false

        // 1. 최소 길이 검사 (본문(content)인데 너무 짧으면 의심)
        if (rule.jsonKey == "content" && text.length < 30) {
            return false
        }

        // 2. 금지어(Blacklist) 검사
        // 광고, 저작권 문구, 메뉴 이름 등이 잡히면 실패로 간주
        val blackList = listOf("Copyright", "All rights reserved", "광고", "배너", "구독", "메인으로")
        if (blackList.any { text.contains(it, ignoreCase = true) }) {
            return false
        }

        return true
    }
}