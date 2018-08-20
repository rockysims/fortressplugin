package me.newyith.fortress.bedrock.util;

import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BlockRevertData {
	private static class Model {
		private final Material material;
		private String data;

		@JsonCreator
		public Model(@JsonProperty("material") Material material,
					 @JsonProperty("data") String data) {
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
		String data = b.getBlockData().getAsString();
		model = new Model(material, data);
		if (material == Material.BEDROCK) {
			Debug.warn("Saved BEDROCK as revertData material at " + p);
//			throw new RuntimeException("Saved BEDROCK as revert material.");
		}
	}

	//-----------------------------------------------------------------------

	public void revert(World world, Point p) {
		Block b = p.getBlock(world);
		b.setType(model.material);
		BlockData blockData = Bukkit.getServer().createBlockData(model.data);
		b.setBlockData(blockData);
	}

	public Material getMaterial() {
		return model.material;
	}
}
