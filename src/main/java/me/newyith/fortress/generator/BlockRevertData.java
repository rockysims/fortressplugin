package me.newyith.fortress.generator;

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
		private final MaterialData materialData;

		@JsonCreator
		public Model(@JsonProperty("material") Material material,
					 @JsonProperty("materialData") MaterialData materialData) {
			this.material = material;
			this.materialData = materialData;

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
		MaterialData materialData = b.getState().getData();
		model = new Model(material, materialData);
	}

	//-----------------------------------------------------------------------

	public void revert(World world, Point p) {
		Block b = p.getBlock(world);
		b.setType(model.material);
		BlockState state = b.getState();
		state.setData(model.materialData);
		state.update();
	}

	public Material getMaterial() {
		return model.material;
	}
}
