package me.newyith.fortress.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.UUID;

public class Batch extends BaseUUID {
	protected static class Model extends BaseUUID.Model {
		private final transient BatchData batchData;

		@JsonCreator
		public Model(@JsonProperty("uuid") UUID uuid) {
			super(uuid);

			//rebuild transient fields
			this.batchData = BatchDataStore.get(uuid);
		}
	}
	private Model model = null;

	@JsonCreator
	public Batch(@JsonProperty("model") Model model) {
		super(model);
		this.model = model;
	}

	public Batch(AuthToken authToken, Set<Point> points) {
		BatchDataStore.put(super.getUuid(), new BatchData(authToken, points));
		model = new Model(super.getUuid());
	}

	//-----------------------------------------------------------------------

	public boolean authorizedBy(AuthToken otherAuthToken) {
		return model.batchData.getAuthToken().equals(otherAuthToken);
	}

	public Set<Point> getPoints() {
		return model.batchData.getPoints();
	}

	public boolean contains(Point p) {
		return model.batchData.getPoints().contains(p);
	}
}
