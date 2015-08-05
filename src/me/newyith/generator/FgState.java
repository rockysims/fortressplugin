package me.newyith.generator;

import me.newyith.util.Debug;

enum FgState {
	RUNNING,
	PAUSED,
	NEEDS_FUEL,
	NULL;

	public static FgState fromInt(int ordinal) {
		FgState fgState = FgState.NULL;

		if (FgState.RUNNING.ordinal() == ordinal) {
			fgState = FgState.RUNNING;
		} else if (FgState.PAUSED.ordinal() == ordinal) {
			fgState = FgState.PAUSED;
		} else if (FgState.NEEDS_FUEL.ordinal() == ordinal) {
			fgState = FgState.NEEDS_FUEL;
		} else {
			Debug.msg("ERROR: FgState.fromInt(" + ordinal + ") did not match any state.");
		}

		return fgState;
	}
}