package com.crispinlab.common.infra.transaction

import com.crispinlab.common.transaction.TransactionProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager

@AutoConfiguration
class TransactionProviderConfiguration {
    @Bean
    @ConditionalOnBean(PlatformTransactionManager::class)
    @ConditionalOnMissingBean(TransactionProvider::class)
    fun transactionProvider(transactionManager: PlatformTransactionManager): TransactionProvider =
        DefaultTransactionProvider(transactionManager)
}
