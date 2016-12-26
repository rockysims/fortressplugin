package me.newyith.fortress.bedrock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.util.Batch;
import me.newyith.fortress.util.Point;

import java.util.Set;

public class BedrockBatch extends Batch {
	protected static class Model extends Batch.Model {
		@JsonCreator
		public Model(@JsonProperty("authToken") BedrockAuthToken authToken,
					 @JsonProperty("points") Set<Point> points) {
			super(authToken, ImmutableSet.copyOf(points));

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public BedrockBatch(@JsonProperty("model") Model model) {
		super(model);
		this.model = model;
	}

	public BedrockBatch(BedrockAuthToken authToken, Set<Point> points) {
		super(authToken, points);
		model = new Model(authToken, ImmutableSet.copyOf(points));
	}

	//------------------------------------------------------------------------------------------------------------------

}
