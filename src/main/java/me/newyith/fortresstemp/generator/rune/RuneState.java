package me.newyith.fortresstemp.generator.rune;

import me.newyith.fortress.util.Debug;

enum RuneState {
	RUNNING,
	PAUSED,
	NEEDS_FUEL,
	NULL;

	public static RuneState fromInt(int ordinal) {
		RuneState runeState = RuneState.NULL;

		if (RuneState.RUNNING.ordinal() == ordinal) {
			runeState = RuneState.RUNNING;
		} else if (RuneState.PAUSED.ordinal() == ordinal) {
			runeState = RuneState.PAUSED;
		} else if (RuneState.NEEDS_FUEL.ordinal() == ordinal) {
			runeState = RuneState.NEEDS_FUEL;
		} else {
			Debug.error("ERROR: RuneState.fromInt(" + ordinal + ") did not match any state.");
		}

		return runeState;
	}
}