package me.newyith.fortress.fix;

import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
		if (!event.isCancelled() && event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
			Location loc = event.getTo();
			World world = loc.getWorld();
			Point target = new Point(loc);

			//cancel pearl if target or above is generated
			boolean targetGenerated = FortressesManager.isGenerated(world, target);
			boolean aboveGenerated = FortressesManager.isGenerated(world, target.add(0, 1, 0));
			if (targetGenerated || aboveGenerated) {
				String msg = ChatColor.AQUA + "Pearling into a fortress wall is not allowed.";
				event.getPlayer().sendMessage(msg);
				event.setCancelled(true);

				//give back ender pearl
				Player player = event.getPlayer();
				player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
			}

			boolean targetClaimed = FortressesManager.isClaimed(world, target);
			if (targetClaimed) {
				//enforce 0.31 minimum distance from edge of block
				loc = enforceMinEdgeDist(loc, 0.31);
			}

			event.setTo(loc);
		}
	}

	private Location enforceMinEdgeDist(Location loc, double minDist) {
		Point target = new Point(loc);
		double xTarget = target.x();
		double yTarget = target.y();
		double zTarget = target.z();
		double xTargetDecimal = xTarget % 1;
		double yTargetDecimal = yTarget % 1;
		double zTargetDecimal = zTarget % 1;
		double xTargetWhole = xTarget - xTargetDecimal;
		double yTargetWhole = yTarget - yTargetDecimal;
		double zTargetWhole = zTarget - zTargetDecimal;

		//enforce minDist minimum distance from edge
		double lowLimit = minDist;
		double highLimit = 1 - minDist;
		xTargetDecimal = Math.max(lowLimit, Math.abs(xTargetDecimal));
		xTargetDecimal = Math.min(highLimit, Math.abs(xTargetDecimal));
		yTargetDecimal = Math.max(lowLimit, Math.abs(yTargetDecimal));
		yTargetDecimal = Math.min(highLimit, Math.abs(yTargetDecimal));
		zTargetDecimal = Math.max(lowLimit, Math.abs(zTargetDecimal));
		zTargetDecimal = Math.min(highLimit, Math.abs(zTargetDecimal));
		if (xTarget < 0) xTargetDecimal *= -1;
		if (yTarget < 0) yTargetDecimal *= -1;
		if (zTarget < 0) zTargetDecimal *= -1;

		xTarget = xTargetWhole + xTargetDecimal;
		yTarget = yTargetWhole + yTargetDecimal;
		zTarget = zTargetWhole + zTargetDecimal;

		//update loc to new target
		//Note: loc = target.toLocation(); doesn't preserve direction player is looking
		loc.setX(xTarget);
		loc.setY(yTarget);
		loc.setZ(zTarget);

		return loc;
	}



	private Location enforceMinEdgeDistOld(Location loc, double minDist) {
//		Point target = new Point(loc);
//
//		Point targetDecimal = new Point(target);
//		targetDecimal.x = targetDecimal.x % 1;
//		targetDecimal.y = targetDecimal.y % 1;
//		targetDecimal.z = targetDecimal.z % 1;
//
//		Point targetWhole = new Point(target);
//		targetWhole.x = target.x - targetDecimal.x;
//		targetWhole.y = target.y - targetDecimal.y;
//		targetWhole.z = target.z - targetDecimal.z;
//
//		//enforce minDist minimum distance from edge
//		double lowLimit = minDist;
//		double highLimit = 1 - minDist;
//		targetDecimal.x = Math.max(lowLimit, Math.abs(targetDecimal.x));
//		targetDecimal.x = Math.min(highLimit, Math.abs(targetDecimal.x));
//		targetDecimal.y = Math.max(lowLimit, Math.abs(targetDecimal.y));
//		targetDecimal.y = Math.min(highLimit, Math.abs(targetDecimal.y));
//		targetDecimal.z = Math.max(lowLimit, Math.abs(targetDecimal.z));
//		targetDecimal.z = Math.min(highLimit, Math.abs(targetDecimal.z));
//		if (target.x < 0) targetDecimal.x *= -1;
//		if (target.y < 0) targetDecimal.y *= -1;
//		if (target.z < 0) targetDecimal.z *= -1;
//
//		target.x = targetWhole.x + targetDecimal.x;
//		target.y = targetWhole.y + targetDecimal.y;
//		target.z = targetWhole.z + targetDecimal.z;
//
//		//update loc to new target (target.toLocation() doesn't preserve direction player is looking)
//		loc.setX(target.x);
//		loc.setY(target.y);
//		loc.setZ(target.z);

		return loc;
	}



}
