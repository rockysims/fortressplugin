package me.newyith.fortress.config

data class ConfigData(
	val glowstoneDustBurnTimeMs: Int,
	val stuckDelayMs: Int,
	val stuckCancelDistance: Int,
	val generationRangeLimit: Int,
	val generationBlockLimit: Int
)