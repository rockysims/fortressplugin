package me.newyith.fortressold.fix;

import me.newyith.fortressold.generator.FortressGeneratorRunesManager;
import me.newyith.fortressold.main.FortressPlugin;
import me.newyith.fortressold.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class PearlGlitchFix implements Listener {

	public PearlGlitchFix(FortressPlugin plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public static void onEnable(FortressPlugin plugin) {
		new PearlGlitchFix(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEnderPearlThrown(PlayerTeleportEvent event) {
		if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
			Location loc = event.getTo();
			Point target = new Point(loc);

			//cancel pearl if target or above is generated
			boolean targetGenerated = FortressGeneratorRunesManager.isGenerated(target);
			boolean aboveGenerated = FortressGeneratorRunesManager.isGenerated(target.add(0, 1, 0));
			if (targetGenerated || aboveGenerated) {
				String msg = ChatColor.AQUA + "Pearling into a fortress wall is not allowed.";
				event.getPlayer().sendMessage(msg);
				event.setCancelled(true);

				//give back ender pearl
				Player player = event.getPlayer();
				player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
			}

			boolean targetClaimed = FortressGeneratorRunesManager.isClaimed(target);
			if (targetClaimed) {
				//enforce 0.31 minimum distance from edge of block
				loc = enforceMinEdgeDist(loc, 0.31);
			}

			event.setTo(loc);
		}
	}

	private Location enforceMinEdgeDist(Location loc, double minDist) {
		Point target = new Point(loc);

		Point targetDecimal = new Point(target);
		targetDecimal.x = targetDecimal.x % 1;
		targetDecimal.y = targetDecimal.y % 1;
		targetDecimal.z = targetDecimal.z % 1;

		Point targetWhole = new Point(target);
		targetWhole.x = target.x - targetDecimal.x;
		targetWhole.y = target.y - targetDecimal.y;
		targetWhole.z = target.z - targetDecimal.z;

		//enforce minDist minimum distance from edge
		double lowLimit = minDist;
		double highLimit = 1 - minDist;
		targetDecimal.x = Math.max(lowLimit, Math.abs(targetDecimal.x));
		targetDecimal.x = Math.min(highLimit, Math.abs(targetDecimal.x));
		targetDecimal.y = Math.max(lowLimit, Math.abs(targetDecimal.y));
		targetDecimal.y = Math.min(highLimit, Math.abs(targetDecimal.y));
		targetDecimal.z = Math.max(lowLimit, Math.abs(targetDecimal.z));
		targetDecimal.z = Math.min(highLimit, Math.abs(targetDecimal.z));
		if (target.x < 0) targetDecimal.x *= -1;
		if (target.y < 0) targetDecimal.y *= -1;
		if (target.z < 0) targetDecimal.z *= -1;

		target.x = targetWhole.x + targetDecimal.x;
		target.y = targetWhole.y + targetDecimal.y;
		target.z = targetWhole.z + targetDecimal.z;

		//update loc to new target (target.toLocation() doesn't preserve direction player is looking)
		loc.setX(target.x);
		loc.setY(target.y);
		loc.setZ(target.z);

		return loc;
	}











}
