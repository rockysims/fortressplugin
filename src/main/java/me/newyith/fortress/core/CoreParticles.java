package me.newyith.fortress.core;

import javafx.util.Pair;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.particle.ParticleEffect;
import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.util.Blocks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CoreParticles {
	private int anchorWaitTicks = 0;
	private int wallWaitTicks = 0;
	private List<Pair<Point, Point>> wallOutsidePairs = null;
	private int wallOutsideIndex = 0;
	private long maxTimeNsPerParticleTick = 1000000 * 2; //2ms (per core)
	private final Random random = new Random();

	public void tick(BaseCore core) {
		tickAnchorParticles(core);
		tickWallParticles(core);
	}







	public void showRipple(BaseCore core, Player player, Block block, BlockFace face) {
		Point origin = new Point(block);

		//get rippleLayers
		World world = block.getWorld();
		int layerLimit = 20;
		Set<Point> searchablePoints = core.getGeneratedPoints();
		CompletableFuture<List<Set<Point>>> future = Blocks.getPointsConnectedAsLayers(world, origin, layerLimit - 1, searchablePoints);
		future.join(); //wait for future to resolve
		List<Set<Point>> rippleLayersFromFuture = future.getNow(null);

		if (rippleLayersFromFuture != null) {
			Set<Point> originLayer = new HashSet<>();
			originLayer.add(origin);
			List<Set<Point>> rippleLayers = new ArrayList<>();
			rippleLayers.add(originLayer);
			rippleLayers.addAll(rippleLayersFromFuture);

			//remove some blocks from last 4 rippleLayers to create a fizzle out effect
			for (int i = 0; i < 4; i++) {
				int index = (layerLimit-1) - i;
				if (index < rippleLayers.size()) {
					Set<Point> rippleLayer = rippleLayers.get(index);
					if (rippleLayer != null) {
						Iterator<Point> it = rippleLayer.iterator();
						while (it.hasNext()) {
							it.next();
							int percentSkipChance = 0;
							if (i == 0) percentSkipChance = 75;
							else if (i == 1) percentSkipChance = 60;
							else if (i == 2) percentSkipChance = 45;
							else if (i == 3) percentSkipChance = 30;
							if (random.nextInt(99) < percentSkipChance) {
								it.remove();
							}
						}
					}
				}
			}

			int layerIndex = 0;
			for (Set<Point> layer : rippleLayers) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(FortressPlugin.getInstance(), () -> {
					for (Point p : layer) {
						TimedBedrockManager.convert(world, p, 2000);
					}
				}, layerIndex * 3); //20 ticks per second

				layerIndex++;
			}
		}
	}








	public void onGeneratedChanges() {
		wallOutsidePairs = null; //mark wallOutSidePairs as needing refresh
	}

	private void tickWallParticles(BaseCore core) {
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

					layerOutsideFortress.stream().forEach((outsidePoint) -> {
						Set<Point> adjacents = Blocks.getAdjacent6(outsidePoint);
						adjacents.stream().forEach((adj) -> {
							if (generated.contains(adj) && adj.getBlock(core.model.world).getType() != Material.AIR) {
								wallOutsidePairs.add(new Pair<>(adj, outsidePoint));
							}
						});
					});

					Collections.shuffle(wallOutsidePairs);
				}

				//Debug.msg("tickWallParticles() filled wallOutsidePairs: " + wallOutsidePairs.size());

//				Debug.end("tickWallParticles() shuffle");
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

					showParticleForWallOutsidePair(core.getWorld(), wallOutsidePairs.get(wallOutsideIndex));
				}
			}

//			Debug.end("tickWallParticles()");
		}
	}

	private void showParticleForWallOutsidePair(World world, Pair<Point, Point> wallOutside) {
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
		if (towardWallAdjusted.x() != 0) {
			xRand = 0;
		}
		if (towardWallAdjusted.y() != 0) {
			yRand = 0;
		}
		if (towardWallAdjusted.z() != 0) {
			zRand = 0;
		}

		Point p = outsidePoint.add(towardWallAdjusted).add(0.5, 0.5, 0.5);
		ParticleEffect.PORTAL.display(xRand, yRand, zRand, 0, 1, p.toLocation(world), 20);
	}

	private void tickAnchorParticles(BaseCore core) {
		if (core.getGeneratedPoints().size() > 0) {
			if (anchorWaitTicks <= 0) {
				long now = (new Date()).getTime();
				anchorWaitTicks = (1000 + (int)(now % 5000)) / TickTimer.msPerTick;
				displayAnchorParticle(core);
			}
			anchorWaitTicks--;
		}
	}

	public void displayAnchorParticle(BaseCore core) {
		Point point = core.model.anchorPoint.add(0, 1, 0);
		float speed = 0;
		int amount = 1;
		double range = 15;
		point = point.add(0.5, -0.4, 0.5);
		ParticleEffect.PORTAL.display(0.2F, 0.0F, 0.2F, speed, amount, point.toLocation(core.model.world), range);
	}
}
