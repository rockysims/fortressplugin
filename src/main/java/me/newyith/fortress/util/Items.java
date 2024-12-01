package me.newyith.fortress.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Items {
	public static boolean tryToRemoveInventoryItems(Inventory inv, Material type, int removeCount) {
		ItemStack[] itemStacks = inv.getContents();

		// calc materialCount
		int materialCount = 0;
		for (int i = itemStacks.length - 1; i >= 0; i--) {
			ItemStack itemStack = itemStacks[i];
			if (itemStack != null && itemStack.getType() == type) {
				materialCount += itemStack.getAmount();
			}
		}

		if (materialCount < removeCount) {
			return false;
		}

		// remove removeCount of material type from inv
		int removeCountRemaining = removeCount;
		for (int i = itemStacks.length - 1; i >= 0; i--) {
			ItemStack itemStack = itemStacks[i];
			if (itemStack != null && itemStack.getType() == type) {
				int stackAmount = itemStack.getAmount();

				if (stackAmount <= removeCountRemaining) {
					inv.remove(itemStack);
					removeCountRemaining -= stackAmount;
				} else if (stackAmount > removeCountRemaining) {
					itemStack.setAmount(stackAmount - removeCountRemaining);
					removeCountRemaining = 0;
				}

				if (removeCountRemaining <= 0) {
					break;
				}
			}
		}

		return true;
	}
}
