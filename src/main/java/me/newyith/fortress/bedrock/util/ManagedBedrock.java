package me.newyith.fortress.bedrock.util;

import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.material.Button;
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
		BlockRevertData revertData = new BlockRevertData(world, p); //TODO: consider setting to null here instead
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
			model.revertData = new BlockRevertData(world, model.point);

			boolean showParticlesInstead;
			switch (model.revertData.getMaterial()) {
				case WOOD_BUTTON: //buttons can get stuck in pressed state
				case STONE_BUTTON:
					Button button = (Button) model.point.getBlock(world).getState().getData();
					showParticlesInstead = button.isPowered();
					break;
				case DETECTOR_RAIL: //rail with pressure plate can get stuck in pressed state
				case WOOD_PLATE: //pressure plates can get stuck in pressed state
				case STONE_PLATE:
					PressureSensor pressureSensor = (PressureSensor) model.point.getBlock(world).getState().getData();
					showParticlesInstead = pressureSensor.isPressed();
					break;
				case IRON_PLATE: //weighted pressure plates can get stuck in pressed state
				case GOLD_PLATE:
					//can't cast to PressureSensor so just check if it's powered instead
					showParticlesInstead = model.point.getBlock(world).getBlockPower() > 0;
					break;
				default:
					showParticlesInstead = false;
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
