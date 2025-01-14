package com.github.saintedlittle

import com.github.saintedlittle.application.ConfigManager
import com.github.saintedlittle.application.JsonManager
import com.github.saintedlittle.commands.SynchronizeCommand
import com.github.saintedlittle.domain.*
import com.github.saintedlittle.listeners.BlockListener
import com.github.saintedlittle.listeners.KafkaListener
import com.github.saintedlittle.listeners.MovementListener
import com.github.saintedlittle.listeners.PlayerEventListener
import com.github.saintedlittle.messaging.KafkaConsumerService
import com.github.saintedlittle.messaging.KafkaProducerService
import com.github.saintedlittle.providers.*
import com.google.inject.AbstractModule
import kotlinx.coroutines.CoroutineScope
import org.bukkit.plugin.Plugin
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.MemoryUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

class MainModule(
    private val plugin: Plugin,
    private val scope: CoroutineScope,
    private val configManager: ConfigManager
) : AbstractModule() {

    override fun configure() {
        val cacheManager: CacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .with(CacheManagerBuilder.persistence(File(plugin.dataFolder, "ehcache")))
            .build(true)

        val playerTimesCache = createCache<UUID, java.lang.Long>(cacheManager, "playerTimes", 1000)
        val playerSessionStartCache = createCache<UUID, java.lang.Long>(cacheManager, "playerSessionStart", 1000)
        val playerMovementsCache = createCache<UUID, String>(cacheManager, "playerMovements", 70000)
        val playerBedsCache = createCache<UUID, String>(cacheManager, "playerBeds", 7000)
        val playerExpCache = createCache<UUID, Triple<Int, Int, Int>>(cacheManager, "playerExp", 7000)
        val playerBlockInteractionsCache = createCache<UUID, String>(cacheManager, "playerBlockInteractions", 40000)

        bind(Plugin::class.java).toInstance(plugin)
        bind(CoroutineScope::class.java).toInstance(scope)
        bind(ConfigManager::class.java).toInstance(configManager)
        bind(Logger::class.java).toInstance(LoggerFactory.getLogger(plugin::class.java))
        bind(CacheManager::class.java).toInstance(cacheManager)

        bind(PlayerTimeTracker::class.java).toInstance(
            PlayerTimeTracker(scope, playerTimesCache, playerSessionStartCache)
        )
        bind(MovementTracker::class.java).toInstance(
            MovementTracker(scope, playerMovementsCache, configManager)
        )
        bind(BlockTracker::class.java).toInstance(
            BlockTracker(scope, playerBlockInteractionsCache)
        )
        bind(BedTracker::class.java).toInstance(
            BedTracker(playerBedsCache)
        )
        bind(ExpTracker::class.java).toInstance(
            ExpTracker(playerExpCache)
        )

        bind(JsonManager::class.java).toInstance(
            JsonManager(
                PlayerTimeTracker(scope, playerTimesCache, playerSessionStartCache),
                BedTracker(playerBedsCache),
                MovementTracker(scope, playerMovementsCache, configManager),
                ExpTracker(playerExpCache),
                BlockTracker(scope, playerBlockInteractionsCache)
            )
        )

        bind(KafkaProducerService::class.java).toProvider(KafkaProducerServiceProvider::class.java)
        bind(KafkaConsumerService::class.java).toProvider(KafkaConsumerServiceProvider::class.java)

        bind(MovementListener::class.java).toProvider(MovementListenerProvider::class.java)
        bind(BlockListener::class.java).toProvider(BlockListenerProvider::class.java)
        bind(PlayerEventListener::class.java).toProvider(PlayerEventListenerProvider::class.java)
        bind(KafkaListener::class.java).toProvider(KafkaListenerProvider::class.java)

        bind(SynchronizeCommand::class.java).toProvider(SynchronizeCommandProvider::class.java)
    }

    private inline fun <reified K, reified V> createCache(
        cacheManager: CacheManager,
        cacheName: String,
        heapSize: Long
    ): Cache<K, V> {
        return cacheManager.createCache(
            cacheName,
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                K::class.java,
                V::class.java,
                ResourcePoolsBuilder.heap(heapSize).disk(5, MemoryUnit.GB, true)
            )
        )
    }
}