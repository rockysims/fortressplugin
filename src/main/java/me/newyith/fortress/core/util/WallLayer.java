package me.newyith.fortress.core.util;

import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.util.Point;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Set;

//WallLayer represents a layer of wall and should be immutable
public class WallLayer {
	protected static class Model {
		private final ImmutableSet<Point> points;
		private final ImmutableSet<Point> alterPoints;

		@JsonCreator
		public Model(@JsonProperty("points") Set<Point> points,
					 @JsonProperty("alterPoints") Set<Point> alterPoints) {
			this.points = ImmutableSet.copyOf(points);
			this.alterPoints = ImmutableSet.copyOf(alterPoints);

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public WallLayer(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public WallLayer(Set<Point> layerPoints, Set<Point> alterPoints) {
		model = new Model(ImmutableSet.copyOf(layerPoints), ImmutableSet.copyOf(alterPoints));
	}

	//-----------------------------------------------------------------------

	public Set<Point> getPoints() {
		return model.points;
	}

	public Set<Point> getAlterPoints() {
		return model.alterPoints;
	}
}
