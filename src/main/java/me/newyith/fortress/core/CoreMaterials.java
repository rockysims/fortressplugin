package me.newyith.fortress.core;

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

	// - Refreshing -

	public Set<Material> refresh() {
		//Debug.msg("CoreMaterials refresh()ed. chest: " + chestPoint);
		resetToBaseBlockTypes();

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
					case STONE:
					case GRAVEL:
					case SAND:
					case NETHERRACK:
					//*/
					case BEDROCK:
						invalidWallMaterials.add(mat);
						break;
					//special protectable
					case IRON_DOOR:
						addProtectable(Material.IRON_DOOR_BLOCK);
						break;
					case WOOD_DOOR:
						addProtectable(Material.WOODEN_DOOR);
						break;
					case ACACIA_DOOR_ITEM:
						addProtectable(Material.ACACIA_DOOR);
						break;
					case BIRCH_DOOR_ITEM:
						addProtectable(Material.BIRCH_DOOR);
						break;
					case DARK_OAK_DOOR_ITEM:
						addProtectable(Material.DARK_OAK_DOOR);
						break;
					case JUNGLE_DOOR_ITEM:
						addProtectable(Material.JUNGLE_DOOR);
						break;
					case SPRUCE_DOOR_ITEM:
						addProtectable(Material.SPRUCE_DOOR);
						break;
					case WATER_BUCKET:
						addProtectable(Material.STATIONARY_WATER);
						break;
					case LAVA_BUCKET:
						addProtectable(Material.STATIONARY_LAVA);
						break;
					case STEP:
						addProtectable(Material.STEP);
						addProtectable(Material.DOUBLE_STEP);
						break;
					//pistons not protectable because bedrock ripple and shield don't seem to play nice with pistons
					//had piston turn into piston extension (invisible) when toggling switch controlling piston with empty hand (causing ripple)
					/*
					case PISTON_BASE:
//						addProtectable(Material.PISTON_BASE);
//						addProtectable(Material.PISTON_EXTENSION);
//						break;
//					case PISTON_STICKY_BASE:
//						addProtectable(Material.PISTON_STICKY_BASE);
//						addProtectable(Material.PISTON_MOVING_PIECE);
//						break;
					/*/
					case PISTON_BASE:
					case PISTON_STICKY_BASE:
						break;
					//*/
					case REDSTONE:
						addProtectable(Material.REDSTONE_WIRE);
						break;
					default:
						if (mat.isBlock()) {
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

	private void resetToBaseBlockTypes() {
		//clear all
		model.protectableWallMaterials.clear();
		model.alterableWallMaterials.clear();
		model.generatableWallMaterials.clear();

		//fill alterableWallMaterials
		model.alterableWallMaterials.add(Material.COBBLESTONE);

		//fill generatableWallMaterials
		for (Material m : model.protectableWallMaterials)
			model.generatableWallMaterials.add(m);
		for (Material m : model.alterableWallMaterials)
			model.generatableWallMaterials.add(m);
	}
}
