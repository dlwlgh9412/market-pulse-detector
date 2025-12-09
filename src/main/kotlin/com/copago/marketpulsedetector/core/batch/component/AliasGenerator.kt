package com.copago.marketpulsedetector.core.batch.component

import com.copago.marketpulsedetector.core.batch.provider.dto.StockDto
import org.springframework.stereotype.Component

@Component
class AliasGenerator {
    fun generate(dto: StockDto): Set<String> {
        val aliases = mutableSetOf<String>()

        aliases.add(dto.name)
        aliases.add(dto.code)

        dto.englishName?.let {
            aliases.add(it)
        }

        if (dto.country == "KR") {
            val cleanName = dto.name.replace(Regex("\\(.*\\)"), "").trim()
            if (cleanName != dto.name) aliases.add(cleanName)
        }

        return aliases
    }
}