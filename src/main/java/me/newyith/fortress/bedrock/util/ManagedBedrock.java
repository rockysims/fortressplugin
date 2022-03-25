package me.newyith.fortress.bedrock.util;

import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Switch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.material.PressureSensor;

public class ManagedBedrock extends ManagedBedrockBase {
	private static class Model {
		private Point point;
		private boolean isConverted;
		private BlockRevertData revertData;

		@JsonCreator
		public Model(@JsonProperty("point") Point point,
					 @JsonProperty("isConverted") boolean isConverted,
					 @JsonProperty("revertData") BlockRevertData revertData) {
			this.point = point;
			this.isConverted = isConverted;
			this.revertData = revertData;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public ManagedBedrock(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public ManagedBedrock(World world, Point p) {
		boolean isConverted = false;
		BlockRevertData revertData = new BlockRevertData(world, p);
		model = new Model(p, isConverted, revertData);
	}

	//-----------------------------------------------------------------------

	public void convert(World world) {
		model.isConverted = true;
		updateConverted(world);
	}

	public void revert(World world) {
		model.isConverted = false;
		updateConverted(world);
	}

	public boolean isConverted() {
		return model.isConverted;
	}

	public Material getMaterial(Point p) {
		return model.revertData.getMaterial();
	}

	private void updateConverted(World world) {
		boolean isBedrock = model.point.is(Material.BEDROCK, world);
		boolean isConverted = model.isConverted;

		if (isConverted && !isBedrock) {
			//convert

			Block block = model.point.getBlock(world);
			BlockData blockData = block.getBlockData();
			Material mat = model.revertData.getMaterial();
			boolean showParticlesInstead = false;

			showParticlesInstead = block.getState() instanceof Container;

			if (!showParticlesInstead) {
				boolean showParticlesInsteadWhenPressed = false
					|| blockData instanceof PressureSensor; //detector rail is considered a PressureSensor
				if (showParticlesInsteadWhenPressed) {
					showParticlesInstead = ((PressureSensor)blockData).isPressed(); //otherwise pressure plates can get stuck in pressed state
				}
			}

			if (!showParticlesInstead) {
				boolean showParticlesInsteadWhenPowered = false
					|| blockData instanceof Switch; //buttons are considered a Switch
				if (showParticlesInsteadWhenPowered) {
					showParticlesInstead = ((Switch)blockData).isPowered(); //otherwise buttons can get stuck in pressed state
				}
			}

			if (!showParticlesInstead) {
				boolean showParticlesInsteadWhenAnalogPowered = false
					|| blockData instanceof AnaloguePowerable; //light/heavy pressure plate is considered an AnaloguePowerable
				if (showParticlesInsteadWhenAnalogPowered) {
					showParticlesInstead = ((AnaloguePowerable)blockData).getPower() > 0; //otherwise buttons can get stuck in pressed state
				}
			}

			if (!showParticlesInstead) {
				boolean showParticlesInsteadWhenNearbyLivingEntities = false
					|| blockData instanceof Switch
					|| blockData instanceof Stairs
					|| blockData instanceof Slab
					|| mat == Material.DIRT_PATH;
				if (showParticlesInsteadWhenNearbyLivingEntities) {
					showParticlesInstead = getLivingEntitiesInRange(world, model.point, 2).size() > 0; //otherwise potential security breach (due to entities getting pushed around)
				}
			}

			if (showParticlesInstead) {
				showParticles(world, model.point);
			} else {
				model.point.setType(Material.BEDROCK, world);
			}
		} else if (!isConverted && isBedrock) {
			//revert
			model.revertData.revert(world, model.point);
		}
	}
}
