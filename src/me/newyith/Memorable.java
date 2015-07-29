package me.newyith;

import org.bukkit.configuration.ConfigurationSection;

public interface Memorable {
	public void saveTo(Memory m);
	/*
	Note: The following is required but not enforced by the compiler.
	public static Object/Point/etc loadFrom(Memory m);
	*/
}
