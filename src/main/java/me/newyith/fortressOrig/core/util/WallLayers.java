package me.newyith.fortressOrig.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.newyith.fortressOrig.main.FortressPlugin;
import me.newyith.fortressOrig.util.Blocks;
import me.newyith.fortressOrig.util.Debug;
import me.newyith.fortressOrig.util.Point;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

public class WallLayers {
	//this method is (or should be) thread safe
	public static ImmutableSet<Point> getAllPointsIn(ImmutableList<WallLayer> wallLayers) {
		Set<Point> points = wallLayers.stream()
				.flatMap(wallLayer -> {


					if (wallLayer == null) Debug.msg("wallLayer is null");
					if (wallLayer.getPoints() == null) Debug.msg("wallLayer.getPoints() is null");
					if (wallLayer.getPoints().stream() == null) Debug.msg("wallLayer.getPoints().stream() is null");


					return wallLayer.getPoints().stream();
				})
				.collect(Collectors.toSet());

		return ImmutableSet.copyOf(points);
	}

	//this method is (or should be) thread safe
	public static ImmutableList<WallLayer> scan(
			final World world,
			final Point origin,
			final ImmutableSet<Point> originPoints,
			final ImmutableSet<Material> wallMaterials,
			final ImmutableSet<Point> nearbyClaimedPoints,
			final ImmutableMap<Point, Material> pretendPoints
	) {
		//set foundLayers to all connected wall points ignoring (and not traversing) nearbyClaimedPoints
		ImmutableSet<Material> traverseMaterials = wallMaterials;
		ImmutableSet<Material> returnMaterials = wallMaterials;
		int maxReturns = FortressPlugin.config_generationBlockLimit;
		int rangeLimit = FortressPlugin.config_generationRangeLimit;
		ImmutableSet<Point> ignorePoints = nearbyClaimedPoints;
		List<Set<Point>> foundLayers = Blocks.getPointsConnectedAsLayers(
				world, origin, originPoints, traverseMaterials, returnMaterials,
				maxReturns, rangeLimit, ignorePoints, pretendPoints
		).join();

		//build wallLayers from foundLayers
		List<WallLayer> wallLayers = foundLayers.stream()
				.map(layerPoints -> {
					Set<Point> cobblePoints = layerPoints.stream()
							.filter(p -> pretendPoints.get(p) == Material.COBBLESTONE || p.is(Material.COBBLESTONE, world))
							.collect(Collectors.toSet());
					return new WallLayer(layerPoints, cobblePoints);
				})
				.collect(Collectors.toList());

		return ImmutableList.copyOf(wallLayers);
	}
}
