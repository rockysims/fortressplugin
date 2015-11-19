package me.newyith.fortress.generator.core;

import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.Wall;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
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

	protected Set<Point> getFallbackWhitelistSignPoints() {
		Set<Point> fallbackSigns = new HashSet<>();

		GeneratorRune rune = FortressesManager.getRune(model.anchorPoint);
		if (rune != null) {
			Set<Point> potentialSigns = getLayerAround(rune.getPattern().getPoints(), Wall.ConnectedThreshold.FACES).join();

			//fill fallbackSigns (from potentialSigns)
			for (Point potentialSign : potentialSigns) {
				Material mat = potentialSign.getBlock(model.world).getType();
				if (Wall.isSign(mat)) {
					fallbackSigns.add(potentialSign);
				}
			}

			if (!fallbackSigns.isEmpty()) {
				//fallbackSigns.addAll(connected signs)
				Point origin = model.anchorPoint;
				Set<Point> originLayer = fallbackSigns;
				Set<Material> traverseMaterials = Wall.getSignMaterials();
				Set<Material> returnMaterials = Wall.getSignMaterials();
				int rangeLimit = model.generationRangeLimit * 2;
				Set<Point> ignorePoints = null;
				Set<Point> searchablePoints = null;
				Set<Point> connectedSigns = Wall.getPointsConnected(model.world, origin, originLayer,
						traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints).join();
				fallbackSigns.addAll(connectedSigns);
			}
		}

		return fallbackSigns;
	}

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
