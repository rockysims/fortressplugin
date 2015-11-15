package me.newyith.fortress.generator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class TimedBedrockData {
	public Material material;
	public int waitTicks;

	public TimedBedrockData(Location loc, int waitTicks) {
		Block b = loc.getBlock();
		this.material = b.getType();
		this.waitTicks = waitTicks;
	}
}
