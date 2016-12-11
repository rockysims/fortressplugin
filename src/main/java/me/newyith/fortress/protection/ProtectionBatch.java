package me.newyith.fortress.protection;

import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.bedrock.BedrockBatch;
import me.newyith.fortress.util.Point;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class ProtectionBatch {
	protected static class Model {
		private final ProtectionAuthToken authToken;
		private final ImmutableSet<Point> points;
		private final Set<BedrockBatch> bedrockBatches;

		@JsonCreator
		public Model(@JsonProperty("authToken") ProtectionAuthToken authToken,
					 @JsonProperty("points") ImmutableSet<Point> points,
					 @JsonProperty("bedrockBatches") Set<BedrockBatch> bedrockBatches) {
			this.authToken = authToken;
			this.points = points;
			this.bedrockBatches = bedrockBatches;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public ProtectionBatch(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public ProtectionBatch(ProtectionAuthToken authToken, Set<Point> points) {
		model = new Model(authToken, ImmutableSet.copyOf(points), new HashSet<>());
	}

	//-----------------------------------------------------------------------

	public Set<Point> getPoints() {
		return model.points;
	}

	public boolean contains(Point p) {
		return model.points.contains(p);
	}

	public boolean authorizedBy(ProtectionAuthToken otherAuthToken) {
		return model.authToken.equals(otherAuthToken);
	}

	public void addBedrockBatch(BedrockBatch bedrockBatch) {
		model.bedrockBatches.add(bedrockBatch);
	}

	public Set<BedrockBatch> removeBedrockBatches() {
		Set<BedrockBatch> bedrockBatches = new HashSet<>();

		bedrockBatches.addAll(model.bedrockBatches);
		model.bedrockBatches.clear();

		return bedrockBatches;
	}
}
