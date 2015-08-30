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

	public FortressGeneratorParticlesManager(FortressGeneratorRune rune) {
		this.rune = rune;
	}

	public void tick() {
		tickRuneAnchorParticles();
		tickWallParticles();
	}

	private List<Pair<Point, Point>> wallOutsidePairs = null;
	private int wallOutsideIndex = 0;
	private void tickWallParticles() {
		if (wallWaitTicks <= 0) {
			wallWaitTicks = (500) / TickTimer.msPerTick;

			Debug.start("tickWallParticles()");

				//fill wallOutsidePairs
				if (wallOutsidePairs == null || wallOutsidePairs.isEmpty()) {
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

					Debug.msg("tickWallParticles() filled wallOutsidePairs: " + wallOutsidePairs.size());
					//Debug.stop("tickWallParticles() shuffle", false);
					//Debug.duration("tickWallParticles() shuffle");
					//Debug.clear("tickWallParticles() shuffle");
				}
				//Debug.msg("tickWallParticles() wallOutsidePairs.size(): " + wallOutsidePairs.size() + ", index: " + wallOutsideIndex);

				int limit = (int)((double)wallOutsidePairs.size() * 0.02);
				//Debug.msg("tickWallParticles() limit: " + limit);
				while (limit-- > 0) {
					wallOutsideIndex++;
					if (wallOutsideIndex >= wallOutsidePairs.size()) {
						wallOutsidePairs = null; //refresh
						wallOutsideIndex = 0;
						break;
					}
					Pair<Point, Point> wallOutside = wallOutsidePairs.get(wallOutsideIndex);

					//display particles for wall face of outsidePoint
					Point wall = wallOutside.getKey();
					Point outsidePoint = wallOutside.getValue();
					Point towardWall = new Point(wall.subtract(outsidePoint));
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
					showParticle(p, xRand, yRand, zRand);
				}





//			Set<Point> generated = rune.getGeneratedPoints();
//			if (generated.size() > 0) {
//				Set<Point> layerOutsideFortress = rune.getLayerOutsideFortress();
//
//				layerOutsideFortress.parallelStream().forEach((outsidePoint) -> {
//					Set<Point> adjacents = Wall.getAdjacent6(outsidePoint);
//					adjacents.parallelStream().forEach((adj) -> {
//						if (generated.contains(adj) && adj.getBlock().getType() != Material.AIR) {
//							if (Math.random() >= 0.95) {
//								//display particles for adj (wall) face of outsidePoint
//
//								Point towardWall = new Point(adj.subtract(outsidePoint));
//								Point towardWallAdjusted = new Point(towardWall);
//								double mult = 0.3;
//								towardWallAdjusted.x *= mult;
//								towardWallAdjusted.y *= mult;
//								towardWallAdjusted.z *= mult;
//
//								float xRand = 0.22F;
//								float yRand = 0.22F;
//								float zRand = 0.22F;
//								//don't randomize particle placement in the axis we moved in to go from wall point to adjacent point
//								if (towardWallAdjusted.x != 0) {
//									xRand = 0;
//								}
//								if (towardWallAdjusted.y != 0) {
//									yRand = 0;
//								}
//								if (towardWallAdjusted.z != 0) {
//									zRand = 0;
//								}
//
//								Point p = new Point(outsidePoint);
//								p.setX(p.x + towardWallAdjusted.x + 0.5);
//								p.setY(p.y + towardWallAdjusted.y + 0.5);
//								p.setZ(p.z + towardWallAdjusted.z + 0.5);
//								showParticle(p, xRand, yRand, zRand);
//							}
//						}
//					});
//				});
//			}



			Debug.stop("tickWallParticles()", false);
			Debug.duration("tickWallParticles()");
			Debug.clear("tickWallParticles()");

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
