package me.newyith.generator;

import me.newyith.event.TickTimer;
import me.newyith.particles.ParticleEffect;
import me.newyith.util.Point;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Date;
import java.util.Set;

public class FortressGeneratorParticlesManager {
	private FortressGeneratorRune rune;
	private int runeWaitTicks = 0;
	private int wallWaitTicks = 0;

	public FortressGeneratorParticlesManager(FortressGeneratorRune rune) {
		this.rune = rune;
	}

	public void tick() {
		tickRuneAnchorParticles();
		tickWallParticles();
	}

	private void tickWallParticles() {
		if (wallWaitTicks <= 0) {
			wallWaitTicks = (500) / TickTimer.msPerTick;

			Set<Point> layerOutsideFortress = rune.getLayerOutsideFortress();
			Set<Point> generated = rune.getGeneratedPoints();
			for (Point outsidePoint : layerOutsideFortress) {
				Set<Point> adjacents = Wall.getAdjacent6(outsidePoint);
				for (Point adj : adjacents) {
					if (generated.contains(adj) && adj.getBlock().getType() != Material.AIR) {
						if (Math.random() < 0.97) {
							continue;
						}

						//display particles for adj (wall) face of outsidePoint

						Point towardAdj = new Point(adj.subtract(outsidePoint));
						Point towardAdjAdjusted = new Point(towardAdj);
						double mult = 0.3;
						towardAdjAdjusted.x *= mult;
						towardAdjAdjusted.y *= mult;
						towardAdjAdjusted.z *= mult;

						float xRand = 0.22F;
						float yRand = 0.22F;
						float zRand = 0.22F;
						//don't randomize particle placement in the axis we moved in to go from wall point to adjacent point
						if (towardAdjAdjusted.x != 0) {
							xRand = 0;
						}
						if (towardAdjAdjusted.y != 0) {
							yRand = 0;
						}
						if (towardAdjAdjusted.z != 0) {
							zRand = 0;
						}

						Point p = new Point(outsidePoint);
						p.setX(p.x + towardAdjAdjusted.x + 0.5);
						p.setY(p.y + towardAdjAdjusted.y + 0.5);
						p.setZ(p.z + towardAdjAdjusted.z + 0.5);
						showParticle(p, xRand, yRand, zRand);
					}
				}
			}
		}
		wallWaitTicks--;
	}

	private void showParticle(Point p, float xRand, float yRand, float zRand) {
		ParticleEffect.PORTAL.display(xRand, yRand, zRand, 0, 1, p, 15);
	}

	private void tickRuneAnchorParticles() {
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
}
