package me.newyith.fortress_try1.rune.generator

enum class GeneratorState {
	NULL,
	RUNNING,
	PAUSED,
	NEEDS_FUEL;

	//TODO: consider uncommenting out (and maybe making back into companion object) toInt() and fromInt() if needed for save/load
//	fun toInt(): Int {
//		return this.ordinal
//	}
//
//	fun fromInt(): GeneratorState {
//		return when (this.ordinal) {
//			GeneratorState.RUNNING.ordinal -> GeneratorState.RUNNING
//			GeneratorState.PAUSED.ordinal -> GeneratorState.PAUSED
//			GeneratorState.NEEDS_FUEL.ordinal -> GeneratorState.NEEDS_FUEL
//			else -> {
//				Log.error("ERROR: state.fromInt(${this.ordinal}) did not match any state.")
//				GeneratorState.NULL
//			}
//		}
//	}
}