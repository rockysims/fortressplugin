package me.newyith.fortress.disruption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.newyith.fortress.util.Batch;
import me.newyith.fortress.util.Point;

import java.util.Set;

public class DisruptionBatch extends Batch {
	protected static class Model {
		private Batch.Model superModel = null;

		@JsonCreator
		public Model(@JsonProperty("superModel") Batch.Model superModel) {
			this.superModel = superModel;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public DisruptionBatch(@JsonProperty("model") Model model) {
		super(model.superModel);
		this.model = model;
	}

	public DisruptionBatch(DisruptionAuthToken authToken, Set<Point> points) {
		super(authToken, points);
		model = new Model(super.model);
	}
}
