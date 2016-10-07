package me.newyith.fortress.core.util;

import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class ManagedBedrock extends ManagedBedrockBase {
	private static class Model {
		private Point point;
		private int converts;
		private BlockRevertData revertData;

		@JsonCreator
		public Model(@JsonProperty("point") Point point,
					 @JsonProperty("converts") int converts,
					 @JsonProperty("revertData") BlockRevertData revertData) {
			this.point = point;
			this.converts = converts;
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
		int converts = 0;
		BlockRevertData revertData = new BlockRevertData(world, p);
		model = new Model(p, converts, revertData);
	}

	//-----------------------------------------------------------------------

	public void convert(World world) {
//		Debug.msg("ManagedBedrock::convert() " + model.point);
		model.converts++;
		updateConverted(world);
	}

	public void revert(World world, boolean fullRevert) {
//		Debug.msg("ManagedBedrock::revert() " + model.point);
		model.converts--;
		if (fullRevert) model.converts = 0;
		updateConverted(world);
	}

	public boolean isConverted() {
		return model.converts > 0;
	}

	public Material getMaterial(Point p) {
		return model.revertData.getMaterial();
	}

	private void updateConverted(World world) {
		boolean converted = model.point.is(Material.BEDROCK, world);
		int converts = model.converts;

		if (converts > 0 && !converted) {
			//convert
			model.point.setType(Material.BEDROCK, world);
		} else if (converts <= 0 && converted) {
			//revert
			model.revertData.revert(world, model.point);
		}
	}
}
