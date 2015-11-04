package me.newyith.fortress.generator.rune;

import me.newyith.fortressold.util.Debug;

public enum GeneratorState {
	NULL,
	RUNNING,
	PAUSED,
	NEEDS_FUEL;

	//-----------------------------------------------------------------------

	public static int toInt(GeneratorState state) {
		return state.ordinal();
	}

	public static GeneratorState fromInt(int ordinal) {
		GeneratorState fgState = GeneratorState.NULL;

		if (GeneratorState.RUNNING.ordinal() == ordinal) {
			fgState = GeneratorState.RUNNING;
		} else if (GeneratorState.PAUSED.ordinal() == ordinal) {
			fgState = GeneratorState.PAUSED;
		} else if (GeneratorState.NEEDS_FUEL.ordinal() == ordinal) {
			fgState = GeneratorState.NEEDS_FUEL;
		} else {
			Debug.error("ERROR: FgState.fromInt(" + ordinal + ") did not match any state.");
		}

		return fgState;
	}
}