package com.copago.marketpulsedetector.config

import com.copago.marketpulsedetector.core.service.UserAgentService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.data.redis.serializer.GenericToStringSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {
    @Bean
    fun longValueRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Long> {
        val template = RedisTemplate<String, Long>()
        template.connectionFactory = connectionFactory

        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()

        template.valueSerializer = GenericToStringSerializer(Long::class.java)
        template.hashValueSerializer = GenericToStringSerializer(Long::class.java)

        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun redisListenerContainer(connectionFactory: RedisConnectionFactory, userAgentService: com.copago.marketpulsedetector.core.service.UserAgentService): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)

        container.addMessageListener(listenerAdapter(userAgentService), ChannelTopic("ua-block-channel"))
        return container
    }

    @Bean
    fun listenerAdapter(userAgentService: com.copago.marketpulsedetector.core.service.UserAgentService): MessageListenerAdapter {
        return MessageListenerAdapter(userAgentService, "handleBlockMessage")
    }
}