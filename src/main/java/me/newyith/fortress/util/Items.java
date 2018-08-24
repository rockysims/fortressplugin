package me.newyith.fortress.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Items {
	public static boolean tryToRemoveOneInventoryItem(Inventory inv, Material type) {
		ItemStack[] itemStacks = inv.getContents();
		for (int i = itemStacks.length - 1; i >= 0; i--) {
			ItemStack itemStack = itemStacks[i];
			if (itemStack != null && itemStack.getType() == type) {
				int stackAmount = itemStack.getAmount();
				if (stackAmount == 1) {
					inv.remove(itemStack);
					return true;
				} else if (stackAmount > 1) {
					itemStack.setAmount(stackAmount - 1);
					return true;
				}
			}
		}
		return false;
	}
}
