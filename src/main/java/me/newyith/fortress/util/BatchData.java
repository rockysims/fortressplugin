package me.newyith.fortress.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class BatchData {
	private static class Model {
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
	public BatchData(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BatchData(AuthToken authToken, Set<Point> points) {
		model = new Model(authToken, points);
	}

	//-----------------------------------------------------------------------

	public AuthToken getAuthToken() {
		return model.authToken;
	}

	public ImmutableSet<Point> getPoints() {
		return model.points;
	}
}
