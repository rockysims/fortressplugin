package me.newyith.fortress.bedrock.util;

import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class BlockRevertData {
	private static class Model {
		private final Material material;
		private byte data;

		@JsonCreator
		public Model(@JsonProperty("material") Material material,
					 @JsonProperty("data") byte data) {
			this.material = material;
			this.data = data;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BlockRevertData(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BlockRevertData(World world, Point p) {
		Block b = p.getBlock(world);
		Material material = b.getType();
		byte data = b.getState().getData().getData();
		model = new Model(material, data);
		if (material == Material.BEDROCK) Debug.warn("Saved BEDROCK as revertData material at " + p);
	}

	//-----------------------------------------------------------------------

	public void revert(World world, Point p) {
		Block b = p.getBlock(world);

		switch (model.material) {
			case TORCH:
			case REDSTONE_TORCH_ON:
			case REDSTONE_TORCH_OFF:
				//set material and data at the same time so torches on walls don't fall down
				b.setTypeIdAndData(model.material.getId(), model.data, false);
				break;
			default:
				b.setType(model.material);
				BlockState state = b.getState();
				MaterialData materialData = new MaterialData(model.material, model.data);
				state.setData(materialData);
				state.update();
				break;
		}
	}

	public Material getMaterial() {
		return model.material;
	}
}
