package me.newyith.fortress.protection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.newyith.fortress.bedrock.BedrockBatch;
import me.newyith.fortress.util.Batch;
import me.newyith.fortress.util.Point;

import java.util.HashSet;
import java.util.Set;

public class ProtectionBatch extends Batch {
	protected static class Model {
		private Batch.Model superModel = null;
		private final Set<BedrockBatch> bedrockBatches;

		@JsonCreator
		public Model(@JsonProperty("superModel") Batch.Model superModel,
					 @JsonProperty("bedrockBatches") Set<BedrockBatch> bedrockBatches) {
			this.superModel = superModel;
			this.bedrockBatches = bedrockBatches;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public ProtectionBatch(@JsonProperty("model") Model model) {
		super(model.superModel);
		this.model = model;
	}

	public ProtectionBatch(ProtectionAuthToken authToken, Set<Point> points) {
		super(authToken, points);
		model = new Model(super.model, new HashSet<>());
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
