package me.newyith.fortress.bedrock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import me.newyith.fortress.util.Batch;
import me.newyith.fortress.util.Point;

import java.util.Set;
import java.util.UUID;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class BedrockBatch extends Batch {
	protected static class Model extends Batch.Model {
		@JsonCreator
		public Model(@JsonProperty("uuid") UUID uuid) {
			super(uuid);

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
		model = new Model(super.getUuid());
	}

	//------------------------------------------------------------------------------------------------------------------

}
