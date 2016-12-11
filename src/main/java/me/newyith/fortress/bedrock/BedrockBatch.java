package me.newyith.fortress.bedrock;

import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.util.Point;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Set;

public class BedrockBatch {
	protected static class Model {
		private final BedrockAuthToken authToken;
		private final ImmutableSet<Point> points;

		@JsonCreator
		public Model(@JsonProperty("authToken") BedrockAuthToken authToken,
					 @JsonProperty("points") Set<Point> points) {
			this.authToken = authToken;
			this.points = ImmutableSet.copyOf(points);

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockBatch(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public BedrockBatch(BedrockAuthToken authToken, Set<Point> points) {
		model = new Model(authToken, ImmutableSet.copyOf(points));
	}

	//-----------------------------------------------------------------------

	public Set<Point> getPoints() {
		return model.points;
	}

	public boolean contains(Point p) {
		return model.points.contains(p);
	}

	public boolean authorizedBy(BedrockAuthToken otherAuthToken) {
		return model.authToken.equals(otherAuthToken);
	}
}
