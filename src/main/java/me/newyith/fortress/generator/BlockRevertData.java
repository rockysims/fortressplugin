package me.newyith.fortress.generator;

import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class BlockRevertData {
	private static class Model {
		private final Point point;
		private final Material material;
		private final String worldName;
		private final transient World world;

		@JsonCreator
		public Model(@JsonProperty("point") Point point,
					 @JsonProperty("material") Material material,
					 @JsonProperty("worldName") String worldName) {
			this.point = point;
			this.material = material;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public BlockRevertData(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BlockRevertData(World world, Point p) {
		Material material = p.getBlock(world).getType();
		String worldName = world.getName();
		model = new Model(p, material, worldName);
	}

	//-----------------------------------------------------------------------

	public void revert() {
		Block b = model.point.getBlock(model.world);
		b.setType(model.material);
	}
}
