package com.copago.marketpulsedetector.core.analysis

import com.copago.marketpulsedetector.domain.entity.StockInfoEntity
import com.copago.marketpulsedetector.domain.repository.StockAliasRepository
import com.copago.marketpulsedetector.domain.repository.StockInfoRepository
import jakarta.annotation.PostConstruct
import org.ahocorasick.trie.Trie
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class StockExtractor(
    private val stockInfoRepository: StockInfoRepository,
    private val stockAliasRepository: StockAliasRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(StockExtractor::class.java)
    private var trie: Trie? = null
    private val aliasMap = ConcurrentHashMap<String, String>()
    private val stockInfoMap: ConcurrentHashMap<String, StockInfoEntity> = ConcurrentHashMap()


    @PostConstruct
    fun init() {
        refreshCache()
    }

    @Scheduled(fixedDelay = 3600000)
    fun refreshCache() {
        logger.info("🔄 Building Aho-Corasick Trie...")

        // 1. 기준 정보 로딩
        val stocks = stockInfoRepository.findAllByIsActiveTrue()
        stockInfoMap.clear()
        stocks.forEach { stockInfoMap[it.code] = it }

        // 2. 별칭 정보 로딩
        val aliases = stockAliasRepository.findAll()
        aliasMap.clear()

        // Trie Builder 시작
        val builder = Trie.builder()
            .ignoreCase() // 대소문자 무시 (본문에 'tesla'가 있어도 'Tesla'로 매칭됨)
            .ignoreOverlaps()

        var count = 0
        aliases.forEach { entity ->
            if (stockInfoMap.containsKey(entity.stock.code)) {
                // [수정 3] Payload 없이 키워드만 등록
                builder.addKeyword(entity.alias)

                // 나중에 찾을 수 있게 Map에 저장
                aliasMap[entity.alias] = entity.stock.code
                count++
            }
        }

        this.trie = builder.build()
        logger.info("✅ Trie Built. Loaded ${stocks.size} stocks and $count aliases.")
    }

    fun extractStocks(text: String): List<StockInfoEntity> {
        if (text.isBlank() || trie == null) return emptyList()

        val foundStockCodes = mutableSetOf<String>()

        // 1. Aho-Corasick 검색
        val emits = trie!!.parseText(text)

        for (emit in emits) {
            // [수정 4] emit.value 대신 emit.keyword 사용
            // emit.keyword는 Trie에 등록했던 원본 키워드(Alias)를 반환함
            val alias = emit.keyword
            val start = emit.start
            val end = emit.end

            // 2. 단어 경계 체크
            if (isWholeWord(text, start, end + 1)) {
                // 별칭(alias)을 이용해 Map에서 진짜 코드(code)를 찾음
                val code = aliasMap[alias]
                if (code != null) {
                    foundStockCodes.add(code)
                }
            }
        }

        return foundStockCodes.mapNotNull { stockInfoMap[it] }
    }

    // (기존 isWholeWord, isAlphaNumeric 로직은 동일하게 유지)
    private fun isWholeWord(text: String, start: Int, end: Int): Boolean {
        if (start > 0) {
            val prevChar = text[start - 1]
            if (Character.isLetterOrDigit(prevChar)) {
                return false
            }
        }

        if (end < text.length) {
            val nextChar = text[end]
            if (Character.isLetterOrDigit(nextChar)) {
                val keyword = text.substring(start, end)
                if (isAlphaNumeric(keyword)) {
                    return false
                }
            }
        }
        return true
    }

    private fun isAlphaNumeric(s: String): Boolean {
        return s.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
    }
}