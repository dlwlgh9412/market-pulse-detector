package com.copago.marketpulsedetector.core.analysis

import com.copago.marketpulsedetector.domain.entity.MarketThemeEntity
import com.copago.marketpulsedetector.domain.repository.MarketThemeKeywordRepository
import com.copago.marketpulsedetector.domain.repository.MarketThemeRepository
import jakarta.annotation.PostConstruct
import org.ahocorasick.trie.Trie
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ThemeExtractor(
    private val themeRepository: MarketThemeRepository,
    private val keywordRepository: MarketThemeKeywordRepository
) {
    private val logger = LoggerFactory.getLogger(ThemeExtractor::class.java)
    private var trie: Trie? = null
    private val keywordMap = ConcurrentHashMap<String, Long>()
    private val themeMap = ConcurrentHashMap<Long, MarketThemeEntity>()

    @PostConstruct
    fun init() = refreshCache()

    @Scheduled(fixedDelay = 3600000)
    fun refreshCache() {
        logger.info("🔄 Building Theme Trie...")
        val themes = themeRepository.findAllByIsActiveTrue()
        themeMap.clear()
        themes.forEach { themeMap[it.id!!] = it }

        val keywords = keywordRepository.findAll()
        keywordMap.clear()

        val builder = Trie.builder().ignoreCase().ignoreOverlaps()
        var count = 0

        keywords.forEach { k ->
            if (themeMap.containsKey(k.theme.id)) {
                builder.addKeyword(k.keyword)
                keywordMap[k.keyword] = k.theme.id!!
                count++
            }
        }

        this.trie = builder.build()
        logger.info("✅ Theme Trie Built. Loaded ${themes.size} themes and $count keywords.")
    }

    fun extractThemes(text: String): List<MarketThemeEntity> {
        if (text.isBlank() || trie == null) return emptyList()

        val foundThemeIds = mutableSetOf<Long>()
        val emits = trie!!.parseText(text)

        for (emit in emits) {
            // 여기서는 Word Boundary 체크를 조금 더 유연하게 할 수도 있음 (정책명은 보통 고유명사가 많음)
            // 일단 단순 매칭으로 진행하거나, StockExtractor의 isWholeWord 재사용 가능
            val themeId = keywordMap[emit.keyword]
            if (themeId != null) {
                foundThemeIds.add(themeId)
            }
        }

        return foundThemeIds.mapNotNull { themeMap[it] }
    }
}