package me.newyith.fortress.core.util;

import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ManagedBedrockBase {
	public abstract void convert(World world);
	public abstract void revert(World world, boolean fullRevert);
	public abstract boolean isConverted();
	public abstract Material getMaterial(Point point);
}
