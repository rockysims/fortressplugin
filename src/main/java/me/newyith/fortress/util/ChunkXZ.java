package me.newyith.fortress.util;

import org.bukkit.Chunk;

public class ChunkXZ {
	public final int x;
	public final int z;

	public ChunkXZ(Chunk chunk) {
		this.x = chunk.getX();
		this.z = chunk.getZ();
	}

	// - Public Utils - //

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(x);
		s.append(", ");
		s.append(z);
		return s.toString();
	}

	// - Overrides - //

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;

		if (o instanceof ChunkXZ) {
			ChunkXZ c = (ChunkXZ)o;
			return x == c.x && z == c.z;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return 49999 * x + z;
	}
}
