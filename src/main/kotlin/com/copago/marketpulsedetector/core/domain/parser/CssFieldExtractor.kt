package com.copago.marketpulsedetector.core.domain.parser

import com.copago.marketpulsedetector.core.domain.model.CrawlSelector
import com.copago.marketpulsedetector.core.domain.model.SelectorType
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class CssFieldExtractor : FieldExtractor {

    override fun supports(type: SelectorType): Boolean {
        return type == SelectorType.CSS
    }

    override fun extract(
        context: ParserContext,
        selector: CrawlSelector
    ): Any? {
        val doc = Jsoup.parse(context.html, context.baseUrl)
        val elements = doc.select(selector.selectorValue)

        if (elements.isEmpty()) return null

        if (selector.isMultiple) {
            return elements.map { el ->
                parseElement(el, selector.extractAttribute)
            }.filter { it.isNotBlank() }
        }

        return parseElement(elements.first(), selector.extractAttribute)
    }

    private fun parseElement(element: Element, attribute: String?): String {
        return when (attribute) {
            "text", null -> element.text().trim()
            "html" -> element.html()
            "href", "src" -> element.absUrl(attribute)
            else -> element.attr(attribute).trim()
        }
    }
}