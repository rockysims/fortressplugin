package me.newyith.fortress.core;

import javafx.util.Pair;
import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Particles;
import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.*;

public class CoreParticles {
	private int animationWaitTicks = 0;
	private int anchorWaitTicks = 0;
	private int wallWaitTicks = 0;
	private int refreshWaitTicks = 0; //0 means no refresh scheduled
	private List<Pair<Point, Point>> wallOutsidePairs = null;
	private int wallOutsideIndex = 0;
	private long maxTimeNsPerParticleTick = 1000000 * 2; //2ms (per core)

	public void tick(BaseCore core) {
		tickAnchorParticles(core);
		tickWallParticles(core);
	}

	public void onGeneratedChanges() {
		if (refreshWaitTicks <= 0) {
			refreshWaitTicks = 3000 / TickTimer.msPerTick; //schedule wallOutsidePairs refresh in 3 seconds
		} //else refresh already pending
	}

	private void tickWallParticles(BaseCore core) {
		if (refreshWaitTicks > 0) {
			refreshWaitTicks--;
			if (refreshWaitTicks <= 0) {
				wallOutsidePairs = null; //mark wallOutsidePairs as needing refresh
			}
		}

		wallWaitTicks--;
		if (wallWaitTicks <= 0) {
			wallWaitTicks = (500) / TickTimer.msPerTick;

//			Debug.start("tickWallParticles()");

			//fill wallOutsidePairs (if needed)
			if (wallOutsidePairs == null) {
				//Debug.start("tickWallParticles() shuffle");

				wallOutsidePairs = new ArrayList<>();
				Set<Point> generated = core.getGeneratedPoints();
				if (generated.size() > 0) {
					Set<Point> layerOutsideFortress = core.getLayerOutsideFortress();

					layerOutsideFortress.forEach((outsidePoint) -> {
						Set<Point> adjacents = Blocks.getAdjacent6(outsidePoint);
						adjacents.forEach((adj) -> {
							if (generated.contains(adj) && adj.getBlock(core.model.world).getType() != Material.AIR) {
								wallOutsidePairs.add(new Pair<>(adj, outsidePoint));
							}
						});
					});

					Collections.shuffle(wallOutsidePairs);
				}

				//Debug.msg("tickWallParticles() filled wallOutsidePairs: " + wallOutsidePairs.size());
			}

			if (!wallOutsidePairs.isEmpty()) {
				long startNs = System.nanoTime();

				int limit = 1 + (int)((double)wallOutsidePairs.size() * 0.02);
				while (true) {
					if (limit-- <= 0) {
//						Debug.msg("particle LIMIT break");
						break;
					}
					if (System.nanoTime() - startNs > maxTimeNsPerParticleTick) {
//						Debug.msg("particle TIME break");
						break;
					}

					wallOutsideIndex++;
					if (wallOutsideIndex >= wallOutsidePairs.size()) {
						Collections.shuffle(wallOutsidePairs);
						wallOutsideIndex = 0;
					}

//					Debug.msg("wallOutsideIndex: " + wallOutsideIndex);

					showParticleForWallOutsidePair(core.getWorld(), wallOutsidePairs.get(wallOutsideIndex), Particle.PORTAL, 1);
				}
			}

//			Debug.end("tickWallParticles()");
		}
	}

	public void showParticleForWallOutsidePair(World world, Pair<Point, Point> wallOutside, Particle particle, int amount) {
		//display particles for wall face of outsidePoint
		Point wall = wallOutside.getKey();
		Point outsidePoint = wallOutside.getValue();
		Point towardWall = wall.difference(outsidePoint);

		Point towardWallAdjusted = new Point(towardWall);
		double mult = 0.3;
		double x = towardWallAdjusted.x() * mult;
		double y = towardWallAdjusted.y() * mult;
		double z = towardWallAdjusted.z() * mult;
		towardWallAdjusted = new Point(x, y, z);

		float xRand = 0.22F;
		float yRand = 0.22F;
		float zRand = 0.22F;
		//don't randomize particle placement in the axis we moved in to go from wall point to adjacent point
		if (towardWallAdjusted.x() != 0) xRand = 0;
		if (towardWallAdjusted.y() != 0) yRand = 0;
		if (towardWallAdjusted.z() != 0) zRand = 0;

		Point p = outsidePoint.add(towardWallAdjusted).add(0.5, 0.5, 0.5);
		Particles.display(particle, amount, world, p, xRand, yRand, zRand);
	}

	private void tickAnchorParticles(BaseCore core) {
		if (core.isActive()) {
			if (anchorWaitTicks <= 0) {
				long now = System.currentTimeMillis();
				anchorWaitTicks = (1000 + (int)(now % 5000)) / TickTimer.msPerTick;
				displayAnchorParticle(core);
			}
			anchorWaitTicks--;
		}
	}

	public void displayAnchorParticle(BaseCore core) {
		Point point = core.model.anchorPoint.add(0, 1, 0);
		point = point.add(0.5, -0.4, 0.5);
		Particles.display(Particle.PORTAL, 1, core.model.world, point, 0.2F, 0.0F, 0.2F);
	}

	public void tickAnimationParticles(BaseCore core) {
		if (animationWaitTicks <= 0) {
			long now = System.currentTimeMillis();
			animationWaitTicks = (300 + (int)(now % 150)) / TickTimer.msPerTick;
			displayAnchorParticle(core);
		}
		animationWaitTicks--;
	}
}
