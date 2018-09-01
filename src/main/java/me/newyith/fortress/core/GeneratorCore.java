package me.newyith.fortress.core;

import javafx.util.Pair;
import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.bedrock.timed.TimedBedrockManager;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.rune.generator.GeneratorRune;
import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Point;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GeneratorCore extends BaseCore {
	private static class Model {
		private BaseCore.Model superModel = null;
		private String datum = null; //placeholder since GeneratorCore doesn't need its own data (at least not yet)
		private final transient Random random = new Random(); //showRipple() needs model.random to be final

		@JsonCreator
		public Model(@JsonProperty("superModel") BaseCore.Model superModel,
					 @JsonProperty("datum") String datum) {
			this.superModel = superModel;
			this.datum = datum;

			//rebuild transient fields
			//this.random = new Random(); //this.random is final so can't init here
		}
	}
	private Model model = null;

	@JsonCreator
	public GeneratorCore(@JsonProperty("model") Model model) {
		super(model.superModel);
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

		GeneratorRune rune = FortressesManager.forWorld(super.model.world).getRuneByPatternPoint(super.model.anchorPoint);
		if (rune != null) {
			Set<Point> potentialSigns = getLayerAround(rune.getPattern().getPoints(), Blocks.ConnectedThreshold.FACES).join();

			//fill fallbackSigns (from potentialSigns)
			for (Point potentialSign : potentialSigns) {
				Material mat = potentialSign.getBlock(super.model.world).getType();
				if (Blocks.isSign(mat)) {
					fallbackSigns.add(potentialSign);
				}
			}

			if (!fallbackSigns.isEmpty()) {
				//fallbackSigns.addAll(connected signs)
				Point origin = super.model.anchorPoint;
				Set<Point> originLayer = fallbackSigns;
				Set<Material> traverseMaterials = Blocks.getSignMaterials();
				Set<Material> returnMaterials = Blocks.getSignMaterials();
				int rangeLimit = super.model.generationRangeLimit * 2;
				Set<Point> ignorePoints = null;
				Set<Point> searchablePoints = null;
				Set<Point> connectedSigns = Blocks.getPointsConnected(super.model.world, origin, originLayer,
						traverseMaterials, returnMaterials, rangeLimit, ignorePoints, searchablePoints).join();
				fallbackSigns.addAll(connectedSigns);
			}
		}

		return fallbackSigns;
	}

	public Set<Material> getInvalidWallMaterials() {
		return super.model.animator.getInvalidWallMaterials();
	}

	public void onPlayerRightClickWall(Player player, Block block, BlockFace face) {
		Material materialInHand = player.getInventory().getItemInMainHand().getType();
		if (materialInHand == Material.AIR) {
			Point origin = new Point(block);
			Point towardFace = origin.add(face.getModX(), face.getModY(), face.getModZ());

			//particle = heart/flame/smoke (inside/outside/disabled)
			Set<Point> generatedPoints = getGeneratedPoints();
			boolean originGenerated = generatedPoints.contains(origin);
			Particle particle = Particle.SMOKE_NORMAL;
			if (originGenerated) {
				boolean inside = getPointsInsideFortress().contains(towardFace);
				if (inside) particle = Particle.HEART;
				else particle = Particle.FLAME;
			}

			//display particle
			Pair<Point, Point> wallOutside = new Pair<>(origin, towardFace);
			super.model.coreParticles.showParticleForWallOutsidePair(super.model.world, wallOutside, particle, 3);

			//show bedrock ripple (unless opening/closing door)
			if (originGenerated) {
				//skip bedrock ripple if opening protected door (hacky solution)
				boolean openingOrClosingDoor = false;
				if (Blocks.isDoor(block.getType())) {
					Block aboveBlock = block.getRelative(0, 1, 0);
					Block topDoorBlock = (Blocks.isTallDoor(block.getType()) && Blocks.isTallDoor(aboveBlock.getType()))
							? aboveBlock
							: block;
					if (playerCanOpenDoor(player, new Point(topDoorBlock))) {
						openingOrClosingDoor = true;
					}
				}

				if (!openingOrClosingDoor) showRipple(origin, generatedPoints);
			}
		}
	}

	private void showRipple(Point origin, Set<Point> generatedPoints) {
		//get rippleLayers
		int layerLimit = 20;
		Set<Point> searchablePoints = generatedPoints;
		CompletableFuture<List<Set<Point>>> future = Blocks.getPointsConnectedAsLayers(super.model.world, origin, layerLimit - 1, searchablePoints);
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

				//make end of wave recede
				int layersRemaining = layerLimit - layerIndex; //layersRemaining == 1 on last layer
				if (layersRemaining <= 4) {
					int msPerLayer = 150;
					msDuration = msDuration - (2 * msPerLayer) * (4 - layersRemaining);
					msDuration += msPerLayer;
				}

				final int msDurationFinal = msDuration;
				Bukkit.getScheduler().scheduleSyncDelayedTask(FortressPlugin.getInstance(), () -> {
					//TODO: consider fixing hacky call to getRuneByPatternPoint() here. GeneratorCore shouldn't need to know about rune
					//	maybe add BaseCore::onBroken() should set super.model.isBroken = true?

					boolean runeStillExists = FortressesManager.forWorld(super.model.world).getRuneByPatternPoint(super.model.anchorPoint) != null;
					if (runeStillExists) { //rune might have been destroyed before ripple ended
						TimedBedrockManager.forWorld(super.model.world).convert(super.model.bedrockAuthToken, layer, msDurationFinal);

						//force reversion of cobble in layer
						Set<Point> cobbleInLayer = layer.stream().filter(p ->
								BedrockManager.forWorld(super.model.world).getMaterialOrNull(p) == Material.COBBLESTONE
						).collect(Collectors.toSet());
						TimedBedrockManager.forWorld(super.model.world).forceReversion(super.model.bedrockAuthToken, cobbleInLayer, 300);
					}
				}, layerIndex * 3); //ticks (50 ms per tick)

				layerIndex++;
			}
		}
	}


	/* Yona's version
	protected Set<Point> getFallbackWhitelistSignPoints() {
		GeneratorRune rune = FortressesManager.getRuneByPatternPoint(super.model.anchorPoint);
		if (rune != null) {
			Set<Point> potentialSigns = getLayerAround(rune.getPattern().getPoints(), Blocks.ConnectedThreshold.FACES).join();

			final Set<Point> fallbackSigns = potentialSigns.stream()
					.filter(sign -> Blocks.isSign(sign.getBlock(super.model.world).getType()))
					.collect(Collectors.toSet());

			//fill fallbackSigns (from potentialSigns)
			for (Point potentialSign : potentialSigns) {
				Material mat = potentialSign.getBlock(super.model.world).getType();
				if (Blocks.isSign(mat)) {
					fallbackSigns.add(potentialSign);
				}
			}

			if (!fallbackSigns.isEmpty()) {
				//fallbackSigns.addAll(connected signs)
				Point origin = super.model.anchorPoint;
				Set<Point> originLayer = fallbackSigns;
				Set<Material> traverseMaterials = Blocks.getSignMaterials();
				Set<Material> returnMaterials = Blocks.getSignMaterials();
				int rangeLimit = super.model.generationRangeLimit * 2;
				Set<Point> ignorePoints = null;
				Set<Point> searchablePoints = null;
				Set<Point> connectedSigns = Blocks.getPointsConnected(super.model.world, origin, originLayer,
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
		GeneratorRune rune = FortressesManager.forWorld(super.model.world).getRuneByPatternPoint(super.model.anchorPoint);
		if (rune != null) {
			rune.onSearchingChanged(searching);
		}
	}

	@Override
	protected Set<Point> getOriginPoints() {
		GeneratorRune rune = FortressesManager.forWorld(super.model.world).getRuneByPatternPoint(super.model.anchorPoint);
		return rune.getPattern().getPoints();
	}
}
