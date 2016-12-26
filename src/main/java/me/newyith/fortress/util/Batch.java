package me.newyith.fortress.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class Batch extends BaseUUID {
	protected static class Model {
		private final AuthToken authToken;
		private final ImmutableSet<Point> points;

		@JsonCreator
		public Model(@JsonProperty("authToken") AuthToken authToken,
					 @JsonProperty("points") Set<Point> points) {
			this.authToken = authToken;
			this.points = ImmutableSet.copyOf(points);

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public Batch(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public Batch(AuthToken authToken, Set<Point> points) {
		model = new Model(authToken, ImmutableSet.copyOf(points));
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
