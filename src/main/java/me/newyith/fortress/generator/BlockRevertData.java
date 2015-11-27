package me.newyith.fortress.generator;

import me.newyith.fortress.util.Point;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class BlockRevertData {
	private static class Model {
		private final Material material;

		@JsonCreator
		public Model(@JsonProperty("material") Material material) {
			this.material = material;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BlockRevertData(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BlockRevertData(World world, Point p) {
		Material material = p.getBlock(world).getType();
		model = new Model(material);
	}

	//-----------------------------------------------------------------------

	public void revert(World world, Point p) {
		Block b = p.getBlock(world);
		b.setType(model.material);
	}

	public Material getMaterial() {
		return model.material;
	}
}
