package me.newyith.fix;

import me.newyith.main.FortressPlugin;
import me.newyith.particle.ParticleEffect;
import me.newyith.util.Debug;
import me.newyith.util.Point;
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
	public void enderPearlThrown(PlayerTeleportEvent event) {
		if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
			Point origin = new Point(event.getFrom());
			Point target = new Point(event.getTo());



			/*
			if target is closer than 0.3 from any edge of block in any direction,
			look in that direction for solid block and if found increase distance from edge to 0.3
			 */
//			if (target.x % 1 > 0.7) {
//				Point p = new Point(target);
//				p.add(0, 0, 0);
//			}

			Point targetDecimal = new Point(target);
			targetDecimal.x = targetDecimal.x % 1;
			targetDecimal.y = targetDecimal.y % 1;
			targetDecimal.z = targetDecimal.z % 1;
			Point targetWhole = new Point(target);
			targetWhole.x = target.x - targetDecimal.x;
			targetWhole.y = target.y - targetDecimal.y;
			targetWhole.z = target.z - targetDecimal.z;

			//enforce 0.3 minimum distance from edge
			targetDecimal.x = Math.max(0.3 + 0.01, Math.abs(targetDecimal.x));
			targetDecimal.x = Math.min(0.7 - 0.01, Math.abs(targetDecimal.x));
			targetDecimal.y = Math.max(0.3 + 0.01, Math.abs(targetDecimal.y));
			targetDecimal.y = Math.min(0.7 - 0.01, Math.abs(targetDecimal.y));
			targetDecimal.z = Math.max(0.3 + 0.01, Math.abs(targetDecimal.z));
			targetDecimal.z = Math.min(0.7 - 0.01, Math.abs(targetDecimal.z));
			if (target.x < 0) targetDecimal.x *= -1;
			if (target.y < 0) targetDecimal.y *= -1;
			if (target.z < 0) targetDecimal.z *= -1;

//			Set<Point> adjacent6 = Wall.getAdjacent6(target);
//			adjacent6.forEach(adj -> {
//				//TODO: check if decimal really needs to enforce 0.3 min distance from edge
//			});

			Debug.msg("targetWhole: " + targetWhole.toStringDoubles());
			Debug.msg("targetDecimal: " + targetDecimal.toStringDoubles());

			Debug.msg("old target: " + target.toStringDoubles());
			target.x = targetWhole.x + targetDecimal.x;
			target.y = targetWhole.y + targetDecimal.y;
			target.z = targetWhole.z + targetDecimal.z;
			Debug.msg("new target: " + target.toStringDoubles());

			//set teleport target (preserving direction player is looking)
			Location loc = event.getTo();
			loc.setX(target.x);
			loc.setY(target.y);
			loc.setZ(target.z);
			event.setTo(loc);

			Debug.msg("real target: " + new Point(event.getTo()).toStringDoubles());
//
//			Debug.msg("origin: " + origin.toStringDoubles() + " (" + origin.getBlock().getType() + ")");
//			Debug.msg("target: " + target.toStringDoubles() + " (" + target.getBlock().getType() + ")");




			Debug.particleAt(origin, ParticleEffect.FLAME);
			Debug.particleAt(target, ParticleEffect.CRIT_MAGIC);
			Debug.particleAt(origin, ParticleEffect.FLAME);
			Debug.particleAt(target, ParticleEffect.CRIT_MAGIC);
			Debug.particleAt(origin, ParticleEffect.FLAME);
			Debug.particleAt(target, ParticleEffect.CRIT_MAGIC);
			Debug.particleAt(origin, ParticleEffect.FLAME);
			Debug.particleAt(target, ParticleEffect.CRIT_MAGIC);
//			Debug.msg("origin: " + origin.toStringDoubles() + " (" + origin.getBlock().getType() + ")");
//			Debug.msg("target: " + target.toStringDoubles() + " (" + target.getBlock().getType() + ")");

			//cancel pearl when target block is origin block (ender pearl through doors/glass/etc glitch only seems to work when origin == target)
//			if (origin.equals(target)) {
//				event.setCancelled(true);
//			}


//			//make target exact center of block
//			Location loc = event.getTo();
//			loc.setX(loc.getBlockX() + 0.5);
//			loc.setY(loc.getBlockY() + 0.0);
//			loc.setZ(loc.getBlockZ() + 0.5);
//			event.setTo(loc);





			/*
			onThisEvent:
				if player would end up part way in solid block, cancel
				if target.y != origin.y, look for solid block between and cancel if found
			 */
		}
	}











}
