package me.newyith.generator;

import javafx.util.Pair;
import me.newyith.event.TickTimer;
import me.newyith.particles.ParticleEffect;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

public class FortressGeneratorParticlesManager {
	private FortressGeneratorRune rune;
	private int runeWaitTicks = 0;
	private int wallWaitTicks = 0;
	private List<Pair<Point, Point>> wallOutsidePairs = null;
	private int wallOutsideIndex = 0;
	private long maxTimeNsPerParticleTick = 1000000 * 25; //25ms

	public FortressGeneratorParticlesManager(FortressGeneratorRune rune) {
		this.rune = rune;
	}

	public void tick() {
		tickRuneAnchorParticles();
		tickWallParticles();
	}

	public void onGeneratedChanges() {
		wallOutsidePairs = null; //mark wallOutSidePairs as needing refresh
	}

	private void tickWallParticles() {
		wallWaitTicks--;
		if (wallWaitTicks <= 0) {
			wallWaitTicks = (500) / TickTimer.msPerTick;

			Debug.start("tickWallParticles()");

			//fill wallOutsidePairs (if needed)
			if (wallOutsidePairs == null) {
				//Debug.start("tickWallParticles() shuffle");

				wallOutsidePairs = new ArrayList<>();
				Set<Point> generated = rune.getGeneratedPoints();
				if (generated.size() > 0) {
					Set<Point> layerOutsideFortress = rune.getLayerOutsideFortress();

					layerOutsideFortress.stream().forEach((outsidePoint) -> {
						Set<Point> adjacents = Wall.getAdjacent6(outsidePoint);
						adjacents.stream().forEach((adj) -> {
							if (generated.contains(adj) && adj.getBlock().getType() != Material.AIR) {
								wallOutsidePairs.add(new Pair<>(adj, outsidePoint));
							}
						});
					});

					Collections.shuffle(wallOutsidePairs);
				}

				//Debug.msg("tickWallParticles() filled wallOutsidePairs: " + wallOutsidePairs.size());

				//Debug.stop("tickWallParticles() shuffle", false);
				//Debug.duration("tickWallParticles() shuffle");
				//Debug.clear("tickWallParticles() shuffle");
			}

			if (!wallOutsidePairs.isEmpty()) {
				int runeCount = FortressGeneratorRunesManager.getRuneCount();
				if (runeCount == 0) runeCount++;

				long timeAllottedNs = maxTimeNsPerParticleTick / runeCount;
				long startNs = System.nanoTime();

				int limit = 1 + (int)((double)wallOutsidePairs.size() * 0.02);
				while (true) {
					if (limit-- <= 0) {
						//Debug.msg("particle LIMIT break");
						break;
					}
					if (System.nanoTime() - startNs > timeAllottedNs) {
						//Debug.msg("particle TIME break");
						break;
					}

					wallOutsideIndex++;
					if (wallOutsideIndex >= wallOutsidePairs.size()) {
						Collections.shuffle(wallOutsidePairs);
						wallOutsideIndex = 0;
					}

					//Debug.msg("wallOutsideIndex: " + wallOutsideIndex);

					showParticleForWallOutsidePair(wallOutsidePairs.get(wallOutsideIndex));
				}
			}

			Debug.stop("tickWallParticles()", false);
			//Debug.duration("tickWallParticles()");
			//Debug.clear("tickWallParticles()");
		}
	}

	private void showParticleForWallOutsidePair(Pair<Point, Point> wallOutside) {
		//display particles for wall face of outsidePoint
		Point wall = wallOutside.getKey();
		Point outsidePoint = wallOutside.getValue();
		Point towardWall = wall.difference(outsidePoint);
		Point towardWallAdjusted = new Point(towardWall);
		double mult = 0.3;
		towardWallAdjusted.x *= mult;
		towardWallAdjusted.y *= mult;
		towardWallAdjusted.z *= mult;

		float xRand = 0.22F;
		float yRand = 0.22F;
		float zRand = 0.22F;
		//don't randomize particle placement in the axis we moved in to go from wall point to adjacent point
		if (towardWallAdjusted.x != 0) {
			xRand = 0;
		}
		if (towardWallAdjusted.y != 0) {
			yRand = 0;
		}
		if (towardWallAdjusted.z != 0) {
			zRand = 0;
		}

		Point p = new Point(outsidePoint);
		p.setX(p.x + towardWallAdjusted.x + 0.5);
		p.setY(p.y + towardWallAdjusted.y + 0.5);
		p.setZ(p.z + towardWallAdjusted.z + 0.5);

		ParticleEffect.PORTAL.display(xRand, yRand, zRand, 0, 1, p, 20);
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
