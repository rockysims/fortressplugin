package me.newyith.fortressOrig.bedrock.timed;

import me.newyith.fortressOrig.bedrock.BedrockAuthToken;
import me.newyith.fortressOrig.bedrock.BedrockBatch;
import me.newyith.fortressOrig.util.Point;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.UUID;

public class TimedBedrockBatch extends BedrockBatch implements Comparable<TimedBedrockBatch> {
	private static class Model extends BedrockBatch.Model {
		private int endTick = 0;

		@JsonCreator
		public Model(@JsonProperty("uuid") UUID uuid,
					 @JsonProperty("endTick") int endTick) {
			super(uuid);
			this.endTick = endTick;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public TimedBedrockBatch(@JsonProperty("model") Model model) {
		super(model);
		this.model = model;
	}

	public TimedBedrockBatch(BedrockAuthToken authToken, Set<Point> points, int endTick) {
		super(authToken, points);
		model = new Model(super.getUuid(), endTick);
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
