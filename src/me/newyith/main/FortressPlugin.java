package me.newyith.main;

import me.newyith.command.Commands;
import me.newyith.event.EventListener;
import me.newyith.event.TickTimer;
import me.newyith.fix.PearlGlitchFix;
import me.newyith.manual.ManualCraftManager;
import me.newyith.memory.SaveLoadMemoryManager;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FortressPlugin extends JavaPlugin {
	public static int config_glowstoneDustBurnTimeMs = 1000 * 60 * 60;
	public static int config_stuckDelayMs = 30 * 1000;
	public static int config_stuckCancelDistance = 4;
	public static int config_generationRange = 128;
	public static int config_generatorBlockLimit = 30000; //roughly 32x32x32

	private void readConfig() {
		FileConfiguration config = getConfig();
		//TODO: uncomment out following block
		/*
		config_glowstoneDustBurnTimeMs = getConfigInt(config, "glowstoneDustBurnTimeMs", config_glowstoneDustBurnTimeMs);
		config_stuckDelayMs = getConfigInt(config, "stuckDelayMs", config_stuckDelayMs);
		config_stuckCancelDistance = getConfigInt(config, "stuckCancelDistance", config_stuckCancelDistance);
		config_generationRange = getConfigInt(config, "generationRange", config_generationRange);
		config_generatorBlockLimit = getConfigInt(config, "generatorBlockLimit", config_generatorBlockLimit);
		//*/
		saveConfig();
	}
	private int getConfigInt(FileConfiguration config, String key, int defaultValue) {
		if (!config.isInt(key)) {
			config.set(key, defaultValue);
		}
		return config.getInt(key);
	}

    @Override
    public void onEnable() {
		readConfig();
		TickTimer.onEnable(this);
		EventListener.onEnable(this);
		SaveLoadMemoryManager.onEnable(this);
		ManualCraftManager.onEnable(this);
		PearlGlitchFix.onEnable(this);

        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
        sendToConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
        sendToConsole("         >> ON <<           ", ChatColor.GREEN);
        sendToConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
    }

    @Override
    public void onDisable() {
        SaveLoadMemoryManager.onDisable(this);

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

		// /fort [subCommand]
		if (commandName.equalsIgnoreCase("fort") && args.length > 0 && sender instanceof Player) {
			String subCommand = args[0];

			// /fort stuck
			if (subCommand.equalsIgnoreCase("stuck")) {
				Player player = (Player)sender;
				Commands.onStuckCommand(player);
				commandHandled = true;
			}
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




//TODO: consider fixing boat/minecart/etc glitch for getting into fortress
//	Entity vehicle = player.getVehicle(); //null if player is not riding anything

//TODO: consider fixing bug where if door is on far side of block you can pearl through it (check if factions fixes this case)
//	could fix by centering pearl teleport target (instead of 0.3 min edge dist)
//		but only when some points connected to target (by points) are generated points
//			consider just keeping global track of claimedPoints and using that
//TODO: make sure enderpearl doesn't work to go through doors/glass/etc (factions does fix it)
//	current fix seems to work for doors/glass/etc and for pearling through floor
//	for floor glitch, maybe
//		if above target is solid and origin is near target, cancel teleport
//DONE: try pearling against a wall again and make sure target point is never actually the wall
//TODO: test if you can pearl through a door before open event cancelled happens

//TODO: save everything with jackson instead of reconstructing (to reduce /reload time)

//in java a promise is called CompletableFuture
//TODO: think about making getPointsConnected() execute over time (to prevent lag) and after done collect results and start generating
//	while waiting for getPointsConnected(), leave wall as is (if generating. degenerating should still be allowed)
//	maybe make a class to represent a fortress wall update (onGenerate)
//
//	maybe make getPointsConnected send stream of layers to animator?
//		problem: without foreknowledge of blocks that will be generated, how can we make it crash tolerant


//DONE: make common blocks protectable and test tick speed for large fortress de/generate
//	during generation? (not sure): about 30ms per tick for large fortress (about 1ms per tick for small)
//	TODO: consider making animator keep track of current layer instead of searching all layers until generatable block found
//		reset current layer on de/generate

//TODO: consider protecting against the boat glitch (see https://www.youtube.com/watch?v=3qBGaqJ0yHc)


//Major:
//TODO: make generationBlockLimit limit search not generation

//Minor:
//TODO: make door white list have to be above door (leave trap door as is)
//TODO: think of a way to protect stuff inside fortress from explosions on the outside (maybe already done?)
//TODO: try to track down why getPointsConnected is being called 4 times during generation (especially the 2 heavier calls)

// --- MVP ---

//Reducing lag:
	//learn mvn so I can use jackson
	//jackson saving/loading
	//animator should remember current layer
	//getConnected calculation over time (need to learn promises in java)
//protect inside from explosions

//TODO: make 'fort stuck' only work in range of generator (almost done but need to make it based on cuboid instead of range)
//TODO: make signs on generator's base a global white list

//TODO: onProtect, if (block is solid || glass) change block to bedrock for a second then back to original material
//first try idea above with bedrock else try idea below with particles
//MAYBE MVP?: make generation display wave of particle to indicate generating wall blocks
//	onGenerateBlock, show particles appearing for a few seconds at random points on all faces not touching solid block
//		maybe use nether particle but make the nether particle be drawn toward block (like the particles are drawn to nether portal)

//TODO: add potentialAlteredPoints and update + re-save it before generation (to make it robust enough to handle server crashes)
//TODO: finish writing version of manual that includes all planned features before actually releasing MVP (just so I've thought it all out)

//TODO: add mcStats: http://mcstats.org/learn-more/

// --- --- ---

//TODO: save data using jackson instead of config?
//use jackson to save/load instead of data.yml
//http://www.mkyong.com/java/how-to-convert-java-object-to-from-json-jackson/

//TODO: consider adding flag block/item to make generate/degenerate animation run faster


//TODO: allow wall particles to be disabled via config





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

//TODO: maybe: if player carries compass then instead of team color particles they see green/yellow/red for team/ally/other
//maybe if player carries clock show team particles even if players are not moving (except self)
//i think ParticleEffects already handles player specific particles but not certain

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