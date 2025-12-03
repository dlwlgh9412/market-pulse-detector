package com.copago.marketpulsedetector.core.healing

import com.copago.marketpulsedetector.core.component.HtmlFetcher
import com.copago.marketpulsedetector.core.healing.enums.SelectorObjective
import com.copago.marketpulsedetector.domain.entity.CrawlExtractionRuleChangeHistoryEntity
import com.copago.marketpulsedetector.domain.entity.CrawlExtractionRuleEntity
import com.copago.marketpulsedetector.domain.entity.CrawlPageRuleEntity
import com.copago.marketpulsedetector.domain.entity.CrawlSiteEntity
import com.copago.marketpulsedetector.domain.entity.CrawlTargetEntity
import com.copago.marketpulsedetector.domain.repository.CrawlExtractionRuleChangeHistoryRepository
import com.copago.marketpulsedetector.domain.repository.CrawlExtractionRuleRepository
import com.copago.marketpulsedetector.domain.repository.CrawlTargetRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import java.util.Optional
import kotlin.test.Test

class SelfHealingServiceTest {

    @MockK
    private lateinit var targetRepository: CrawlTargetRepository

    @MockK
    private lateinit var ruleRepository: CrawlExtractionRuleRepository

    @MockK
    private lateinit var historyRepository: CrawlExtractionRuleChangeHistoryRepository

    @MockK
    private lateinit var htmlFetcher: HtmlFetcher

    @MockK
    private lateinit var htmlSanitizer: HtmlSanitizer

    @MockK
    private lateinit var ollamaClient: OllamaClient

    @InjectMockKs
    private lateinit var selfHealingService: SelfHealingService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    private fun createMockTarget(): CrawlTargetEntity {
        val site = CrawlSiteEntity(siteName = "Test Site", domain = "test.com")
        val pageRule = CrawlPageRuleEntity(
            site = site,
            ruleName = "TestRule",
            pageType = "CONTENT",
            matchPriority = 10,
            urlPatternRegex = null,
            linkSearchScope = null
        )
        return CrawlTargetEntity(
            id = 1L, site = site, pageRule = pageRule,
            targetUrl = "http://test.com/article/1", targetUrlHash = "hash",
            pageType = "CONTENT", status = "BROKEN"
        )
    }

    @Test
    @DisplayName("시나리오 1: 필수 규칙이 깨졌을 때 LLM이 올바른 선택자를 주면 치유에 성공하고 타겟을 PENDING으로 되돌린다")
    fun healSuccess() = runTest {
        val target = createMockTarget()
        // 실제 HTML에는 #old-title 이 없음
        val brokenRule = CrawlExtractionRuleEntity(
            id = 100L, pageRule = target.pageRule!!,
            jsonKey = "title", cssSelector = "#old-title",
            isRequired = true
        )

        // 실제 HTML id는 new-title
        val htmlContent = """<html><body><h1 id="new-title">Real Title</h1></body></html>"""
        val doc = Jsoup.parse(htmlContent)

        every { targetRepository.findById(1L) } returns Optional.of(target)
        coEvery { htmlFetcher.fetch(target) } returns doc
        every { ruleRepository.findAllByPageRule(target.pageRule!!) } returns listOf(brokenRule)
        every { htmlSanitizer.sanitize(any()) } returns "sanitized html"
        every {
            ollamaClient.recommendSelector(any(), any(), SelectorObjective.DATA_EXTRACTION)
        } returns SelectorRecommendation("#new-title", "Found ID")

        val ruleSlot = slot<CrawlExtractionRuleEntity>()
        val targetSlot = slot<CrawlTargetEntity>()
        val historySlot = slot<CrawlExtractionRuleChangeHistoryEntity>()

        every { ruleRepository.save(capture(ruleSlot)) } returns brokenRule
        every { targetRepository.save(capture(targetSlot)) } returns target
        every { historyRepository.save(capture(historySlot)) } returns mockk()

        // When
        selfHealingService.healBrokenTask(1L)

        // Then
        // 1. Rule이 업데이트 되었는가?
        assertEquals("#new-title", ruleSlot.captured.cssSelector)
        assertEquals("LLM", ruleSlot.captured.updatedBy)

        // 2. Target 상태가 복구되었는가?
        assertEquals("PENDING", targetSlot.captured.status)
        assertEquals(0, targetSlot.captured.retryCount)

        // 3. 이력이 남았는가?
        assertEquals("#old-title", historySlot.captured.oldSelector)
        assertEquals("#new-title", historySlot.captured.newSelector)

        verify(exactly = 1) {
            ollamaClient.recommendSelector(any(), any(), SelectorObjective.DATA_EXTRACTION)
        }
    }

