package me.newyith.fortress.bedrock.util;

import me.newyith.fortress.util.Particles;
import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ManagedBedrockBase {
	public abstract void convert(World world);
	public abstract void revert(World world);
	public abstract boolean isConverted();
	public abstract Material getMaterial(Point point);

	protected void showParticles(World world, Point p) {
		p = p.add(0.5, 0.0, 0.5);
		Particles.display(Particle.PORTAL, 35, world, p, 0.25F);
	}

	protected Collection<Entity> getLivingEntitiesInRange(World world, Point p, double range) {
		Collection<Entity> entities =  world.getNearbyEntities(p.add(0.5, 0.5, 0.5).toLocation(world), range, range, range);
		entities.removeIf(entity -> !(entity instanceof LivingEntity));
		return entities;
	}
}
