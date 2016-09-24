package me.newyith.fortress.core.util;

import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.particle.ParticleEffect;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Collection;

public class ManagedBedrockDoor extends ManagedBedrockBase {
	private static class Model {
		private int converts;
		private final Point top;
		private final Point bottom;
		private BlockRevertData topRevertData;
		private BlockRevertData bottomRevertData;

		@JsonCreator
		public Model(@JsonProperty("converts") int converts,
					 @JsonProperty("top") Point top,
					 @JsonProperty("bottom") Point bottom,
					 @JsonProperty("topRevertData") BlockRevertData topRevertData,
					 @JsonProperty("bottomRevertData") BlockRevertData bottomRevertData) {
			this.converts = converts;
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
		int converts = 0;
		BlockRevertData topRevertData = new BlockRevertData(world, top);

		BlockRevertData bottomRevertData = null;
		if (bottom != null) {
			bottomRevertData = new BlockRevertData(world, bottom);
		}
		model = new Model(converts, top, bottom, topRevertData, bottomRevertData);
	}
//	new
//	public ManagedBedrockDoor(World world, Point top, Point bottom) {
//		int converts = 0;
//		model = new Model(converts, top, bottom, null, null);
//	}

	//-----------------------------------------------------------------------

	@Override
	public void convert(World world) {
//		Debug.msg("ManagedBedrockDoor::convert() " + model.top + " ~ " + model.bottom);
		model.converts++;
		updateConverted(world);
	}

	@Override
	public void revert(World world, boolean fullRevert) {
//		Debug.msg("ManagedBedrockDoor::revert() " + model.top + " ~ " + model.bottom);
		model.converts--;
		if (fullRevert) model.converts = 0;
		updateConverted(world);
	}

	@Override
	public boolean isConverted() {
		return model.converts > 0;
	}

	@Override
	public Material getMaterial(Point p) {
		Material mat = null;
		if (p.equals(model.top)) mat = model.topRevertData.getMaterial();
		if (p.equals(model.bottom)) mat = model.bottomRevertData.getMaterial();
		return mat;
	}

	public Point getTop() {
		return model.top;
	}

	public Point getBottom() {
		return model.bottom;
	}

	// utils //

	private void updateConverted(World world) {
		boolean converted = model.top.is(Material.BEDROCK, world);
		int converts = model.converts;

		if (converts > 0 && !converted) {
			//convert
			boolean potentialSecurityBreach = livingEntitiesInRange(world, model.top, 1) > 0;
			if (potentialSecurityBreach) {
				//show particles instead of changing to bedrock
				float rand = 0.25F;
				if (model.bottom == null) {
					Point top = model.top.add(0.5, 0.0, 0.5);
					ParticleEffect.PORTAL.display(rand, rand, rand, 0, 35, top.toLocation(world), 20);
				} else {
					if (model.converts % 2 == 0) {
						Point bottom = model.bottom.add(0.5, 0.0, 0.5);
						ParticleEffect.PORTAL.display(rand, rand, rand, 0, 35, bottom.toLocation(world), 20);
					} else {
						Point top = model.top.add(0.5, 0.0, 0.5);
						ParticleEffect.PORTAL.display(rand, rand, rand, 0, 35, top.toLocation(world), 20);
					}
				}
			} else {
				//change to bedrock
				model.topRevertData = new BlockRevertData(world, model.top);
				if (model.bottom != null) {
					model.bottomRevertData = new BlockRevertData(world, model.bottom);
					//Debug.msg("saved bottomRevertData material: " + model.bottomRevertData.getMaterial() + " -------------------");
					model.bottom.setType(Material.BEDROCK, world);
				}
				model.top.setType(Material.BEDROCK, world);
			}
		} else if (converts <= 0 && converted) {
			//revert

			//isBedrock condition is to prevent chance of duplicating blocks if plugin save gets out of sync with world save
			if (model.bottom != null && model.bottom.is(Material.BEDROCK, world)) {
				model.bottomRevertData.revert(world, model.bottom);
			}
			model.topRevertData.revert(world, model.top);
		}
	}

	private int livingEntitiesInRange(World world, Point p, double range) {
		Collection<Entity> entities =  world.getNearbyEntities(p.add(0.5, 0.5, 0.5).toLocation(world), range, range, range);
		entities.removeIf(entity -> !(entity instanceof LivingEntity));
		return entities.size();
	}
}
