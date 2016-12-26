package me.newyith.fortress.bedrock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.*;

public class BedrockBatchesStoreForWorld {
	private static class Model {
		private final Map<UUID, BedrockBatch> batchesByUuid;
		private final String worldName;
		private final transient World world;

		@JsonCreator
		public Model(@JsonProperty("batchesByUuid") Map<UUID, BedrockBatch> batchesByUuid,
					 @JsonProperty("worldName") String worldName) {
			this.batchesByUuid = batchesByUuid;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockBatchesStoreForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockBatchesStoreForWorld(World world) {
		model = new Model(new HashMap<>(), world.getName());
	}

	//-----------------------------------------------------------------------

	public void put(BedrockBatch batch) {
		model.batchesByUuid.put(batch.getUuid(), batch);
	}

	public BedrockBatch get(UUID uuid) {
		return model.batchesByUuid.get(uuid);
	}
}
