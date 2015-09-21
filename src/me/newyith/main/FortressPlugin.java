package me.newyith.main;

import me.newyith.commands.Commands;
import me.newyith.event.EventListener;
import me.newyith.event.TickTimer;
import me.newyith.manual.ManualCraftManager;
import me.newyith.memory.ConfigManager;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FortressPlugin extends JavaPlugin {
	public static int config_glowstoneDustBurnTimeMs = 1000*60*60; //TODO: add this to config and make other places in code use this value
	//TODO: add more config_whatever values

    @Override
    public void onEnable() {
		TickTimer.onEnable(this);
		EventListener.onEnable(this);
		ConfigManager.onEnable(this);
		ManualCraftManager.onEnable(this);

        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
        sendToConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
        sendToConsole("         >> ON <<           ", ChatColor.GREEN);
        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
    }

    @Override
    public void onDisable() {
        ConfigManager.onDisable(this);

		sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
		sendToConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
        sendToConsole("         >> OFF <<          ", ChatColor.RED);
        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
    }

    private void sendToConsole(String s, ChatColor color) {
        ConsoleCommandSender console = this.getServer().getConsoleSender();
        console.sendMessage(color + s);
    }


    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		String commandName = cmd.getName();
		boolean commandHandled = false;

		// /stuck
		if (commandName.equalsIgnoreCase("stuck")) {
			if (sender instanceof Player) {
				Player player = (Player)sender;
				Commands.onStuckCommand(player);
			}
			commandHandled = true;
		}

		//TODO: remove this command
		// /test
		if (cmd.getName().equalsIgnoreCase("test")) {
			if (sender instanceof Player) {
				Debug.msg("executing test command...");

				int distance = 20;
				Player player = (Player)sender;
				Point center = new Point(player.getLocation());

				for (int xOffset = -1 * distance; xOffset <= distance; xOffset++) {
					for (int yOffset = -1 * distance; yOffset <= distance; yOffset++) {
						for (int zOffset = -1 * distance; zOffset <= distance; zOffset++) {
							Point p = new Point(center.world, center.x + xOffset, center.y + yOffset, center.z + zOffset);
							if (p.y > 5) {
								if (p.getBlock().getType() == Material.BEDROCK) {
									p.getBlock().setType(Material.COBBLESTONE);
								}
							}
						}
					}
				}
			}
			commandHandled = true;
		}

        return commandHandled;
    }



}

// --- MVP ---

//TODO: change to new rune pattern
//TODO: make signs on generator's base a global white list
//TODO: make placing sign be the way to activate rune
//TODO: make swapping redstone/chest positions still work
//TODO: change '/stuck' to '/fort stuck' and make delay configurable
//TODO: increase generation range (64? at least some). make range configurable

//TODO: save data into a file instead of config (so config won't get too big to open)
//TODO: add potentialAlteredPoints and update + re-save it before generation (to make it robust enough to handle server crashes)

//MAYBE MVP?: make generation display wave of particle to indicate generating wall blocks
//	onGenerateBlock, show particles appearing for a few seconds at random points on all faces not touching solid block
//		maybe use nether particle but make the nether particle be drawn toward block (like the particles are drawn to nether portal)

//maybe finish writing version of manual that includes all planned features before actually releasing MVP

// --- --- ---





//teams:
//on base white list sign created, prompt named player(s) not already on team to type '/fort join [teamColor]'
//if unallied player on base white list tries to open protected door, cancel event and prompt to type '/fort join [teamColor]'
//after joining, player remains teamed until joining another team or typing '/fort leave'
//teamed players can't damage each other
//for each player in a team
//	if a non team/ally player is within 16 blocks of player
//		if player is moving
//			display colored particles around player's feet (color based on dye in fortress chest)

//team color and allies is based on fuel chest contents:
//first dye: selects team color
//	allow multiple teams to be same color (differentiate teams by number of dye items in stack)
//	if someone else has already claimed that team color, tell player team color is taken
//		after 24 hours of not being on, generator's team color is unclaimed
//	if no valid team color indicated, team particles are both white and black (interspersed)
//	replace generator's running indicator particles with team particles
//other dyes: allies you with other team(s) if alliance is mutual
//	while allied, enchant dye item with Protection I (remove enchant if removed from chest)
//quartz item: ignore white listed names not also on generator's base.
//	that way removing someone from base white list signs is an effective way to kick from team
//		make doors with white list sign(s) but no valid names fall back to base white list signs

