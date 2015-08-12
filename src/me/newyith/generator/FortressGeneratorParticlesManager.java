package me.newyith.generator;

import me.newyith.event.TickTimer;
import me.newyith.particles.ParticleEffect;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Date;
import java.util.Random;
import java.util.Set;

public class FortressGeneratorParticlesManager {
	private FortressGeneratorRune rune;
	private int runeWaitTicks = 0;
	private static int wallWaitTicks = 0;

	public FortressGeneratorParticlesManager(FortressGeneratorRune rune) {
		this.rune = rune;
	}

	public void tick() {
		if (rune.isRunning()) {
			if (runeWaitTicks <= 0) {
				long now = (new Date()).getTime();
				runeWaitTicks = (1000 + (int)(now % 5000)) / TickTimer.msPerTick;

				Point point = new Point(rune.getPattern().anchorPoint);
				point.add(0, 1, 0);
				float speed = 0;
				int amount = 1;
				double range = 15;
				Location loc = point.add(0.5, -0.4, 0.5);
				ParticleEffect.PORTAL.display(0.2F, 0.0F, 0.2F, speed, amount, loc, range);
			}
			runeWaitTicks--;
		}
	}

	public static void tickProtectionParticles(Set<Point> protectedPoints) {
		if (protectedPoints.size() > 0) {
			if (wallWaitTicks <= 0) {
				wallWaitTicks = (300) / TickTimer.msPerTick;

				for (Point p : protectedPoints) {
					if (p.getBlock().getType() != Material.AIR) {
						Point point = new Point(p);
						point.add(0, 0, 0);
						float speed = 0;
						int amount = 10;
						double range = 15;


						int xOffset;
						int yOffset;
						int zOffset;

						if (Math.random() > 0.5) xOffset = 1;
						else xOffset = -1;
						if (Math.random() > 0.5) yOffset = 1;
						else yOffset = -1;
						if (Math.random() > 0.5) zOffset = 1;
						else zOffset = -1;

						if (Math.random() > 0.3) {
							yOffset = 0;
							zOffset = 0;
						} else if (Math.random() > 0.3) {
							xOffset = 0;
							zOffset = 0;
						} else {
							xOffset = 0;
							yOffset = 0;
						}


						Location loc = point.add(0.5 + xOffset, 0.0 + yOffset, 0.5 + zOffset);
						ParticleEffect.PORTAL.display(0.1F, 0.0F, 0.1F, speed, amount, loc, range);
					}
				}
			}
			wallWaitTicks--;
		}
	}
}
