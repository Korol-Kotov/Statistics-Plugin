package com.github.saintedlittle.extensions

import com.github.saintedlittle.data.PotionEffectData
import org.bukkit.Material
import org.bukkit.Statistic
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

fun Player.collectStatistics(): Map<String, Int> {
    val statisticsMap = mutableMapOf<String, Int>()

    Statistic.entries.forEach { statistic ->
        when (statistic.type) {
            Statistic.Type.BLOCK -> Material.entries.asSequence()
                .filter { it.isBlock }
                .forEach { material ->
                    statisticsMap.safeAdd("${statistic.name}_${material.name}", checkPositive = true)
                    { getStatistic(statistic, material) }
                }

            Statistic.Type.ITEM -> Material.entries.asSequence()
                .filter { it.isItem }
                .forEach { material ->
                    statisticsMap.safeAdd("${statistic.name}_${material.name}", checkPositive = true)
                    { getStatistic(statistic, material) }
                }

            Statistic.Type.ENTITY -> EntityType.entries.asSequence()
                .forEach { entityType ->
                    statisticsMap.safeAdd("${statistic.name}_${entityType.name}", checkPositive = true)
                    { getStatistic(statistic, entityType) }
                }

            else -> statisticsMap.safeAdd(statistic.name) { getStatistic(statistic) }
        }
    }

    return statisticsMap
}

private fun MutableMap<String, Int>.safeAdd(key: String, checkPositive: Boolean = false, valueProvider: () -> Int) {
    try {
        if (checkPositive) {
            val value = valueProvider()
            if (value > 0) this[key] = value
        } else this[key] = valueProvider()
    } catch (ignored: IllegalArgumentException) {}
}

fun Player.collectAttributes(): Map<String, Double> {
    return Attribute.entries.mapNotNull { attribute ->
        getAttribute(attribute)?.value?.let { attribute.name to it }
    }.toMap()
}

fun Player.collectPotionEffects(): List<PotionEffectData> {
    return activePotionEffects.map { it.toPotionEffectData() }
}

fun PotionEffect.toPotionEffectData(): PotionEffectData {
    return PotionEffectData(
        type = type.name,
        amplifier = amplifier,
        duration = duration
    )
}