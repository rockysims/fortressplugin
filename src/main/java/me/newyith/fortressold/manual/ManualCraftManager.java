package me.newyith.fortressold.manual;

import me.newyith.fortressold.main.FortressPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BookMeta;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class ManualCraftManager {
	public static void onEnable(FortressPlugin plugin) {
		addManualRecipe(plugin);
	}

	private static void addManualRecipe(FortressPlugin plugin) {
		ItemStack manualStack = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bm = (BookMeta) manualStack.getItemMeta();
		bm.setTitle("Fortress Manual");
		bm.setAuthor("NewYith");
		List<String> pages = getManualPages();
		bm.setPages(pages);
		manualStack.setItemMeta(bm);

		ShapelessRecipe manualRecipe = new ShapelessRecipe(manualStack);
		manualRecipe.addIngredient(1, Material.OBSIDIAN);
		manualRecipe.addIngredient(1, Material.BOOK);
		plugin.getServer().addRecipe(manualRecipe);
	}

	private static List<String> getManualPages() {
		String manualStr = readManualFile();

		//replace GLOWSTONE_BURN_TIME_HOURS and GLOWSTONE_BURN_TIME_HOURS_S
		float burnHours = (float)FortressPlugin.config_glowstoneDustBurnTimeMs / (1000*60*60);
		String burnTimeHours = String.format("%.3f", burnHours);
		burnTimeHours = burnTimeHours.replaceAll("\\.?0+$", "");
		manualStr = manualStr.replaceAll("GLOWSTONE_BURN_TIME_HOURS_S", (burnHours != 1)?"s":"");
		manualStr = manualStr.replaceAll("GLOWSTONE_BURN_TIME_HOURS", burnTimeHours);

		List<String> pages = Arrays.asList(manualStr.split("===+\n"));
		return pages;
	}

	private static String readManualFile() {
		String manualStr = "Failed to read manual.txt";

		InputStream stream = ManualCraftManager.class.getResourceAsStream("manual.txt");
		if (stream != null) {
			InputStreamReader streamReader = new InputStreamReader(stream);
			BufferedReader br = new BufferedReader(streamReader);
			try {
				StringBuilder sb = new StringBuilder();

				String line = br.readLine();
				while (line != null) {
					sb.append(line);
					sb.append(System.lineSeparator());

					line = br.readLine();
				}

				manualStr = sb.toString();

				br.close();
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}

		return manualStr;
	}
}