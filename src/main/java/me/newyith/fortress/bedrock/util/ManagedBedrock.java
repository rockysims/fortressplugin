package me.newyith.fortress.bedrock.util;

import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
			model.point.setType(Material.BEDROCK, world);
		} else if (!isConverted && isBedrock) {
			//revert
			model.revertData.revert(world, model.point);
		}
	}
}
