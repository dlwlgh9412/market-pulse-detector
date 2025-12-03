package com.copago.marketpulsedetector.core.healing

import com.copago.marketpulsedetector.core.component.HtmlFetcher
import com.copago.marketpulsedetector.core.healing.enums.SelectorObjective
import com.copago.marketpulsedetector.scheudler.CrawlHealingScheduler
import com.copago.marketpulsedetector.scheudler.CrawlScheduler
import org.jsoup.Jsoup
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class OllamaClientTest(
    @Autowired private val ollamaClient: OllamaClient,
    @Autowired private val htmlFetcher: HtmlFetcher,
    @Autowired private val htmlSanitizer: HtmlSanitizer
) {
    @MockitoBean
    private lateinit var crawlScheduler: CrawlScheduler

    @MockitoBean
    private lateinit var crawlHealingScheduler: CrawlHealingScheduler

    @Test
    fun `should return css selector`() {
        // Given: 간단한 HTML 조각
        val html = """
            <div class="news_view">
                <h2 id="title_area" class="media_end_head_headline">
                    <span>삼성전자, 100조 투자 확정</span>
                </h2>
                <div id="newsct_body">본문 내용입니다...</div>
            </div>
        """.trimIndent()

        // When: "기사 제목"에 대한 선택자 요청
        val result = ollamaClient.recommendSelector(
            htmlSource = html,
            targetDescription = "The specific element containing the article title text.", // 구체적 설명
            objective = SelectorObjective.DATA_EXTRACTION // [수정] 데이터 추출 목적
        )

        // Then: 결과 검증
        println("Recommended Selector: ${result.selector}")

        // 예상: #title_area span 또는 h2.media_end_head_headline 등 유효한 선택자가 나와야 함
        assert(result.selector!!.isNotBlank())
    }

    @Test
    @DisplayName("실제 네이버 뉴스 목록 html을 가져와서 리스트 css 선택자를 반환")
    fun `should return css selector from naver list`() {
        val doc = Jsoup.connect("https://news.naver.com/section/100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .timeout(100000)
            .execute()
            .parse()

        val sanitizer = htmlSanitizer.sanitize(doc.html())

        val result = ollamaClient.recommendSelector(
            htmlSource = sanitizer,
            targetDescription = "The main container element that wraps the list of news articles.", // "list_scope" 대신 구체적 설명
            objective = SelectorObjective.DISCOVERY_SCOPE // [수정] 범위 탐색 목적
        )

        println("Recommended Selector: ${result.selector}")

        assert(result.selector!!.isNotBlank())
    }


}