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
                "header, footer, nav, aside, .footer, .bottom, #footer, .copyright, .ad, .banner"
        doc.select(blacklist).remove()
        removeComments(doc)
        cleanAttributes(doc)
        truncateTextContent(doc.body())
        return cleanOutput(doc.body().html())
    }

    fun sanitizeForStructure(rawHtml: String): String {
        if (rawHtml.isBlank()) return ""

        val doc = Jsoup.parse(rawHtml)

        val blacklist = "script, style, svg, iframe, noscript, meta, link, header, footer, nav, aside"
        doc.select(blacklist).remove()

        removeComments(doc)
        cleanAttributes(doc.body())
        minimizeTextNodes(doc.body())

        return cleanOutput(doc.body().html())
    }

    private fun minimizeTextNodes(element: Element) {
        for (node in element.textNodes()) {
            if (node.text().isNotBlank()) {
                node.text("T")
            }
        }

        for (child in element.children()) {
            minimizeTextNodes(child)
        }
    }

    private fun cleanOutput(html: String): String {
        return html.replace(Regex("\\s+"), " ")
            .replace(Regex(">\\s+<"), "><")
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
                val newText = if (text.length > 15) text.substring(0, 15) + "..." else text
                node.text(newText)
            }
        }
        for (child in element.children()) {
            truncateTextContent(child)
        }
    }
}