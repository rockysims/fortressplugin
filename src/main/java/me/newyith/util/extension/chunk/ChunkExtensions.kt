package me.newyith.util.extension.chunk

import me.newyith.util.ChunkPos
import org.bukkit.Chunk

fun Chunk.getPos(): ChunkPos {
	return ChunkPos(this)
}
