package me.newyith.fortress.core;

import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.BedrockBatch;
import me.newyith.fortress.bedrock.BedrockManager;
import me.newyith.fortress.bedrock.timed.TimedBedrockManager;
import me.newyith.fortress.core.util.WallLayer;
import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.protection.ProtectionAuthToken;
import me.newyith.fortress.protection.ProtectionBatch;
import me.newyith.fortress.protection.ProtectionManager;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class CoreAnimator {
	private static class Model {
		private final Set<List<ProtectionBatch>> allOldProtectionBatches;
		private List<ProtectionBatch> curProtectionBatches;
		private Point anchorPoint = null;
		private List<WallLayer> wallLayers = null;
		private CoreMaterials coreMats = null;
		private BedrockAuthToken bedrockAuthToken;
		private ProtectionAuthToken protectionAuthToken;
		private boolean skipAnimation = false;
		private boolean fastAnimation = false;
		private boolean animationInProgress = false;
		private int curIndex = 0;
		private String worldName = null;
		private transient World world = null;
		private final transient int ticksPerFrame;
		private transient int animationWaitTicks = 0;

		@JsonCreator
		public Model(@JsonProperty("allOldProtectionBatches") Set<List<ProtectionBatch>> allOldProtectionBatches,
					 @JsonProperty("curProtectionBatches") List<ProtectionBatch> curProtectionBatches,
					 @JsonProperty("anchorPoint") Point anchorPoint,
					 @JsonProperty("wallLayers") List<WallLayer> wallLayers,
					 @JsonProperty("coreMats") CoreMaterials coreMats,
					 @JsonProperty("bedrockAuthToken") BedrockAuthToken bedrockAuthToken,
					 @JsonProperty("protectionAuthToken") ProtectionAuthToken protectionAuthToken,
					 @JsonProperty("skipAnimation") boolean skipAnimation,
					 @JsonProperty("fastAnimation") boolean fastAnimation,
					 @JsonProperty("animationInProgress") boolean animationInProgress,
					 @JsonProperty("curIndex") int curIndex,
					 @JsonProperty("worldName") String worldName) {
			this.allOldProtectionBatches = allOldProtectionBatches;
			this.curProtectionBatches = curProtectionBatches;
			this.anchorPoint = anchorPoint;
			this.wallLayers = wallLayers;
			this.coreMats = coreMats;
			this.bedrockAuthToken = bedrockAuthToken;
			this.protectionAuthToken = protectionAuthToken;
			this.skipAnimation = skipAnimation;
			this.fastAnimation = fastAnimation;
			this.animationInProgress = animationInProgress;
			this.curIndex = curIndex;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.ticksPerFrame = (150 / TickTimer.msPerTick); // msPerFrame / msPerTick
			this.animationWaitTicks = 0;
		}
	}
	private Model model = null;

	@JsonCreator
	public CoreAnimator(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public CoreAnimator(World world, Point anchorPoint, CoreMaterials coreMats, BedrockAuthToken bedrockAuthToken, ProtectionAuthToken protectionAuthToken) {
		Set<List<ProtectionBatch>> allOldProtectionBatches = new HashSet<>();
		List<ProtectionBatch> curProtectionBatches = new ArrayList<>();
		List<WallLayer> wallLayers = new ArrayList<>();
		boolean skipAnimation = false;
		boolean fastAnimation = false;
		boolean animationInProgress = false;
		int curIndex = 0;
		String worldName = world.getName();
		model = new Model(allOldProtectionBatches, curProtectionBatches, anchorPoint, wallLayers, coreMats,
				bedrockAuthToken, protectionAuthToken, skipAnimation, fastAnimation, animationInProgress, curIndex, worldName);
	}

	//------------------------------------------------------------------------------------------------------------------

	public void generate(List<WallLayer> wallLayers) {
		//add curBatches to allOldBatches so this method is robust enough to get called twice without calling degenerate() in between
		model.allOldProtectionBatches.add(model.curProtectionBatches);
		model.curProtectionBatches = new ArrayList<>();

		model.wallLayers = wallLayers;
		model.curIndex = 0;
		model.animationInProgress = true;

		model.fastAnimation = model.coreMats.getFastAnimationFlag();
	}

	public void degenerate(boolean skipAnimation) {
		model.allOldProtectionBatches.add(model.curProtectionBatches);
		model.curProtectionBatches = new ArrayList<>();

		model.wallLayers = new ArrayList<>();
		model.curIndex = 0;
		model.animationInProgress = true;

		model.fastAnimation = model.coreMats.getFastAnimationFlag();

		if (skipAnimation) {
			model.skipAnimation = true;
			tick();
			model.skipAnimation = false;
		}
	}

	public Set<Point> getGeneratedPoints() {
		//TODO: consider rewriting this so we don't have to rebuild it every time (or better yet figured out how to remove this method entirely)
		return ProtectionManager.forWorld(model.world).buildProtectedPointsByAuthToken(model.protectionAuthToken);
	}

	public Set<Material> getGeneratableWallMaterials() {
		model.coreMats.refresh(); //refresh protectable blocks list based on chest contents
		return model.coreMats.getGeneratableWallMaterials();
	}

	public Set<Material> getInvalidWallMaterials() {
		return model.coreMats.getInvalidWallMaterials();
	}

	public void tick() {
		if (model.animationInProgress) {
			model.animationWaitTicks++;
			if (model.animationWaitTicks >= model.ticksPerFrame || model.skipAnimation) {
				model.animationWaitTicks = 0;

				int frameUpdatesLimit = (model.fastAnimation)?3:1;
				while (true) {
					//try to update to next frame
					boolean updatedFrame = updateToNextFrame();
					if (!updatedFrame) {
						//no next frame so stop trying to animate
						model.animationInProgress = false;
						break;
					}
					if (updatedFrame && !model.skipAnimation) {
						frameUpdatesLimit--;
						if (frameUpdatesLimit <= 0) {
							//updated to next frame(s) so we're done for now
							break;
						}
					}
				}
			}
		}
	}

	// --------- Internal Methods ---------

	private boolean updateToNextFrame() {
		//try to generate a new layer
		boolean generatedNewLayer = false;
		while (!generatedNewLayer && model.curIndex < model.wallLayers.size()) {
			//generate this layer
			WallLayer layer = model.wallLayers.get(model.curIndex);
			if (layer != null) {
				ProtectionBatch protectionBatch = new ProtectionBatch(model.protectionAuthToken, layer.getPoints());
				Set<Point> newlyProtecteds = ProtectionManager.forWorld(model.world).protect(protectionBatch);
				model.curProtectionBatches.add(protectionBatch);
				alter(protectionBatch, layer.getAlterPoints());

				if (newlyProtecteds.size() > 0) {
					generatedNewLayer = true;

					//show bedrock wave (if animation on)
					if (!model.skipAnimation) {
						int ms = 4 * model.ticksPerFrame * TickTimer.msPerTick;
						TimedBedrockManager.forWorld(model.world).convert(model.bedrockAuthToken, newlyProtecteds, ms);
					}
				}
			} else {
				Debug.warn("updateToNextFrame() failed to find wallLayer with layerIndex " + model.curIndex + " (anchor: " + model.anchorPoint + ")");
			}

			model.curIndex++;
		}

		//try to degenerate old layer(s)
		boolean degeneratedOldLayer = false;
		Iterator<List<ProtectionBatch>> it = model.allOldProtectionBatches.iterator();
		while (it.hasNext()) {
			List<ProtectionBatch> oldProtectionBatches = it.next();
			if (oldProtectionBatches.isEmpty()) {
				it.remove();
				continue;
			}
			//we know oldProtectionBatches is not empty now

			//remove and degenerate last batch in oldProtectionBatches
			ProtectionBatch lastOldProtectionBatch = oldProtectionBatches.remove(oldProtectionBatches.size() - 1);
			Set<Point> newlyUnprotecteds = ProtectionManager.forWorld(model.world).unprotect(lastOldProtectionBatch);
			unalter(lastOldProtectionBatch);

			if (newlyUnprotecteds.size() > 0) {
				degeneratedOldLayer = true;

				//show bedrock wave (if animation on)
				if (!model.skipAnimation) {
					int ms = 4 * model.ticksPerFrame * TickTimer.msPerTick;
					TimedBedrockManager.forWorld(model.world).convert(model.bedrockAuthToken, newlyUnprotecteds, ms);
				}
			}
		}

		boolean updatedToNextFrame = generatedNewLayer || degeneratedOldLayer;

		//tell CoreParticles to update where to display particles
		if (updatedToNextFrame) {
			BaseCore core = FortressesManager.forWorld(model.world).getCore(model.anchorPoint);
			if (core != null) {
				core.onGeneratedChanged();
			} else {
				Debug.error("CoreAnimator.onGeneratedChanged(): Core at " + model.anchorPoint + " is null.");
			}
		}

		return updatedToNextFrame;
	}

	private void alter(ProtectionBatch batch, Set<Point> alterPoints) {
		//convert cobblestone in batch to bedrock
		BedrockBatch bedrockBatch = new BedrockBatch(model.bedrockAuthToken, alterPoints);
		BedrockManager.forWorld(model.world).convert(bedrockBatch);
		batch.addBedrockBatch(bedrockBatch);
	}

	private void unalter(ProtectionBatch protectionBatch) {
		//revert bedrock in protectionBatch to cobblestone
		Set<BedrockBatch> bedrockBatches = protectionBatch.removeBedrockBatches();
		for (BedrockBatch bedrockBatch : bedrockBatches) {
			BedrockManager.forWorld(model.world).revert(bedrockBatch);
		}
	}
}
