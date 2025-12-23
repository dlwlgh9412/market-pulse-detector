package com.copago.marketpulsedetector.core.domain.parser

import com.copago.marketpulsedetector.core.domain.model.CrawlSelector
import com.copago.marketpulsedetector.core.domain.model.SelectorType


interface FieldExtractor {
    fun supports(type: SelectorType): Boolean
    fun extract(context: ParserContext, selector: CrawlSelector): Any?
}