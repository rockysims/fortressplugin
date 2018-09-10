package me.newyith.fortress.util;

import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.*;

public class Chunks {
	private static Map<String, Set<ChunkBatch>> keepLoadedBatchesByWorld = new HashMap<>();
	private static Map<String, Set<ChunkXZ>> keepLoadedChunkXZsByWorld = new HashMap<>();

	public static boolean onChunkUnload(Chunk chunk) {
		boolean cancel = false;

		World world = chunk.getWorld();
		Set<ChunkXZ> keepLoadedChunkXZs = chunkXZsForWorld(world);
		if (keepLoadedChunkXZs.contains(new ChunkXZ(chunk))) {
			cancel = true;
		}

		return cancel;
	}

	public static void loadAndPreventUnload(World world, ChunkBatch batch) {
		batchesForWorld(world).add(batch);
		keepLoadedChunkXZsByWorld.remove(world.getName()); //ensure recompute on next access
		for (Chunk chunk : batch.getChunks(world)) {
			chunk.load();
		}
	}

	public static void allowUnload(World world, ChunkBatch batch) {
		batchesForWorld(world).remove(batch);
		keepLoadedChunkXZsByWorld.remove(world.getName()); //ensure recompute on next access
	}

	public static ChunkBatch inRange(World world, Point origin, int range) {
		Set<Chunk> chunks = new HashSet<>();
		int rangeOver = range + 16;
		for (int xOffsetOver = -1 * rangeOver; xOffsetOver <= rangeOver; xOffsetOver += 16) {
			for (int zOffsetOver = -1 * rangeOver; zOffsetOver <= rangeOver; zOffsetOver += 16) {
				int xOffset = Math.min(Math.max(xOffsetOver, -1 * range), range);
				int zOffset = Math.min(Math.max(zOffsetOver, -1 * range), range);
				Point p = origin.add(xOffset, 0, zOffset);
				chunks.add(p.toChunk(world));
			}
		}

		return new ChunkBatch(chunks);
	}

	// util //

	private static Set<ChunkBatch> batchesForWorld(World world) {
		return keepLoadedBatchesByWorld.computeIfAbsent(world.getName(), key -> new HashSet<>());
	}

	private static Set<ChunkXZ> chunkXZsForWorld(World world) {
		return keepLoadedChunkXZsByWorld.computeIfAbsent(world.getName(), key -> {
			Set<ChunkXZ> chunkXZs = new HashSet<>();

			Set<ChunkBatch> keepLoadedBatches = batchesForWorld(world);
			for (ChunkBatch keepLoadedBatch : keepLoadedBatches) {
				chunkXZs.addAll(keepLoadedBatch.getChunkXZs());
			}

			return chunkXZs;
		});
	}
}