    @Test
    @DisplayName("시나리오 2: LLM이 제안한 선택자도 틀리면(검증 실패) DB를 업데이트하지 않는다")
    fun healFailVerification() = runTest {
        // Given
        val target = createMockTarget()
        val brokenRule = CrawlExtractionRuleEntity(
            id = 100L, pageRule = target.pageRule!!,
            jsonKey = "title", cssSelector = "#old-title",
            isRequired = true
        )

        // 실제 HTML (제목이 아예 텍스트가 없거나 엉뚱함)
        val htmlContent = """<html><body><div id="nothing"></div></body></html>"""
        val doc = Jsoup.parse(htmlContent)

        // Mocks
        every { targetRepository.findById(1L) } returns Optional.of(target)
        coEvery { htmlFetcher.fetch(target) } returns doc
        every { ruleRepository.findAllByPageRule(target.pageRule!!) } returns listOf(brokenRule)
        every { htmlSanitizer.sanitize(any()) } returns "sanitized html"

        // [LLM] 잘못된 선택자 제안 (환각)
        every {
            ollamaClient.recommendSelector(any(), any(), SelectorObjective.DATA_EXTRACTION)
        } returns SelectorRecommendation("#wrong-selector", "Guessing")
        // When
        selfHealingService.healBrokenTask(1L)

        // Then
        // 1. Rule 저장(업데이트)이 호출되지 않아야 함 (검증 실패했으므로)
        verify(exactly = 0) { ruleRepository.save(any()) }

        // 2. History 저장도 안 함 (혹은 실패 이력을 저장하게 로직을 짰다면 검증 필요)
        verify(exactly = 0) { historyRepository.save(any()) }

        // 3. 타겟 상태는 여전히 BROKEN 이어야 함 (복구된 게 없으므로)
        // (로직상 아무것도 안 고쳐지면 save(target) 호출 시 상태 변경이 없어야 함)
        verify(exactly = 1) { targetRepository.save(match { it.status == "BROKEN" }) }
    }

    @Test
    @DisplayName("시나리오 3: 선택적(Optional) 규칙이 실패한 경우 LLM을 호출하지 않고 무시한다")
    fun skipOptionalRule() = runTest {
        // Given
        val target = createMockTarget()
        val optionalRule = CrawlExtractionRuleEntity(
            id = 200L, pageRule = target.pageRule!!,
            jsonKey = "image", cssSelector = "#img-area",
            isRequired = false // [선택] 없어도 그만
        )

        // 실제 HTML (이미지 없음)
        val htmlContent = """<html><body><h1>Title is here</h1></body></html>"""
        val doc = Jsoup.parse(htmlContent)

        // Mocks
        every { targetRepository.findById(1L) } returns Optional.of(target)
        coEvery { htmlFetcher.fetch(target) } returns doc
        every { ruleRepository.findAllByPageRule(target.pageRule!!) } returns listOf(optionalRule)
        every { htmlSanitizer.sanitize(any()) } returns "sanitized" // 호출될 수도 있음 (로직 위치에 따라)

        // Save 호출 스터빙 (아무 변화 없어도 호출될 수 있음)
        every { targetRepository.save(any()) } returns target

        // When
        selfHealingService.healBrokenTask(1L)

        // Then
        // [핵심] LLM Client가 호출되지 않아야 한다!
        verify(exactly = 0) {
            ollamaClient.recommendSelector(any(), any(), any())
        }
        // 규칙 업데이트도 없어야 한다.
        verify(exactly = 0) { ruleRepository.save(any()) }
    }
}