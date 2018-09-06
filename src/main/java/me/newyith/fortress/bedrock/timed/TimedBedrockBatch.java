package me.newyith.fortress.bedrock.timed;

import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.BedrockBatch;
import me.newyith.fortress.util.Point;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class TimedBedrockBatch extends BedrockBatch implements Comparable<TimedBedrockBatch> {
	private static class Model {
		private BedrockBatch.Model superModel = null;
		private int endTick = 0;

		@JsonCreator
		public Model(@JsonProperty("superModel") BedrockBatch.Model superModel,
					 @JsonProperty("endTick") int endTick) {
			this.superModel = superModel;
			this.endTick = endTick;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public TimedBedrockBatch(@JsonProperty("model") Model model) {
		super(model.superModel);
		this.model = model;
	}

	public TimedBedrockBatch(BedrockAuthToken authToken, Set<Point> points, int endTick) {
		super(authToken, points);
		model = new Model(super.model, endTick);
	}

	//------------------------------------------------------------------------------------------------------------------

	@Override
	public int compareTo(TimedBedrockBatch otherTimedBedrock) {
		return model.endTick - otherTimedBedrock.getEndTick();
	}

	public int getEndTick() {
		return model.endTick;
	}
}
