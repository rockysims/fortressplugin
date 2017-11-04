package me.newyith.util.extension.chunk

import me.newyith.util.ChunkAnchor
import org.bukkit.Chunk

fun Chunk.getAnchor(): ChunkAnchor {
	return ChunkAnchor(this)
}
