package me.newyith.fortressOrig.bedrock.util;

import me.newyith.fortressOrig.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ManagedBedrockBase {
	public abstract void convert(World world);
	public abstract void revert(World world);
	public abstract boolean isConverted();
	public abstract Material getMaterial(Point point);
}
