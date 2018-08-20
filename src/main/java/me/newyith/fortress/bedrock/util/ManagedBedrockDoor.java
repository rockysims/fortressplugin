package me.newyith.fortress.bedrock.util;

import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.particle.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

public class ManagedBedrockDoor extends ManagedBedrockBase {
	private static class Model {
		private boolean isConverted;
		private final Point top;
		private final Point bottom;
		private BlockRevertData topRevertData;
		private BlockRevertData bottomRevertData;

		@JsonCreator
		public Model(@JsonProperty("isConverted") boolean isConverted,
					 @JsonProperty("top") Point top,
					 @JsonProperty("bottom") Point bottom,
					 @JsonProperty("topRevertData") BlockRevertData topRevertData,
					 @JsonProperty("bottomRevertData") BlockRevertData bottomRevertData) {
			this.isConverted = isConverted;
			this.top = top;
			this.bottom = bottom;
			this.topRevertData = topRevertData;
			this.bottomRevertData = bottomRevertData;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public ManagedBedrockDoor(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public ManagedBedrockDoor(World world, Point top, Point bottom) {
		boolean isConverted = false;
		BlockRevertData topRevertData = new BlockRevertData(world, top); //TODO: consider setting to null here instead

		BlockRevertData bottomRevertData = null;
		if (bottom != null) {
			bottomRevertData = new BlockRevertData(world, bottom); //TODO: consider setting to null here instead
		}
		model = new Model(isConverted, top, bottom, topRevertData, bottomRevertData);
	}

	//-----------------------------------------------------------------------

	@Override
	public void convert(World world) {
		model.isConverted = true;
		updateConverted(world);
	}

	@Override
	public void revert(World world) {
		model.isConverted = false;
		updateConverted(world);
	}

	@Override
	public boolean isConverted() {
		return model.isConverted;
	}

	@Override
	public Material getMaterial(Point p) {
		Material mat = null;
		if (p.equals(model.top)) mat = model.topRevertData.getMaterial();
		if (p.equals(model.bottom)) mat = model.bottomRevertData.getMaterial();
		return mat;
	}

	public boolean isTallDoor() {
		return model.bottom != null;
	}

	public Point getTop() {
		return model.top;
	}

	public Point getBottom() {
		return model.bottom;
	}

	// utils //

	private void updateConverted(World world) {
		boolean isBedrock = model.top.is(Material.BEDROCK, world);
		boolean isConverted = model.isConverted;

		if (isConverted && !isBedrock) {
			//convert
			boolean potentialSecurityBreach = livingEntitiesInRange(world, model.top, 1) > 0;
			if (potentialSecurityBreach) {
				//show particles instead of changing to bedrock
				showParticles(world, model.top);
				if (model.bottom != null) {
					showParticles(world, model.bottom);
				}
			} else {
				//change to bedrock
				model.topRevertData = new BlockRevertData(world, model.top);
				if (model.bottom != null) {
					model.bottomRevertData = new BlockRevertData(world, model.bottom);
					model.bottom.getBlock(world).setType(Material.BEDROCK, false);
				}
				model.top.setType(Material.BEDROCK, world);
			}
		} else if (!isConverted && isBedrock) {
			//revert
			model.topRevertData.revert(world, model.top);
			//isBedrock condition is to prevent chance of duplicating blocks if plugin save gets out of sync with world save
			if (model.bottom != null && model.bottom.is(Material.BEDROCK, world)) {
				model.bottomRevertData.revert(world, model.bottom);
			}
		}
	}

	private int livingEntitiesInRange(World world, Point p, double range) {
		Collection<Entity> entities =  world.getNearbyEntities(p.add(0.5, 0.5, 0.5).toLocation(world), range, range, range);
		entities.removeIf(entity -> !(entity instanceof LivingEntity));
		return entities.size();
	}

	private void showParticles(World world, Point p) {
		float rand = 0.25F;
		Location loc = p.add(0.5, 0.0, 0.5).toLocation(world);
		ParticleEffect.PORTAL.display(rand, rand, rand, 0, 35, loc, 20);
	}
}
