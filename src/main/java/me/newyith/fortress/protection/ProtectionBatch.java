package me.newyith.fortress.protection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.bedrock.BedrockBatch;
import me.newyith.fortress.util.Batch;
import me.newyith.fortress.util.Point;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProtectionBatch extends Batch {
	protected static class Model extends Batch.Model {
		private final Set<BedrockBatch> bedrockBatches;

		@JsonCreator
		public Model(@JsonProperty("uuid") UUID uuid,
					 @JsonProperty("bedrockBatches") Set<BedrockBatch> bedrockBatches) {
			super(uuid);
			this.bedrockBatches = bedrockBatches;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public ProtectionBatch(@JsonProperty("model") Model model) {
		super(model);
		this.model = model;
	}

	public ProtectionBatch(ProtectionAuthToken authToken, Set<Point> points) {
		super(authToken, points);
		model = new Model(super.getUuid(), new HashSet<>());
	}

	//------------------------------------------------------------------------------------------------------------------

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