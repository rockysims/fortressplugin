package me.newyith.fortress.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

public class WallLayers {
	//this method is (or should be) thread safe
	//TODO: make sure this method is thread safe
	public static ImmutableSet<Point> getAllPointsIn(ImmutableList<WallLayer> wallLayers) {
		//TODO: write
		return null;
	}

	//this method is (or should be) thread safe
	//TODO: make sure this method is thread safe
	public static ImmutableList<WallLayer> merge(ImmutableList<WallLayer> wallLayers, ImmutableList<ImmutableSet<Point>> moreWallLayers) {
		//TODO: write
		return null;
	}

	//this method is (or should be) thread safe
	public static ImmutableList<WallLayer> scan(
			final World world,
			final Point origin,
			final ImmutableSet<Point> originPoints,
			final ImmutableSet<Material> wallMaterials,
			final ImmutableSet<Point> nearbyClaimedPoints,
			final ImmutableMap<Point, Material> pretendPoints // = BedrockManagerNew.forWorld(world).getMaterialByPointMap();
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
		List<WallLayer> wallLayers = foundLayers.stream().map(WallLayer::new).collect(Collectors.toList());

		return ImmutableList.copyOf(wallLayers);
	}
}
