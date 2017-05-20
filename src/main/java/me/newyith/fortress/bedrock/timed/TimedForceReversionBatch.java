package me.newyith.fortress.bedrock.timed;

import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.ForceReversionBatch;
import me.newyith.fortress.util.Point;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.UUID;

public class TimedForceReversionBatch extends ForceReversionBatch implements Comparable<TimedForceReversionBatch> {
	private static class Model extends ForceReversionBatch.Model {
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
	public TimedForceReversionBatch(@JsonProperty("model") Model model) {
		super(model);
		this.model = model;
	}

	public TimedForceReversionBatch(BedrockAuthToken authToken, Set<Point> points, int endTick) {
		super(authToken, points);
		model = new Model(super.getUuid(), endTick);
	}

	//------------------------------------------------------------------------------------------------------------------

	@Override
	public int compareTo(TimedForceReversionBatch otherBatch) {
		return model.endTick - otherBatch.getEndTick();
	}

	public int getEndTick() {
		return model.endTick;
	}
}