//commands:
/*
/fort stuck
/fort home
/fort info [teamColor?] (display owner, teamColor, ally teamColors)
/fort list (display team members starting with owner)
/fort leave
/fort join [teamColor?] (example: '/fort join green3')
	if (teamColor specified)
		try to join team (player must be on base white list of the generator claiming that team color)
	else
		display list of teamColors (if any teams have invited the player)
Note: To kick, remove name from base sign
TODO: remove home and key runes
*/



//TODO: make /stuck only work within generation range of a generator

//TODO: change rune pattern to alternate 3x2x1 -ish rune pattern
//TODO: make rune pattern ignore air points in pattern (maybe make '*' mean any block material?)
//TODO: write manual book
//TODO: add potentialAlteredPoints and update + re-save it before generation (to make it robust enough to handle server crashes)
//	then onEnable look through potentialAlteredPoints and unalter where point not found among generated points
//  then test killing the server (ctrl+c not "stop") and make sure plugin is robust enough to handle it
//TODO: make generation display wave of particle to indicate generating wall blocks
//	onProtectBlock, show particles appearing for a few seconds at random points on all faces not touching solid block
//		maybe use nether particle but make the nether particle be drawn toward center of block (like the particles are drawn to nether portal)
//TODO: allow chest / redstone to be swapped and have rune remain valid
//TODO: instead of breaking generator when redstone is cycled too quickly, just ignore changes to redstone power for 1 second after a change?


//TODO: add emergency key rune
//TODO: add disruptor rune


//low priority:
//TODO: consider making Point immutable (final)
//TODO: refactor to use the listener pattern?
//TODO: in Wall class and other places its used: rename wallMaterials to traverseMaterials
//TODO: consider making mossy cobblestone be generated but not transmit generation to anything except mossy
//TODO: consider making rune activation require an empty hand
//TODO: consider making creating rune require empty hand (again)
//TODO: make glowstone blocks work as fuel for 4x the fuel value of glowstone dust (silk touch works on glowstone block and fortune III does not)

/* New Feature:
make pistons transmit generation when extended
    this will serve as a switch to allow nearby buildings to connect/disconnect from fortress generation
    pistons should have particle to indicate when the piston has been found by a fortress generator (onGeneratorStart searches)
    pistons should not be protected (breakable)
//*/

/*
pistonCores should respect even its parent generator's claims
pistonCores should respect other pistonCores' claims
	other generatorCores' claimed points including their pistonCores' claims and also any other pistonCores belonging to parent generator
pistonCore's wallMaterials should be based on parent generator's wallMaterials

on piston added to claimedWallPoints:
	create new PistonCore
on piston removed from claimedWallPoints:
	break pistonCore
on generator broken:
	break all its pistonCores

pistonCore:
	onExtend:
		if (parent generator is running)
			if (piston protected || piston extended to touch protected) tell pistonCore to generate
	onRetract:
		tell pistonCore to degenerate

generatorCore:
	onProtectPiston, onProtectPistonExtensionTouchPoint:
		set pistonCore.layerIndex
		if (extended) tell pistonCore to generate
	onGenerate:
		for each pistonCore
			if (pistonCore is protected)
				tell pistonCore to generate
	onDegenerate:
		include child pistonCores' generated
			use pistonCore.layerIndex to merge piston's generated with generator's generated

maybe instead of requiring piston be protected before it can work as a mini generator just require that a pistonCore has been created
	also create pistonCore if block piston is extended to touch is generated
*/









//		int taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
//			for (Player player : Bukkit.getServer().getOnlinePlayers()) {
//				Point point = new Point(player.getLocation().add(0, 2, 0));
//				float speed = 1;
//				int amount = 1;
//				double range = 10;
//				ParticleEffect.PORTAL.display(0, 0, 0, speed, amount, point, range);
//				Bukkit.broadcastMessage("display portal at " + point);
//			}
//		}, 0, 20); //20 ticks per second
//		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
//			//
//			Bukkit.getServer().getScheduler().cancelTask(taskId);
//			Bukkit.broadcastMessage("canceling taskId: " + taskId);
//		}, 20*120);