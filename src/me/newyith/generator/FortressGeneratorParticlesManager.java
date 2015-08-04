package me.newyith.generator;

import me.newyith.event.TickTimer;
import me.newyith.particles.ParticleEffect;
import me.newyith.util.Point;
import org.bukkit.Location;

import java.util.Date;

public class FortressGeneratorParticlesManager {
	private FortressGeneratorRune rune;
	private int waitTicks = 0;

	public FortressGeneratorParticlesManager(FortressGeneratorRune rune) {
		this.rune = rune;
	}

	public void tick() {
		if (rune.isRunning()) {
			if (waitTicks <= 0) {
				long now = (new Date()).getTime();
				waitTicks = (1000 + (int)(now % 5000)) / TickTimer.msPerTick;

				Point point = new Point(rune.getPattern().anchorPoint);
				point.add(0, 1, 0);
				float speed = 0;
				int amount = 1;
				double range = 15;
				Location loc = point.add(0.5, -0.4, 0.5);
				ParticleEffect.PORTAL.display(0.2F, 0.0F, 0.2F, speed, amount, loc, range);
			}
			waitTicks--;
		}
	}
}
