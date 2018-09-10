package me.newyith.fortress.util;

import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ChunkBatch extends BaseUUID {
	private Set<ChunkXZ> chunks = new HashSet<>();

	public ChunkBatch(Set<Chunk> chunks) {
		this.chunks = chunks.stream()
				.map(ChunkXZ::new)
				.collect(Collectors.toSet());
	}

	public boolean contains(Chunk chunk) {
		return chunks.contains(new ChunkXZ(chunk));
	}

	public Set<Chunk> getChunks(World world) {
		return chunks.stream()
				.map(chunkXZ -> world.getChunkAt(chunkXZ.x, chunkXZ.z))
				.collect(Collectors.toSet());
	}

	public Set<ChunkXZ> getChunkXZs() {
		return chunks;
	}

	public int size() {
		return chunks.size();
	}
}
