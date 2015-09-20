package me.newyith.particle;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public enum ColoredParticle {

	SPELL_MOB("SPELL_MOB"),
	SPELL_MOB_AMBIENT("SPELL_MOB_AMBIENT");

	String name;

	ColoredParticle(String name) {
		this.name = name;
	}

	public void display(Location location, List<Player> players, int r, int g, int b) {
		ParticleEffect.valueOf(name).display(r/255, g / 255, b / 255, 1, 0, location, players);
	}

	public void display(Location location, int Distance, int r, int g, int b) {
		ParticleEffect.valueOf(name).display(r/255, g / 255, b / 255, 1, 0, location, Distance);
	}

}