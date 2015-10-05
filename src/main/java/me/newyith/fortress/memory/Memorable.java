package me.newyith.fortress.memory;

public interface Memorable {
	void saveTo(Memory m);
	/*
	Note: The following is required but not enforced by the compiler.
	public static Object/Point/etc loadFrom(Memory m);
	*/
}
