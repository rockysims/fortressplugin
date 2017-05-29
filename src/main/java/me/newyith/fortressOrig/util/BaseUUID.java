package me.newyith.fortressOrig.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public abstract class BaseUUID {
	protected static class Model {
		private final UUID uuid;

		@JsonCreator
		public Model(@JsonProperty("uuid") UUID uuid) {
			this.uuid = uuid;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BaseUUID(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BaseUUID() {
		model = new Model(UUID.randomUUID());
	}

	//-----------------------------------------------------------------------

	public UUID getUuid() {
		return model.uuid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || !(o instanceof BaseUUID)) return false;

		BaseUUID that = (BaseUUID) o;

		return !(model.uuid != null ? !model.uuid.equals(that.getUuid()) : that.getUuid() != null);
	}

	@Override
	public int hashCode() {
		return model.uuid != null ? model.uuid.hashCode() : 0;
	}
}
