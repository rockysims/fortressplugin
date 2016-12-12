package me.newyith.fortress.core;

import javafx.util.Pair;
import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.timed.TimedBedrockManagerNew;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.protection.ProtectionAuthToken;
import me.newyith.fortress.rune.generator.GeneratorRune;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Point;
import me.newyith.fortress.util.particle.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GeneratorCore extends BaseCore {
	private static class Model extends BaseCore.Model {
		private String datum = null; //placeholder since GeneratorCore doesn't need its own data (at least not yet)
		private final transient Random random = new Random(); //showRipple() needs model.random to be final

		@JsonCreator
		public Model(@JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("claimedPoints") Set<Point> claimedPoints,
					 @JsonProperty("claimedWallPoints") Set<Point> claimedWallPoints,
					 @JsonProperty("bedrockAuthToken") BedrockAuthToken bedrockAuthToken,
					 @JsonProperty("protectionAuthToken") ProtectionAuthToken protectionAuthToken,
					 @JsonProperty("animator") CoreAnimator animator,
					 @JsonProperty("active") boolean active,
					 @JsonProperty("placedByPlayerId") UUID placedByPlayerId,
					 @JsonProperty("layerOutsideFortress") Set<Point> layerOutsideFortress,
					 @JsonProperty("pointsInsideFortress") Set<Point> pointsInsideFortress,
					 @JsonProperty("worldName") String worldName,
					 @JsonProperty("datum") String datum) {
			super(anchorPoint, claimedPoints, claimedWallPoints, bedrockAuthToken, protectionAuthToken,
					animator, active, placedByPlayerId, layerOutsideFortress, pointsInsideFortress, worldName);
			this.datum = datum;

			//rebuild transient fields
			//this.random = new Random(); //this.random is final so can't init here
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

		GeneratorRune rune = FortressesManager.forWorld(model.world).getRuneByPatternPoint(model.anchorPoint);
		if (rune != null) {
			Set<Point> potentialSigns = getLayerAround(rune.getPattern().getPoints(), Blocks.ConnectedThreshold.FACES).join();

			//fill fallbackSigns (from potentialSigns)
			for (Point potentialSign : potentialSigns) {
				Material mat = potentialSign.getBlock(model.world).getType();
				if (Blocks.isSign(mat)) {
					fallbackSigns.add(potentialSign);
				}
			}

			if (!fallbackSigns.isEmpty()) {
				//fallbackSigns.addAll(connected signs)
				Point origin = model.anchorPoint;
				Set<Point> originLayer = fallbackSigns;
				Set<Material> traverseMaterials = Blocks.getSignMaterials();
				Set<Material> returnMaterials = Blocks.getSignMaterials();
				int rangeLimit = model.generationRangeLimit * 2;
				Set<Point> ignorePoints = null;
				Set<Point> searchablePoints = null;
				Set<Point> connectedSigns = Blocks.getPointsConnected(model.world, origin, originLayer,
						traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints).join();
				fallbackSigns.addAll(connectedSigns);
			}
		}

		return fallbackSigns;
	}

	public Set<Material> getInvalidWallMaterials() {
		return model.animator.getInvalidWallMaterials();
	}

	public void onPlayerRightClickWall(Player player, Block block, BlockFace face) {
		Material materialInHand = player.getItemInHand().getType();
		if (materialInHand == Material.AIR) {
			Point origin = new Point(block);
			Point towardFace = origin.add(face.getModX(), face.getModY(), face.getModZ());

			//particleEffect = heart/flame/smoke (inside/outside/disabled)
			Set<Point> generatedPoints = getGeneratedPoints();
			boolean originGenerated = generatedPoints.contains(origin);
			ParticleEffect particleEffect = ParticleEffect.SMOKE_NORMAL;
			if (originGenerated) {
				boolean inside = getPointsInsideFortress().contains(towardFace);
				if (inside) particleEffect = ParticleEffect.HEART;
				else particleEffect = ParticleEffect.FLAME;
			}

			//display particleEffect
			Pair<Point, Point> wallOutside = new Pair<>(origin, towardFace);
			model.coreParticles.showParticleForWallOutsidePair(model.world, wallOutside, particleEffect, 3);

			//show bedrock ripple
			if (originGenerated) {
				showRipple(origin, generatedPoints);
			}
		}
	}

	private void showRipple(Point origin, Set<Point> generatedPoints) {
		//get rippleLayers
		int layerLimit = 20;
		Set<Point> searchablePoints = generatedPoints;
		CompletableFuture<List<Set<Point>>> future = Blocks.getPointsConnectedAsLayers(model.world, origin, layerLimit - 1, searchablePoints);
		future.join(); //wait for future to resolve
		List<Set<Point>> rippleLayersFromFuture = future.getNow(null);

		if (rippleLayersFromFuture != null) {
			Set<Point> originLayer = new HashSet<>();
			originLayer.add(origin);
			List<Set<Point>> rippleLayers = new ArrayList<>();
			rippleLayers.add(originLayer);
			rippleLayers.addAll(rippleLayersFromFuture);

			//remove some blocks from last 4 rippleLayers to create a fizzle out effect
			for (int i = 0; i < 4; i++) {
				int index = (layerLimit-1) - i;
				if (index < rippleLayers.size()) {
					Set<Point> rippleLayer = rippleLayers.get(index);
					if (rippleLayer != null) {
						Iterator<Point> it = rippleLayer.iterator();
						while (it.hasNext()) {
							it.next();
							int percentSkipChance = 0;
							if (i == 0) percentSkipChance = 50;
							else if (i == 1) percentSkipChance = 40;
							else if (i == 2) percentSkipChance = 30;
							else if (i == 3) percentSkipChance = 20;
							if (model.random.nextInt(99) < percentSkipChance) {
								it.remove();
							}
						}
					}
				}
			}

			int layerIndex = 0;
			for (Set<Point> layer : rippleLayers) {
				int msDuration = 2000;

				int layersRemaining = rippleLayers.size() - layerIndex;
				if (layersRemaining < 4) {
					msDuration = 450 * layersRemaining;
				}

				final int msDurationFinal = msDuration;
				Bukkit.getScheduler().scheduleSyncDelayedTask(FortressPlugin.getInstance(), () -> {
					//TODO: consider fixing hacky call to getRuneByPatternPoint() here. GeneratorCore shouldn't need to know about rune
					//	maybe add BaseCore::onBroken() should set model.isBroken = true?
					boolean runeStillExists = FortressesManager.forWorld(model.world).getRuneByPatternPoint(model.anchorPoint) != null;
					if (runeStillExists) { //rune might have been destroyed before ripple ended
						TimedBedrockManagerNew.forWorld(model.world).convert(model.bedrockAuthToken, layer, msDurationFinal);
					}
				}, layerIndex * 3); //20 ticks per second

				layerIndex++;
			}
		}
	}


	/* Yona's version
	protected Set<Point> getFallbackWhitelistSignPoints() {
		GeneratorRune rune = FortressesManager.getRuneByPatternPoint(model.anchorPoint);
		if (rune != null) {
			Set<Point> potentialSigns = getLayerAround(rune.getPattern().getPoints(), Blocks.ConnectedThreshold.FACES).join();

			final Set<Point> fallbackSigns = potentialSigns.stream()
					.filter(sign -> Blocks.isSign(sign.getBlock(model.world).getType()))
					.collect(Collectors.toSet());

			//fill fallbackSigns (from potentialSigns)
			for (Point potentialSign : potentialSigns) {
				Material mat = potentialSign.getBlock(model.world).getType();
				if (Blocks.isSign(mat)) {
					fallbackSigns.add(potentialSign);
				}
			}

			if (!fallbackSigns.isEmpty()) {
				//fallbackSigns.addAll(connected signs)
				Point origin = model.anchorPoint;
				Set<Point> originLayer = fallbackSigns;
				Set<Material> traverseMaterials = Blocks.getSignMaterials();
				Set<Material> returnMaterials = Blocks.getSignMaterials();
				int rangeLimit = model.generationRangeLimit * 2;
				Set<Point> ignorePoints = null;
				Set<Point> searchablePoints = null;
				Set<Point> connectedSigns = Blocks.getPointsConnected(model.world, origin, originLayer,
						traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints).join();

				return Sets.union(fallbackSigns, connectedSigns);
			}

			return fallbackSigns;
		} else {
			return Collections.emptySet();
		}
	}
//*/
	@Override
	protected void onSearchingChanged(boolean searching) {
		GeneratorRune rune = FortressesManager.forWorld(model.world).getRuneByPatternPoint(model.anchorPoint);
		if (rune != null) {
			rune.onSearchingChanged(searching);
		}
	}

	@Override
	protected Set<Point> getOriginPoints() {
		GeneratorRune rune = FortressesManager.forWorld(model.world).getRuneByPatternPoint(model.anchorPoint);
		return rune.getPattern().getPoints();
	}
}
