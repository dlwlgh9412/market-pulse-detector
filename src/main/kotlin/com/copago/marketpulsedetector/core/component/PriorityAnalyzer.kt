package com.copago.marketpulsedetector.core.component

import com.copago.marketpulsedetector.domain.entity.CrawlPriorityRuleEntity
import com.copago.marketpulsedetector.domain.enums.PriorityConditionType
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class PriorityAnalyzer {
    fun calculatePriority(linkElement: Element, rules: List<CrawlPriorityRuleEntity>, basePriority: Int = 50): Int {
        var totalPriority = basePriority

        for (rule in rules) {
            if (!rule.isActive) continue

            val isMatched = when (rule.conditionType) {
                PriorityConditionType.SELECTOR_MATCH -> try {
                    linkElement.`is`(rule.conditionExpression)
                } catch (e: Exception) {
                    false
                }

                PriorityConditionType.TEXT_CONTAINS -> linkElement.text()
                    .contains(rule.conditionExpression, ignoreCase = true)
            }

            if (isMatched) {
                totalPriority += rule.priorityBonus
            }
        }
        return totalPriority
    }
}