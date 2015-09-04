package me.newyith.generator;

import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class WallMaterials {
	private Point anchorPoint;
	private Set<Material> wallMaterials = new HashSet<>();
	private Set<Material> generatableWallMaterials = new HashSet<>();
	private Set<Material> protectableWallMaterials = new HashSet<>();
	private Set<Material> alterableWallMaterials = new HashSet<>();
//	private long lastRefreshStamp = 0;

	public WallMaterials(Point anchorPoint) {
		this.anchorPoint = anchorPoint;
	}

	// - Getters -

	public Set<Material> getWallMaterials() {
//		considerRefresh();
		return wallMaterials;
	}

	public Set<Material> getGeneratableWallMaterials() {
//		considerRefresh();
		return generatableWallMaterials;
	}

	public boolean isProtectableWallMaterial(Material m) {
//		considerRefresh();
		return protectableWallMaterials.contains(m);
	}

	public boolean isAlterableWallMaterial(Material m) {
		return alterableWallMaterials.contains(m);
	}

	// - Refreshing -

//	private void considerRefresh() {
//		long now = (new Date()).getTime();
//		//Debug.msg("now - lastRefreshStamp: " + (now - lastRefreshStamp));
//		if (now - lastRefreshStamp > 1000) { //ms
//			lastRefreshStamp = now;
//			refresh();
//		}
//	}

	public void refresh() {
		Debug.msg("WallMaterials refresh()ed. anchor: " + anchorPoint);

		resetToBaseBlockTypes();

		ItemStack[] items = getChestContents();
		for (ItemStack item : items) {
			if (item != null) {
				Material mat = item.getType();
				switch (mat) {
					//non protectable
					case GRASS:
					case DIRT:
					case STONE:
					case GRAVEL:
					case SAND:
					case BEDROCK:
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
					default:
						if (mat.isBlock()) {
							addProtectable(item.getType());
						}
				}
			}
		}
	}
	private ItemStack[] getChestContents() {
		FortressGeneratorRune rune = FortressGeneratorRunesManager.getRune(anchorPoint);
		if (rune != null) {
			Block chestBlock = rune.getPattern().chestPoint.getBlock();
			if (chestBlock.getState() instanceof Chest) {
				Chest chest = (Chest)chestBlock.getState();
				Inventory inv = chest.getInventory();
				ItemStack[] items = inv.getContents();

				return items;
			}
		}

		Debug.error("wallMats.getChestContents() failed. returning empty list");
		return new ItemStack[0];
	}
	private void addProtectable(Material mat) {
		protectableWallMaterials.add(mat);
		generatableWallMaterials.add(mat);
		wallMaterials.add(mat);
	}

	private void resetToBaseBlockTypes() {
		//clear all
		protectableWallMaterials.clear();
		alterableWallMaterials.clear();
		generatableWallMaterials.clear();
		wallMaterials.clear();

//		//fill protectableWallMaterials
//		protectableWallMaterials.add(Material.GLASS);
//		//iron door
//		protectableWallMaterials.add(Material.IRON_DOOR_BLOCK);
//		//wooden doors
//		protectableWallMaterials.add(Material.WOODEN_DOOR);
//		protectableWallMaterials.add(Material.ACACIA_DOOR);
//		protectableWallMaterials.add(Material.BIRCH_DOOR);
//		protectableWallMaterials.add(Material.DARK_OAK_DOOR);
//		protectableWallMaterials.add(Material.JUNGLE_DOOR);
//		protectableWallMaterials.add(Material.SPRUCE_DOOR);
//		//trap doors
//		protectableWallMaterials.add(Material.TRAP_DOOR);
//		protectableWallMaterials.add(Material.IRON_TRAPDOOR);

		//fill alterableWallMaterials
		alterableWallMaterials.add(Material.COBBLESTONE);

		//fill generatableWallMaterials
		for (Material m : protectableWallMaterials)
			generatableWallMaterials.add(m);
		for (Material m : alterableWallMaterials)
			generatableWallMaterials.add(m);

		//fill wallMaterials
		for (Material m : generatableWallMaterials)
			wallMaterials.add(m);
		wallMaterials.add(Material.BEDROCK);
	}

}
