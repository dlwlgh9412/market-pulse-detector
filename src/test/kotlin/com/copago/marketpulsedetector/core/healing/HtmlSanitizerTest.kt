package com.copago.marketpulsedetector.core.healing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HtmlSanitizerTest {
    private val sanitizer = HtmlSanitizer()

    @Test
    @DisplayName("불필요한 태그(script, style 등)가 제거되어야 한다")
    fun removeUnwantedTags() {
        // Given
        val rawHtml = """
            <div>
                <script>console.log('remove me');</script>
                <h1>Title</h1>
                <style>.css { color: red; }</style>
                <iframe src="ads.html"></iframe>
            </div>
        """.trimIndent()

        // When
        val result = sanitizer.sanitize(rawHtml)

        // Then
        // script, style, iframe이 사라지고 div와 h1만 남아야 함
        // sanitizer가 공백을 압축하므로 결과 문자열 포맷에 주의
        assertEquals("<div><h1>Title</h1></div>", result)
    }

    @Test
    @DisplayName("허용된 속성(id, class, name) 외에는 모두 제거되어야 한다")
    fun cleanAttributes() {
        // Given
        // style, onclick, data-id는 제거 대상
        // id, class는 유지 대상
        val rawHtml = """
            <div id="content" class="main-article" style="color: blue" onclick="alert('hi')" data-id="123">
                <p class="text">Hello</p>
            </div>
        """.trimIndent()

        // When
        val result = sanitizer.sanitize(rawHtml)

        // Then
        val expected = """<div id="content" class="main-article"><p class="text">Hello</p></div>"""
        assertEquals(expected, result)
    }

    @Test
    @DisplayName("HTML 주석이 제거되어야 한다")
    fun removeComments() {
        // Given
        val rawHtml = """
            <div>
                <span>Content</span>
                </div>
        """.trimIndent()

        // When
        val result = sanitizer.sanitize(rawHtml)

        // Then
        assertEquals("<div><span>Content</span></div>", result)
    }

    @Test
    @DisplayName("네비게이션(header, footer, nav) 영역이 제거되어야 한다")
    fun removeLayoutAreas() {
        // Given
        val rawHtml = """
            <body>
                <header>Menu</header>
                <main>
                    <div id="news">News Body</div>
                </main>
                <footer>Copyright</footer>
            </body>
        """.trimIndent()

        // When
        val result = sanitizer.sanitize(rawHtml)

        // Then
        // header, footer는 사라지고 main과 그 내용만 남아야 함
        val expected = "<main><div id=\"news\">News Body</div></main>"
        assertEquals(expected, result)
    }

    @Test
    @DisplayName("복잡한 실제 뉴스 페이지 HTML 시뮬레이션 검증")
    fun complexRealWorldSimulation() {
        // Given: 실제 뉴스 페이지와 유사한 복잡한 구조
        val rawHtml = """
            <html>
            <head>
                <title>뉴스 제목</title>
                <script>var x = 1;</script>
            </head>
            <body>
                <div id="wrap">
                    <header class="header">로고 및 메뉴</header>
                    
                    <div id="container" style="padding: 10px;">
                        <div class="ad-banner" onclick="track()">광고입니다</div>
                        
                        <div id="newsct_article" class="article_view">
                            <h2 class="title" data-log="title">속보입니다</h2>
                            <span class="date">2023-10-01</span>
                            <div id="dic_area">
                                본문 내용입니다. <br>
                                <img src="img.jpg" style="width:100%">
                            </div>
                        </div>
                    </div>
                    
                    <footer>푸터입니다</footer>
                </div>
            </body>
            </html>
        """.trimIndent()

        // When
        val result = sanitizer.sanitize(rawHtml)

        // Then
        // 1. 구조적 핵심(id="newsct_article")이 살아있는지 확인
        assertTrue(result.contains("<div id=\"newsct_article\" class=\"article_view\">"))

        // 2. 제목과 본문 내용이 유지되는지 확인
        assertTrue(result.contains("<h2 class=\"title\">속보입니다</h2>"))
        assertTrue(result.contains("<div id=\"dic_area\">"))

        // 3. 불필요한 태그/속성 제거 확인
        assertTrue(!result.contains("<script>"))
        assertTrue(!result.contains("style="))
        assertTrue(!result.contains("onclick="))
        assertTrue(!result.contains("header"))
        assertTrue(!result.contains("footer"))

        // 4. 전체 압축 결과 확인 (공백 제거됨)
        // img 태그의 src도 제거됨 (Sanitizer 로직상 id, class, name만 남기므로)
        // 이는 LLM이 "구조"만 보고 선택자를 찾게 하기 위함임.
        println("Sanitized HTML Output: $result")
    }
}