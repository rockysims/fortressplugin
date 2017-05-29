package me.newyith.fortressOrig.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.newyith.fortressOrig.main.FortressPlugin;
import me.newyith.fortressOrig.util.Blocks;
import me.newyith.fortressOrig.util.Cuboid;
import me.newyith.fortressOrig.util.Debug;
import me.newyith.fortressOrig.util.Point;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class GenPrepData {
	public final ImmutableList<WallLayer> wallLayers;
	public final ImmutableSet<Point> layerAroundWall;
	public final ImmutableSet<Point> pointsInside;
	public final ImmutableSet<Point> layerOutside;

	private GenPrepData(ImmutableList<WallLayer> wallLayers,
						ImmutableSet<Point> layerAroundWall,
						ImmutableSet<Point> pointsInside,
						ImmutableSet<Point> layerOutside) {
		this.wallLayers = wallLayers;
		this.layerAroundWall = layerAroundWall;
		this.pointsInside = pointsInside;
		this.layerOutside = layerOutside;
	}

	public static CompletableFuture<GenPrepData> makeFuture(
			final World world,
			final Point anchorPoint,
			final ImmutableSet<Point> originPoints,
			final ImmutableSet<Material> wallMaterials,
			final ImmutableSet<Point> nearbyClaimedPoints,
			final ImmutableMap<Point, Material> pretendPoints
	) {
		CompletableFuture<GenPrepData> future = CompletableFuture.supplyAsync(() -> {
			//build wallLayers and merge in existing wallLayers
			ImmutableList<WallLayer> wallLayers = WallLayers.scan(world, anchorPoint, originPoints, wallMaterials, nearbyClaimedPoints, pretendPoints);

			//set layerAroundWall
			ImmutableSet<Point> wallPoints = WallLayers.getAllPointsIn(wallLayers);
			ImmutableSet<Point> layerAroundWall = ImmutableSet.copyOf(
					getLayerAround(world, anchorPoint, wallPoints, Blocks.ConnectedThreshold.POINTS).join()
			);

			//set layerOutside and pointsInside
			ImmutableSet<Point> layerOutside = ImmutableSet.copyOf(
					getLayerOutside(world, originPoints, wallPoints, layerAroundWall)
			);
			ImmutableSet<Point> pointsInside = ImmutableSet.copyOf(
					getPointsInside(world, layerOutside, layerAroundWall, wallPoints)
			);

			return new GenPrepData(wallLayers, layerAroundWall, pointsInside, layerOutside);
		});

		return future;
	}

	private static CompletableFuture<Set<Point>> getLayerAround(World world, Point origin, Set<Point> originLayer, Blocks.ConnectedThreshold threshold) {
		Set<Material> traverseMaterials = new HashSet<>(); //no blocks are traversed
		Set<Material> returnMaterials = null; //all blocks are returned
		int rangeLimit = FortressPlugin.config_generationRangeLimit + 1;
		Set<Point> ignorePoints = null; //no points ignored
		return Blocks.getPointsConnected(world, origin, originLayer, traverseMaterials, returnMaterials, rangeLimit, ignorePoints, threshold);
	}

	private static Set<Point> getLayerOutside(World world, Set<Point> originPoints, Set<Point> wallPoints, Set<Point> layerAroundWall) {
		Set<Point> layerOutside = new HashSet<>();

		if (!layerAroundWall.isEmpty()) {
			//find a top block in layerAroundWall
			Point top = layerAroundWall.iterator().next();
			for (Point p : layerAroundWall) {
				if (p.y() > top.y()) {
					top = p;
				}
			}

			//fill layerOutside
			Point origin = top;
			Set<Point> originLayer = new HashSet<>();
			originLayer.add(origin);
			Set<Material> traverseMaterials = null; //traverse all block types
			Set<Material> returnMaterials = null; //return all block types
			int rangeLimit = 2 * (FortressPlugin.config_generationRangeLimit + 1);
			Set<Point> ignorePoints = wallPoints;
			Set<Point> searchablePoints = new HashSet<>(layerAroundWall);
			searchablePoints.addAll(originPoints);
			layerOutside = Blocks.getPointsConnected(world, origin, originLayer,
					traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints
			).join();
			layerOutside.addAll(originLayer);
			layerOutside.retainAll(layerAroundWall); //this is needed because we add origin points to searchablePoints
		}

		return layerOutside;
	}

	private static Set<Point> getPointsInside(World world, Set<Point> layerOutside, Set<Point> layerAroundWall, Set<Point> wallPoints) {
		Set<Point> pointsInside = new HashSet<>();

		if (!layerAroundWall.isEmpty()) {
			//get layerInside
			Set<Point> layerInside = new HashSet<>(layerAroundWall);
			layerInside.removeAll(layerOutside);

			//fill pointsInside
			if (!layerInside.isEmpty()) {
				Point origin = layerInside.iterator().next();
				Set<Point> originLayer = layerInside;
				Set<Material> traverseMaterials = null; //traverse all block types
				Set<Material> returnMaterials = null; //all block types
				int maxReturns = (new Cuboid(world, layerInside)).countBlocks() + 1; //set maxReturns as anti near infinite search (just in case)
				int rangeLimit = 2 * FortressPlugin.config_generationRangeLimit;
				Set<Point> ignorePoints = wallPoints;
				Set<Point> searchablePoints = null; //search all points
				pointsInside = Blocks.getPointsConnected(world, origin, originLayer,
						traverseMaterials, returnMaterials, maxReturns, rangeLimit, ignorePoints, searchablePoints
				).join();
				if (pointsInside.size() == maxReturns) {
					Debug.error("BaseCore::getPointsInside() tried to do infinite search.");
				}
				pointsInside.addAll(originLayer);
			}
		}

		return pointsInside;
	}
}
