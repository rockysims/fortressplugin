package me.newyith.fortress.core;

import me.newyith.fortress.util.Blocks;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class CoreMaterials {
	private static class Model {
		private final Point chestPoint;
		private final String worldName;
		private final transient World world;
		private final transient Set<Material> generatableWallMaterials;
		private final transient Set<Material> protectableWallMaterials;
		private final transient Set<Material> alterableWallMaterials;
		private final transient Set<String> flags;

		@JsonCreator
		public Model(@JsonProperty("chestPoint") Point chestPoint,
					 @JsonProperty("worldName") String worldName) {
			this.chestPoint = chestPoint;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
			this.generatableWallMaterials = new HashSet<>();
			this.protectableWallMaterials = new HashSet<>();
			this.alterableWallMaterials = new HashSet<>();
			this.flags = new HashSet<>();
		}
	}
	private Model model = null;

	@JsonCreator
	public CoreMaterials(@JsonProperty("model") Model model) {
		this.model = model;
		refresh();
	}

	public CoreMaterials(World world, Point chestPoint) {
		model = new Model(chestPoint, world.getName());
		refresh();
	}

	//-----------------------------------------------------------------------

	// - Getters -

	public Set<Material> getGeneratableWallMaterials() {
		return model.generatableWallMaterials;
	}

	public boolean isProtectable(Block b) {
		return model.protectableWallMaterials.contains(b.getType());
	}

	public boolean isAlterable(Block b) {
		return model.alterableWallMaterials.contains(b.getType());
	}

	public boolean isProtectable(Material m) {
		if (m != null) {
			return model.protectableWallMaterials.contains(m);
		}
		return false;
	}

	public boolean isAlterable(Material m) {
		if (m != null) {
			return model.alterableWallMaterials.contains(m);
		}
		return false;
	}

	public Set<Material> getInvalidWallMaterials() {
		return refresh();
	}

	public boolean getFastAnimationFlag() {
		refresh(); //needed in case items in chest have changed (such as when items are added by hopper)
		return model.flags.contains("fastAnimation");
	}

	// - Refreshing -

	public Set<Material> refresh() {
		//Debug.msg("CoreMaterials refresh()ed. chest: " + chestPoint);
		resetToDefaultBlockTypesAndFlags();

		Set<Material> invalidWallMaterials = new HashSet<>();

		ItemStack[] items = getChestContents();
		for (ItemStack item : items) {
			if (item != null) {
				Material mat = item.getType();
				switch (mat) {
					//non protectable
					//* //TODO: leave this block enabled (except when debugging)
					case GRASS:
					case DIRT:
					case DIRT_PATH:
					case STONE:
					case GRAVEL:
					case SAND:
					case NETHERRACK:
					case END_STONE:
					case CHEST: //chest items ejected by bedrock wave
					case TRAPPED_CHEST: //chest items ejected by bedrock wave
					//pistons not protectable because bedrock ripple and shield don't seem to play nice with pistons
					//had piston turn into piston extension (invisible) when toggling switch controlling piston with empty hand (causing ripple)
					case PISTON:
					case STICKY_PISTON:

					//*/
					case BEDROCK:
						invalidWallMaterials.add(mat);
						break;
					//special protectable
//					case WATER_BUCKET:
//						addProtectable(Material.STATIONARY_WATER);
//						break;
//					case LAVA_BUCKET:
//						addProtectable(Material.STATIONARY_LAVA);
//						break;
//					case PISTON_BASE:
//						addProtectable(Material.PISTON_BASE);
//						addProtectable(Material.PISTON_EXTENSION);
//						break;
//					case PISTON_STICKY_BASE:
//						addProtectable(Material.PISTON_STICKY_BASE);
//						addProtectable(Material.PISTON_MOVING_PIECE);
//						break;
					case REDSTONE:
						addProtectable(Material.REDSTONE_WIRE);
						break;
					case TORCH:
						addProtectable(Material.TORCH);
						addProtectable(Material.WALL_TORCH);
						break;
					case REDSTONE_TORCH:
						addProtectable(Material.REDSTONE_TORCH);
						addProtectable(Material.REDSTONE_WALL_TORCH);
						break;
					//flags
					case BLAZE_ROD:
						model.flags.add("fastAnimation");
						break;
					default:
						boolean isProtectable = mat.isBlock()
							&& !Blocks.isSign(mat); //text removed by bedrock wave
						if (isProtectable) {
							addProtectable(item.getType());
						} else if (mat != Material.GLOWSTONE_DUST) {
							invalidWallMaterials.add(mat);
						}
				}
			}
		}

		return invalidWallMaterials;
	}
	private ItemStack[] getChestContents() {
		Block chestBlock = model.chestPoint.getBlock(model.world);
		if (chestBlock.getState() instanceof Chest) {
			Chest chest = (Chest)chestBlock.getState();
			Inventory inv = chest.getInventory();
			ItemStack[] items = inv.getContents();

			return items;
		}

		Debug.error("wallMats.getChestContents() failed. returning empty list");
		return new ItemStack[0];
	}
	private void addProtectable(Material mat) {
		model.protectableWallMaterials.add(mat);
		model.generatableWallMaterials.add(mat);
	}

	private void resetToDefaultBlockTypesAndFlags() {
		//clear all
		model.protectableWallMaterials.clear();
		model.alterableWallMaterials.clear();
		model.generatableWallMaterials.clear();
		model.flags.clear();

		//fill alterableWallMaterials
		model.alterableWallMaterials.add(Material.COBBLESTONE);

		//fill generatableWallMaterials
		for (Material m : model.protectableWallMaterials)
			model.generatableWallMaterials.add(m);
		for (Material m : model.alterableWallMaterials)
			model.generatableWallMaterials.add(m);
	}
}
