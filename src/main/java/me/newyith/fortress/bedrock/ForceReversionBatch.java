package me.newyith.fortress.bedrock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import me.newyith.fortress.util.Batch;
import me.newyith.fortress.util.Point;

import java.util.Set;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ForceReversionBatch extends Batch {
	protected static class Model {
		private Batch.Model superModel = null;

		@JsonCreator
		public Model(@JsonProperty("superModel") Batch.Model superModel) {
			this.superModel = superModel;

			//rebuild transient fields
		}
	}
	protected Model model = null;

	@JsonCreator
	public ForceReversionBatch(@JsonProperty("model") Model model) {
		super(model.superModel);
		this.model = model;
	}

	public ForceReversionBatch(BedrockAuthToken authToken, Set<Point> points) {
		super(authToken, points);
		model = new Model(super.model);
	}

	//------------------------------------------------------------------------------------------------------------------

}