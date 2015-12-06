package me.newyith.fortress.generator;

import org.bukkit.material.MaterialData;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class MaterialDataWrapper {
	private static class Model {
		private final int type;
		private byte data;
		private transient MaterialData materialData;

		@JsonCreator
		public Model(@JsonProperty("type") int type,
					 @JsonProperty("data") byte data) {
			this.type = type;
			this.data = data;

			//rebuild transient fields
			this.materialData = new MaterialData(type, data);
		}
	}
	private Model model = null;

	@JsonCreator
	public MaterialDataWrapper(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public MaterialDataWrapper(MaterialData materialData) {
		int type = materialData.getItemTypeId();
		byte data = materialData.getData();
		model = new Model(type, data);
	}

	//-----------------------------------------------------------------------

	public MaterialData unwrap() {
		return model.materialData;
	}
}
