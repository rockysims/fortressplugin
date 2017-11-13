package me.newyith.fortressOrig.fix;

import me.newyith.fortressOrig.main.FortressPlugin;
import me.newyith.fortressOrig.main.FortressesManager;
import me.newyith.fortressOrig.util.Blocks;
import me.newyith.fortressOrig.util.Point;
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
			Point above = target.add(0, 1, 0);

			boolean validTarget = true;
			validTarget = validTarget && !FortressesManager.forWorld(world).isGenerated(target);
			validTarget = validTarget && !FortressesManager.forWorld(world).isGenerated(above);
			if (validTarget) {
				boolean safeTarget = true;
				safeTarget = safeTarget && !FortressesManager.forWorld(world).isClaimed(target);
				safeTarget = safeTarget && !FortressesManager.forWorld(world).isClaimed(above);
				if (!safeTarget) {
					boolean targetAiry = Blocks.isAiry(target, world);
					if (targetAiry) {
						//enforce 0.31 minimum distance from edge of block
						loc = enforceMinEdgeDist(loc, 0.31);
						loc.setY(loc.getBlockY()); //y = y - y % 1
						event.setTo(loc);
					} else {
						//cancel because can't safely floor y (player could be standing on slab)
						onCancelPearl(event.getPlayer(), "Pearl glitch via fortress wall is not allowed.");
						event.setCancelled(true);
					}
				}
			} else { //invalid target
				onCancelPearl(event.getPlayer(), "Pearling into a fortress wall is not allowed.");
				event.setCancelled(true);
			}
		}
	}

	private void onCancelPearl(Player player, String msg) {
		player.sendMessage(ChatColor.AQUA + msg);
		player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
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
}
