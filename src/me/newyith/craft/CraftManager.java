package me.newyith.craft;

import me.newyith.main.FortressPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class CraftManager {
	private FortressPlugin plugin;

	public CraftManager(FortressPlugin plugin) {
		this.plugin = plugin;
		addManualRecipe();
	}

	public static void onEnable(FortressPlugin plugin) {
		new CraftManager(plugin);
	}

	private void addManualRecipe() {
		ItemStack manualStack = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bm = (BookMeta) manualStack.getItemMeta();
		bm.setTitle("Fortress Manual");
		bm.setAuthor("NewYith");
		List<String> pages = new ArrayList<>();
		pages.add("first page");
		pages.add("second page");
		bm.setPages(pages);
		manualStack.setItemMeta(bm);

		ShapelessRecipe manualRecipe = new ShapelessRecipe(manualStack);
		manualRecipe.addIngredient(1, Material.OBSIDIAN);
		manualRecipe.addIngredient(1, Material.BOOK);
		plugin.getServer().addRecipe(manualRecipe);
	}
}
