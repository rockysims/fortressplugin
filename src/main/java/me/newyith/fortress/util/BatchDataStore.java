package me.newyith.fortress.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BatchDataStore {
	private static BatchDataStore instance = null;
	public static BatchDataStore getInstance() {
		if (instance == null) {
			instance = new BatchDataStore();
		}
		return instance;
	}
	public static void setInstance(BatchDataStore newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private final Map<UUID, BatchData> dataByUuid;

		@JsonCreator
		public Model(@JsonProperty("dataByUuid") Map<UUID, BatchData> dataByUuid) {
			this.dataByUuid = dataByUuid;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BatchDataStore(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BatchDataStore() {
		model = new Model(new HashMap<>());
	}

	//-----------------------------------------------------------------------

	public static void put(UUID uuid, BatchData batchData) {
		getInstance().model.dataByUuid.put(uuid, batchData);
	}

	public static BatchData get(UUID uuid) {
		return getInstance().model.dataByUuid.get(uuid);
	}
}
