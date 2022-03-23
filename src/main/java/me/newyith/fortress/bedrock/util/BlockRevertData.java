package me.newyith.fortress.bedrock.util;

import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.block.data.BlockData;

public class BlockRevertData {
	private static class Model {
		private final Material material;
		private String dataStr;

		@JsonCreator
		public Model(@JsonProperty("material") Material material,
					 @JsonProperty("dataStr") String dataStr) {
			this.material = material;
			this.dataStr = dataStr;

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
		BlockData data = b.getBlockData().clone();
		model = new Model(material, data.getAsString());
		if (material == Material.BEDROCK) {
			Debug.warn("Saved BEDROCK as revertData material at " + p);
//			throw new RuntimeException("Saved BEDROCK as revert material.");
		}
	}

	//-----------------------------------------------------------------------

	public void revert(World world, Point p) {
		Block b = p.getBlock(world);
		b.setType(model.material);
		BlockState state = b.getState();
		state.setBlockData(Bukkit.createBlockData(model.dataStr));
		state.update();
	}

	public Material getMaterial() {
		return model.material;
	}
}
