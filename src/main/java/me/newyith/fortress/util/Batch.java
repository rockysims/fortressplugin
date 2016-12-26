package me.newyith.fortress.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.UUID;

public class Batch extends BaseUUID {
	protected static class Model extends BaseUUID.Model {
		private final AuthToken authToken;
		private final ImmutableSet<Point> points;
//		private final transient BatchData batchData;

		@JsonCreator
		public Model(@JsonProperty("uuid") UUID uuid,
					 @JsonProperty("authToken") AuthToken authToken,
					 @JsonProperty("points") Set<Point> points) {
			super(uuid);
			this.authToken = authToken;
			this.points = ImmutableSet.copyOf(points);

			//rebuild transient fields
//			this.batchData = BatchDataStore.get(this.uuid);
		}
	}
	private Model model = null;

	@JsonCreator
	public Batch(@JsonProperty("model") Model model) {
		super(model);
		this.model = model;
	}

	public Batch(AuthToken authToken, Set<Point> points) {
		model = new Model(super.getUuid(), authToken, ImmutableSet.copyOf(points));
	}

	//-----------------------------------------------------------------------

	public boolean authorizedBy(AuthToken otherAuthToken) {
		return model.authToken.equals(otherAuthToken);
	}

	public Set<Point> getPoints() {
		return model.points;
	}

	public boolean contains(Point p) {
		return model.points.contains(p);
	}
}
