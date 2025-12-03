package com.copago.marketpulsedetector.core.healing

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.springframework.stereotype.Component

@Component
class HtmlSanitizer {
    fun sanitize(rawHtml: String): String {
        if (rawHtml.isBlank()) return ""

        val doc = Jsoup.parse(rawHtml)

        val blacklist = "script, style, svg, iframe, noscript, link, meta, " +
                "header, footer, nav, aside, " +
                ".footer, .bottom, #footer, .copyright, .ad, .banner"

        doc.select(blacklist).remove()

        // 주석 제거
        removeComments(doc)

        // 불필요한 속성 제거
        // id, class만 남기고 style, onclick, data-* 등은 제거
        cleanAttributes(doc)

        truncateTextContent(doc.body())

        return doc.body().html()
            .replace(Regex("\\s+"), " ") // 연속된 공백을 하나로
            .replace(Regex(">\\s+<"), "><") // 태그 사이 공백 제거
            .trim()
    }

    private fun removeComments(node: Node) {
        var i = 0
        while (i < node.childNodeSize()) {
            val child = node.childNode(i)
            if (child.nodeName() == "#comment") {
                child.remove()
                i--
            } else {
                removeComments(child)
            }
            i++
        }
    }

    private fun cleanAttributes(element: Element) {
        // 남길 속성들 (구조 파악에 필수적인 것만)
        val allowedAttributes = setOf("id", "class", "name")

        // 모든 하위 요소를 순회하며 속성 청소
        element.allElements.forEach { el ->
            val attributes = el.attributes().toList()
            for (attr in attributes) {
                if (attr.key !in allowedAttributes) {
                    el.removeAttr(attr.key)
                }
            }
        }
    }

    /**
     * 텍스트 노드를 순회하며 앞의 15자만 남기고 자릅니다.
     */
    private fun truncateTextContent(element: Element) {
        for (node in element.textNodes()) {
            val text = node.text().trim()
            if (text.isNotEmpty()) {
                // 15자 이상이면 자르고 "..." 붙임
                // 이렇게 하면 긴 저작권 문구는 파괴되고, 짧은 제목(키워드)은 살아남음
                val newText = if (text.length > 15) text.substring(0, 15) + "..." else text
                node.text(newText)
            }
        }

        // 재귀 호출
        for (child in element.children()) {
            truncateTextContent(child)
        }
    }
}