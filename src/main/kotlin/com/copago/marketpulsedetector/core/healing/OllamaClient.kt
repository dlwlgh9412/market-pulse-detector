package com.copago.marketpulsedetector.core.healing

import com.copago.marketpulsedetector.core.healing.enums.SelectorObjective
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.stereotype.Component

@Component
class OllamaClient(
    private val ollamaChatModel: OllamaChatModel,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(OllamaClient::class.java)
    private val chatClient =
        ChatClient.builder(ollamaChatModel)
            .defaultOptions(
                OllamaOptions.builder()
                    .format("json")
                    .temperature(0.0)
                    .build()
            )
            .build()

    /**
     * LLM에게 HTML을 분석시켜 새로운 CSS 선택자를 추천받음
     * @param htmlSource 압축된 HTML (전체 HTML X)
     * @param targetKey 추출하려는 데이터의 의미 (예: "기사 제목", "title")
     */
    fun recommendSelector(
        htmlSource: String,
        targetDescription: String,
        objective: SelectorObjective
    ): SelectorRecommendation {

        // 1. [중요] 입력 HTML 길이 제한 (Context Overflow 방지)
        // Llama3 8b 기준 약 4~8k 토큰 한계가 있으므로, 안전하게 문자열 길이로 자름.
        // 목록 탐색이나 제목 찾기는 상단 15,000자면 충분히 파악 가능.
        val truncatedHtml = if (htmlSource.length > 15000) {
            htmlSource.substring(0, 15000) + "...(truncated)"
        } else {
            htmlSource
        }

        logger.info("🤖 Asking Ollama ($objective): [$targetDescription] (Len: ${truncatedHtml.length})")

        // 2. 목적별 시스템 프롬프트 분리
        val systemInstruction = when (objective) {
            SelectorObjective.DISCOVERY_SCOPE -> """
                ROLE: CSS Selector Generator Tool.
                TASK: Find the CSS Selector for the PARENT CONTAINER that wraps the list items described by the user.
                
                [ONE-SHOT EXAMPLE]
                User Input Description: "The main list of product cards."
                User Input HTML: 
                '<div id="main"><section class="products"><div class="card">...</div><div class="card">...</div></section></div>'
                
                Correct Output JSON:
                {
                    "selector": "section.products",
                    "reason": "The <section> with class 'products' directly wraps all .card items."
                }
                
                [YOUR INSTRUCTIONS]
                1. Analyze the HTML provided by the user.
                2. Identify the WRAPPER element for: '${'$'}targetDescription'.
                3. Return ONLY the JSON object with keys "selector" and "reason".
                4. DO NOT extract the content data. ONLY return the CSS selector string.
            """.trimIndent()

            SelectorObjective.DATA_EXTRACTION -> """
                ROLE: CSS Selector Generator Tool.
                TASK: Find the CSS Selector for the specific LEAF ELEMENT containing the data described by the user.
                
                [ONE-SHOT EXAMPLE]
                User Input Description: "The product price text."
                User Input HTML: 
                '<div class="card"><h2 class="title">Item</h2><span id="price-tag">$100</span></div>'
                
                Correct Output JSON:
                {
                    "selector": "span#price-tag",
                    "reason": "The <span> with id 'price-tag' contains the price text."
                }
                
                [YOUR INSTRUCTIONS]
                1. Analyze the HTML provided by the user.
                2. Identify the unique element for: '${'$'}targetDescription'.
                3. Return ONLY the JSON object with keys "selector" and "reason".
                4. If it is an image, select the <img> tag.
                5. DO NOT extract the content data. ONLY return the CSS selector string.
            """.trimIndent()
        }

        val userTextTemplate = """
            TARGET: {targetDescription}
            
            HTML SNIPPET:
            {html}
        """.trimIndent()

        val promptTemplate = PromptTemplate(userTextTemplate)
        val prompt = promptTemplate.create(
            mapOf(
                "targetDescription" to targetDescription,
                "html" to truncatedHtml
            )
        )

        try {
            val response = chatClient.prompt(prompt)
                .system(systemInstruction)
                .call()
                .content()

            logger.info("🤖 Ollama Response: $response")

            // (이전 단계에서 만든 JsonExtractor를 쓰시거나, 직접 파싱)
            val result = objectMapper.readValue(response, SelectorRecommendation::class.java)

            if (result.selector.isNullOrBlank()) {
                logger.warn("🚨 LLM returned JSON but 'selector' is null. Raw: $response")
                return SelectorRecommendation(selector = "", reason = "LLM returned empty selector")
            }

            return result
        } catch (e: Exception) {
            logger.error("💥 LLM Analysis failed", e)
            throw RuntimeException("Failed to get recommendation", e)
        }
    }
}