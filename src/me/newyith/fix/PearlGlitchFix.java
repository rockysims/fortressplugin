package me.newyith.fix;

import me.newyith.generator.FortressGeneratorRunesManager;
import me.newyith.main.FortressPlugin;
import me.newyith.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

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

			//enforce 0.3 minimum distance from edge of block at loc
			loc = enforceMinEdgeDist(loc);

			//cancel pearl if pearling from outside to inside fortress
			Point origin = new Point(event.getFrom());
			Point target = new Point(loc);
			boolean originInside = FortressGeneratorRunesManager.isInsideFortress(origin);
			boolean targetInside = FortressGeneratorRunesManager.isInsideFortress(target);
			if (targetInside && !originInside) {
				String msg = ChatColor.AQUA + "Pearling into a fortress is not allowed.";
				event.getPlayer().sendMessage(msg);
				event.setCancelled(true);
			}

			event.setTo(loc);
		}
	}

	private Location enforceMinEdgeDist(Location loc) {
		Point target = new Point(loc);

		Point targetDecimal = new Point(target);
		targetDecimal.x = targetDecimal.x % 1;
		targetDecimal.y = targetDecimal.y % 1;
		targetDecimal.z = targetDecimal.z % 1;

		Point targetWhole = new Point(target);
		targetWhole.x = target.x - targetDecimal.x;
		targetWhole.y = target.y - targetDecimal.y;
		targetWhole.z = target.z - targetDecimal.z;

		//enforce 0.3 minimum distance from edge
		double lowLimit = 0.3 + 0.01;
		double highLimit = 0.7 - 0.01;
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
