package me.newyith.fortress.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public abstract class Batch extends BaseUUID {
	protected static class Model {
		private BaseUUID.Model superModel = null;
		private final transient BatchData batchData;

		@JsonCreator
		public Model(@JsonProperty("superModel") BaseUUID.Model superModel) {
			this.superModel = superModel;

			//rebuild transient fields
			this.batchData = BatchDataStore.get(superModel.uuid);
		}
	}
	protected Model model = null;

	@JsonCreator
	public Batch(@JsonProperty("model") Model model) {
		super(model.superModel);
		this.model = model;
	}

	public Batch(AuthToken authToken, Set<Point> points) {
		super(); //sets super.model
		BatchDataStore.put(super.model.uuid, new BatchData(authToken, points));
		model = new Model(super.model);
	}

	//-----------------------------------------------------------------------

	public boolean authorizedBy(AuthToken otherAuthToken) {
		return model.batchData.getAuthToken().equals(otherAuthToken);
	}

	public AuthToken getAuthToken() {
		return model.batchData.getAuthToken();
	}

	public Set<Point> getPoints() {
		return model.batchData.getPoints();
	}

	public boolean contains(Point p) {
		return model.batchData.getPoints().contains(p);
	}

	public void destroy() {
		BatchDataStore.remove(super.model.uuid);
	}
}
