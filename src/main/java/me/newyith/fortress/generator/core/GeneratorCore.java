package me.newyith.fortress.generator.core;

import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Point;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Set;
import java.util.UUID;

public class GeneratorCore extends BaseCore {
	private static class Model extends BaseCore.Model {
		private String datum = null;

		@JsonCreator
		public Model(@JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("claimedPoints") Set<Point> claimedPoints,
					 @JsonProperty("claimedWallPoints") Set<Point> claimedWallPoints,
					 @JsonProperty("animator") CoreAnimator animator,
					 @JsonProperty("placedByPlayerId") UUID placedByPlayerId,
					 @JsonProperty("layerOutsideFortress") Set<Point> layerOutsideFortress,
					 @JsonProperty("pointsInsideFortress") Set<Point> pointsInsideFortress,
					 @JsonProperty("worldName") String worldName,
					 @JsonProperty("datum") String datum) {
			super(anchorPoint, claimedPoints, claimedWallPoints, animator, placedByPlayerId, layerOutsideFortress, pointsInsideFortress, worldName);
			this.datum = datum;

			//rebuild transient fields
		}

		//should this be JsonCreator instead? if not, delete this comment
		public Model(BaseCore.Model model, String datum) {
			super(model);
			this.datum = datum;
		}
	}
	private Model model = null;

	@JsonCreator
	public GeneratorCore(@JsonProperty("model") Model model) {
		super(model);
		this.model = model;
	}

	public GeneratorCore(World world, Point anchorPoint, CoreMaterials coreMats) {
		super(world, anchorPoint, coreMats); //sets super.model
		String datum = "myDatum";
		model = new Model(super.model, datum);
	}

	//-----------------------------------------------------------------------

	@Override
	protected void onSearchingChanged(boolean searching) {
		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
		if (rune != null) {
			rune.onSearchingChanged(searching);
		}
	}

	@Override
	protected Set<Point> getOriginPoints() {
		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
		return rune.getPattern().getPoints();
	}
}
