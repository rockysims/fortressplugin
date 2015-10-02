package me.newyith.command;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Commands {
	private static List<StuckPlayer> stuckList = new ArrayList<>();

	public static void onTick() {
		onTickStuck();
	}

	private static void onTickStuck() {
		//check if any stuck players need message or teleport
		Iterator<StuckPlayer> it = stuckList.iterator();
		while (it.hasNext()) {
			StuckPlayer player = it.next();

			if (player.isDoneWaiting()) {
				player.stuckTeleport();
				it.remove();
			} else {
				boolean cancelled = player.considerCancelling();
				if (cancelled) {
					it.remove();
				} else {
					player.considerSendingMessage();
				}
			}
		}
	}

	public static void onStuckCommand(Player player) {
		StuckPlayer alreadyStuckPlayer = null;
		for (StuckPlayer stuckPlayer : stuckList) {
			if (stuckPlayer.isPlayer(player)) {
				alreadyStuckPlayer = stuckPlayer;
			}
		}

		if (alreadyStuckPlayer == null) {
			StuckPlayer stuckPlayer = new StuckPlayer(player);
			boolean cancelStuck = stuckPlayer.considerCancelling();
			if (!cancelStuck) {
				stuckPlayer.sendStartMessage();
				stuckList.add(stuckPlayer);
			}
		} else {
			alreadyStuckPlayer.sendBePatientMessage();
		}
	}
}
