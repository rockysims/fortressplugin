package me.newyith.fortress.main;

import me.newyith.fortress.command.Commands;
import me.newyith.fortress.event.EventListener;
import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.fix.PearlGlitchFix;
import me.newyith.fortress.manual.ManualCraftManager;
import me.newyith.fortress.sandbox.jackson.SandboxSaveLoadManager;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Log;
import me.newyith.fortress.util.Point;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class FortressPlugin extends JavaPlugin {
	public static final boolean releaseBuild = false; //TODO: change this to true for release builds
	private static final double saveDelayMs = 5*60*1000;
	private static int saveWaitTicks = 0;

	private static FortressPlugin instance;
	private static SaveLoadManager saveLoadManager;
	private static SandboxSaveLoadManager sandboxSaveLoadManager;

	public static int config_glowstoneDustBurnTimeMs = 1000 * 60 * 60;
	public static int config_stuckDelayMs = 30 * 1000;
	public static int config_stuckCancelDistance = 4;
	public static int config_generationRangeLimit = 128;
	public static int config_generationBlockLimit = 40000; //roughly 125 empty 8x8x8 rooms (6x6x6 air inside)

	private void loadConfig() {
		FileConfiguration config = getConfig();
		if (releaseBuild) {
			config_glowstoneDustBurnTimeMs = getConfigInt(config, "glowstoneDustBurnTimeMs", config_glowstoneDustBurnTimeMs);
			config_stuckDelayMs = getConfigInt(config, "stuckDelayMs", config_stuckDelayMs);
			config_stuckCancelDistance = getConfigInt(config, "stuckCancelDistance", config_stuckCancelDistance);
			config_generationRangeLimit = getConfigInt(config, "generationRangeLimit", config_generationRangeLimit);
			config_generationBlockLimit = getConfigInt(config, "generationBlockLimit", config_generationBlockLimit);
		}
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
		instance = this;

		loadConfig();

		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
		Log.sendConsole("         >> ON <<           ", ChatColor.GREEN);
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);

		saveLoadManager = new SaveLoadManager(this);
		saveLoadManager.load();

		if (!releaseBuild) {
			sandboxSaveLoadManager = new SandboxSaveLoadManager(this);
//			sandboxSaveLoadManager.load();
		}

		EventListener.onEnable(this);
		TickTimer.onEnable(this);
		ManualCraftManager.onEnable(this);
		PearlGlitchFix.onEnable(this);
	}

	@Override
	public void onDisable() {
		saveLoadManager.save();
		if (!releaseBuild) {
//			sandboxSaveLoadManager.save();
		}

		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
		Log.sendConsole(">>    Fortress Plugin     <<", ChatColor.GOLD);
		Log.sendConsole("         >> OFF <<          ", ChatColor.RED);
		Log.sendConsole("%%%%%%%%%%%%%%%%%%%%%%%%%%%%", ChatColor.RED);
	}

	public static void onTick() {
		if (saveWaitTicks == 0) {
//			saveLoadManager.save(); //TODO: uncomment out this line later (or decide not to save periodically)
			saveWaitTicks = (int) (saveDelayMs / TickTimer.msPerTick);
		} else {
			saveWaitTicks--;
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		String commandName = cmd.getName();
		boolean commandHandled = false;

		// /stuck
		if (commandName.equalsIgnoreCase("stuck") && sender instanceof Player) {
			Player player = (Player)sender;
			Commands.onStuckCommand(player);
			commandHandled = true;
		}

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

		if (!releaseBuild) {
			// /test
			if (cmd.getName().equalsIgnoreCase("test")) {
				if (sender instanceof Player) {
					Debug.msg("executing test command...");

					/*
					Player player = (Player) sender;
					Point p = new Point(player.getLocation());
					Material mat = p.getBlock(player.getWorld()).getType();
					Debug.msg(p + " is " + mat);
					/*/
					int distance = 40;
					Player player = (Player)sender;
					World world = player.getWorld();
					Point center = new Point(player.getLocation());

					for (int xOffset = -1 * distance; xOffset <= distance; xOffset++) {
						for (int yOffset = -1 * distance; yOffset <= distance; yOffset++) {
							for (int zOffset = -1 * distance; zOffset <= distance; zOffset++) {
								Point p = center.add(xOffset, yOffset, zOffset);
								if (p.y() > 5) {
									if (p.is(Material.BEDROCK, world)) {
										p.getBlock(world).setType(Material.COBBLESTONE);
									}
								}
							}
						}
					}
					//*/
				}
				commandHandled = true;
			}

			// /test2
			if (cmd.getName().equalsIgnoreCase("test2")) {
				if (sender instanceof Player) {
					Debug.msg("executing test2 command...");

					Player player = (Player)sender;
					World world = player.getWorld();
					Point anchor = new Point(player.getLocation()).add(0, -1, 0);

					int num = 2;
					int size = 8;
					for (int x = -1*num; x <= num; x++) {
						for (int y = -1*num; y <= num; y++) {
							for (int z = -1*num; z <= num; z++) {
								Point p = anchor.add(x*size, y*size, z*size);
								boxAt(p, world, size);
							}
						}
					}

				}
				commandHandled = true;
			}

			// /test3
			if (cmd.getName().equalsIgnoreCase("test3")) {
				if (sender instanceof Player) {
					Debug.msg("executing test3 command...");

					int distance = 40;
					Player player = (Player)sender;
					World world = player.getWorld();
					Point center = new Point(player.getLocation());

					for (int xOffset = -1 * distance; xOffset <= distance; xOffset++) {
						for (int yOffset = -1 * distance; yOffset <= distance; yOffset++) {
							for (int zOffset = -1 * distance; zOffset <= distance; zOffset++) {
								Point p = center.add(xOffset, yOffset, zOffset);
								if (p.y() > 5) {
									if (p.is(Material.COBBLESTONE, world)) {
										p.getBlock(world).setType(Material.AIR);
									}
									if (p.is(Material.GLASS, world)) {
										p.getBlock(world).setType(Material.AIR);
									}
								}
							}
						}
					}
				}
				commandHandled = true;
			}

			// /test4
			if (cmd.getName().equalsIgnoreCase("test4")) {
				if (sender instanceof Player) {
					Debug.msg("executing test4 command...");

					Player player = (Player)sender;
					Location loc = player.getLocation();
					loc.getWorld().createExplosion(loc, 3.0f);
				}
				commandHandled = true;
			}
		}

		return commandHandled;
	}

	private void boxAt(Point anchor, World world, int num) {
		Random random = new Random();
		for (int x = 0; x < num; x++) {
			for (int y = 0; y < num; y++) {
				for (int z = 0; z < num; z++) {

					boolean xMatch = x == 0 || x == num-1;
					boolean yMatch = y == 0 || y == num-1;
					boolean zMatch = z == 0 || z == num-1;
					if (xMatch || yMatch || zMatch) {
						Point p = anchor.add(x, y, z);
						Material m = Material.COBBLESTONE;
						if (random.nextBoolean()) {
							m = Material.MOSSY_COBBLESTONE;
						}
						p.getBlock(world).setType(m);
					}

				}
			}
		}
	}

	public static FortressPlugin getInstance() {
		return instance;
	}
}


//TODO: remove "Old" classes and remove "New" from new classes

//TODO: work on removing need for buildProtectedPointsByAuthToken() (or just making it run faster?)


//TODO: rename BedrockAuthToken to BedrockGroupId (and update variable names)
//	or maybe not unless I can think of a better new name

//TODO: finish changing from BedrockManagerOld to BedrockManagerNew
//	TODO: consider adding AnimationLayer to wrap layerPoints and bedrockBatch (would handle altered and protected)
//	then delete all classes ending in "Old" (and remove "New" suffix from replacements)


//TODO: retest bedrock safety (once change to new bedrock manager is done)


//TODO: consider: why not move the actual converting in BedrockManager to onTick and just keep 2 maps: origMaterialByPoint and convertCountByPoint
//	convert() and revert() just change convertCountByPoint (and dirtyPoints.addAll(updatedPoints))
//	onTick() do the actual convert/revert (update which points are actually converted to match convertCountByPoint)
//		and update origMaterialByPoint
//	doesn't that solve the lots of synchronizations issue?
//		don't even necessarily need to add bedrock batches
//MAYBE: convertCountByPoint should be convertAuthTokensByPoint? (where auth token is a BaseCore?)
//	not sure if/how jackson can save that properly (so that a whole new copy of BaseCore data is not saved)


//BedrockManager::convert(Set<Point> points) returns BedrockBatch
//BedrockManager::revert(BedrockBatch batch)
//BedrockManager::convertTimed(Set<Point> points)

//TODO: try to refactor to use batch converts
//	BedrockBatch batch = BedrockManager.convert(points);
//	batch.revert();


//class BedrockBatch
//class BedrockDoorsBatch

//maybe add abstract class BedrockBatchAuthorization
//	then each batch of bedrock can come with an authorization
//		authorization(s) can then be checked to see if


//Yona's Suggestion: add ManagedBedrockBatch class with its own timer
//	decide if its time to revert a point by checking if any batches (not expired) contain the point
//		synchronize convert/revert batch methods to solve concurrency problem
//		by batching I can batch up convert/revert calls to be faster too


//TODO: work on lag when de/generating lots of big fortresses at once
//	also make bedrock safety save each rune in a separate file and have a master bedrock safety file with names of other bedrock safety files
//		currently every time a generator turns on the whole bedrock safety file has to be written again
//			TODO: time this and make sure saving bedrock safety is the issue
//TODO: fix concurrent modification exception in BedrockManager::getMaterialByPointMapForWorld()
//	turning 2 big fortresses off and on while others are de/generating (especially right after reload) seems to cause this


//TODO: make max blocks per generator configurable (definitely needs to be configurable)

//TODO: make bedrock safety save the revert data for a while even if it was reverted
//	in case its reverted but then the minecraft world isn't
//	skip for now. worried that multiple reverts could do more than just revert abandoned bedrock

//TODO: have another look at saving (make sure it happens periodically and maybe also on world(s) save)

//TODO: consider making all containers non generatable (or make changing block to bedrock not drop contents)
//TODO: make anything that doesn't work properly non generatable (such as containers and probably much more)
//	maybe white list generatable blocks and make generatable blocks configurable
//	maybe make protected containers refuse to change to bedrock (show particles like for door)

//TODO: add config setting to specify debug level: errors, errors & warnings, all

//TODO: change name on sign from "Generator:" to "Fortress:"




//TODO: switch paused/running around so redstone on means running (and update manual)
//	I think people will scan the rune first then scan the structure and try to protect it (when they're first learning)

//TODO: don't show hearts (inside) unless all doors are on generated blocks
//	currently it can show hearts even if door can be broken/bypassed by breaking block its on

//TODO: make protection prevent breaking by water

//TODO: consider leaving in bug where piston extensions don't receive protection from explosions (and other things)
//	or think of some reasonably elegant way of handling the fact that piston extension is technically a different block
//		don't want retracted piston to still protect block piston would extend into

//TODO: make bedrock shield not apply to transparent blocks like glass
//	show a bunch of nether particles instead






//Fortress Collector Idea:
//fortress disruptor needs to be invulnerable and visible during the 1 RL day warm up (so each side can marshal an army)
//fortress collector must be built touching the fortress wall and slowly accumulates enchanted wool (disruptor fuel)
//the fortress disruptor rune would then burn enchanted wool
//	only the fortress from which the enchanted wool came is disrupted
//		PROBLEM: player can just destroy generator and create a new one (seems like a deal breaker... can't do collector idea?)

//Fortress Disruptor Idea:
//	Overview:
//	disruptor must be built touching fortress and needs time to warm up
//	this means defending players can do periodic sweeps for disruptor on their fortress
//	even if defending players know the disruptor is there the attacking players can defend the disruptor
//	attacker must balancing giving the disruptor more time to charge against the danger that it will be found and destroyed
//	also gives players a reason to set off bedrock ripples periodically (disruptor emits bedrock pings when touched by ripple)
//	Specifics:
//	rune pattern: quartz block with redstone on top and sign on side
//	must be built touching a fortress wall
//	disruptor needs to warm up for 1 RL day
//	timer counts up while redstone is off and down while redstone is on
//		counts up 1 second for every 1 minute redstone is off
//	when redstone is on and time > 0, disruptor is enabled
//		sphere of disruption expands slowly to radius of 16
//		on disabled, sphere contracts slowly
//	fairly small disruptor range (16 block radius?)
//		players can still create multiple disruptors to attack all at once
//		small radius also means fortress can be partially raided with some parts still remaining secure
//		large radius would make it too hard to sweep for disruptors
//	when bedrock ripple touches disruptor, disruptor emits bedrock pings
//		wait 5 seconds then ping 3 times (3 seconds between pings)
//		pings should be 2 wide


//Fortress Disruptor Idea (first draft):
//	Overview:
//	disruptor must be built touching fortress and needs time to warm up
//	this means defending players can do periodic sweeps for disruptor on their fortress
//		seems really hard to sweep for so maybe:
//			when bedrock ripple touches disruptor, disruptor emits 3 ripples each 3 wide (3 seconds between ripples? and 3 seconds before first)
//	even if defending players know the disruptor is there the attacking players can defend the disruptor
//	Specifics:
//	rune pattern: quartz block with sign on side
//	must be built touching a fortress wall
//	disruptor has to warm up for 1 RL day
//	any player can right click the disruptor sign to enable it (expands slowly then disrupts indefinitely)
//	fairly small disruptor range (16 block radius?)
//		players can still create multiple disruptors to attack all at once
//		small radius also means fortress can be partially raided with some parts still remaining secure
//		large radius would make it too hard to sweep for disruptors





//TODO: consider allowing bedrock ripple on unprotected blocks
//	could be useful when rebuilding fortress to see which blocks were protected
//	allows players to not use fuel but still defend against attackers temporarily
//		probably makes little difference since you can already save fuel by pausing generator until you see attackers
//			could be a fun game mode though if fuel burned really fast
//				still doesn't need ripple on unprotected to work

//TODO: consider refactoring so that each world has essentially a separate instance of the plugin (and thus separate save files)
//	should drastically decrease the number of places the world name must be saved
//	should make saving bedrockSafety.json faster

//LATER: fix bug where breaking piston while protected and extended results in piston's extension disappearing
//	maybe make converting piston to bedrock automatically convert extension too (if extended)?





//TODO: give altered blocks same protection as protected blocks (make protected include altered?)
//	then:
//		maybe make bedrock ripple change altered blocks to cobblestone (mossy?) so user can see ripple
//			maybe BedrockManager.addRevertLock(world, p)
//		maybe display particles to indicate ripple (altered and protected)?
//			probably this option because it will also make overlapping ripples visible




//TODO: better bedrock
//	DONE: make BedrockManager convert() and revert() count conversions/reversions (revert() vs fullRevert())
//	FIXED: currently quartz ripple reverts altered points
//	DONE: remove managed bedrock wave and replace with TimedBedrock
//	Problem: bedrock ripple doesn't show up when traversing generated cobblestone
//		maybe show nether particles kind of like with doors?

//TODO: consider moving bedrock ripple code from FortressesManagerForWorld to ? (maybe BaseCore?)

//TODO: continue working on timed bedrock
//	make BedrockManager.revert() undo 1 convert() call (not all convert() calls)
//		maybe do fullRevert() when degenerating? not sure its needed
//	test to make sure timed bedrock is /reload and ctrl+c safe
//	DONE: replace wave with timed bedrock
//	maybe: make all attempts to break protected blocks show timed bedrock
//		currently explosions and breaking blocks by hand show timed bedrock
//	make sure timed bedrock from breaking blocks and timed bedrock from wave interact correctly

//TODO: on right click generated point with empty hand:
//	TODO: show particle to indicate inside/outside (heart / 3 portals)
//	DONE: send out wave of bedrock from right clicked block
//		limit wave range to 16 blocks?
//			make the wave fizzle out (last few layers of wave should have increasing chance of wave block being skipped)

//TODO: add new ubiquitous blocks like granite to ungeneratable list
//	probably need to make it configurable



//TODO: consider adding a small (2 blocks wide?) wave of bedrock when generator consumes another glowstone dust
//	maybe 2 ripples where blocks between ripples indicates how many glowstone/fuel items are left








//problem: hard to tell once you've built and generated something new which blocks are actually generated
//solution:
//on right click generated point with nether quartz:
//	show particle to indicate inside/outside (heart/none?)
//	send out wave of bedrock from right clicked block
//		limit wave range to 8 blocks?
//consider reworking BedrockManager so that a block converted n times would have to be reverted n times before it really reverts
//	but have a fullRevert() method for degeneration


//TODO: fix explosion issue where players take damage through protected blocks (go back to bedrock shield with delayed, eventless explosion?)
//	SOLUTION: onPlayerExplosionDamage, do ray track between player and source and cancel if generated block in the way
// 		Done except could improve bounding box check (currently ray trace is from explosion to player's feet block
//		EntityDamageEvent cast to EntityDamageByEntityEvent allows us to get location of source of explosion


//TODO: upgrade to minecraft 1.9 or even 1.10? (yes but on other branches so 1.8, 1.9, and 1.10 can all be supported)
//	see https://www.youtube.com/watch?v=F3yNVw_22sQ&t=36s

//TODO: prioritize the full todo list


//SKIP: make redstone off mean paused (because generator has a sign that says paused, people will think to power redstone)
//	remember to update manual

//TODO: consider removing EventListener and having FortressesManagerForWorld register events directly


//------------------------------//
//		first priority			//
//------------------------------//

//tasks here
//DONE (except performance improvements): add vehicle glitch fix
//Vehicle Glitch Solution:
//	cancel pearls thrown while player is in generated point(s) (so that when in vehicle and in generated point they can't throw pearls)
//		turns out this is already done because throwing pearl while inside a block targets that block with pearl teleport and we already prevent that
//			DONE: do explicitly anyway so removing pearl glitch fix doesn't break it
//	on player exits vehicle
//		if player is in generated point
//			teleport away immediately using /stuck algorithm
//			display "You got stuck in fortress wall."

//	Assumption: Vehicle glitch only gets player inside a block not the vehicle itself (so tnt can't be used to shoot player in mine cart up through fortress floor)
//		SKIP: test if this is a valid assumption
//		even if perfectly timed use of minecart + tnt gets player into fortress its an extreme enough case that I don't think it matters

//TODO: consider onWorldSave vs periodicSave vs noSave (except on enable/disable)

//-------------------------------//
//-------------------------------//



//------------------------------//
//		next priority			//
//------------------------------//

//tasks here

//-------------------------------//
//-------------------------------//



//------------------------------//
//		safety checks			//
//------------------------------//



//-------------------------------//
//-------------------------------//



//------------------------------//
//			bugs				//
//------------------------------//

//TODO: look into why only every other layer of water gets protected
//	should probably just disable water/lava protection for now

//TODO: look into bug where when maxBlocksPerFrame limit is encountered, bedrock gets left behind (think this is fixed but need to recheck)
//	think it has something to do with the layerIndex being passed to wave (same layerIndex so old layer gets cleaned up?)
//	also protected points get left out of wave (though they still can't be broken in survival)
//	also BedrockManager seems to forget to revert some points (different issue?)
//	to reproduce:
//		make 4x4 glass wall next to generator
//		stop server
//		delete all json data files for plugin
//		start server
//		turn on generator (some points will be left as bedrock)
//		note: make sure maxBlocksPerFrame = 3 for this test


//-------------------------------//
//-------------------------------//



//------------------------------//
//			optional			//
//------------------------------//

//TODO: maybe play sound when generator is created, enabled, disabled

//TODO: remove from BedrockManager any bedrock points broken (via creative)
// or make BedrockManager check when returning material that actual material is bedrock

//TODO: add back moment of bedrock onBurn (+ onBreak, onIgnite, and onExplode) once issue where /reload causes delayed task to be forgotten is fixed
//	solution 1: onLoad, search protected points for managed bedrock that isn't wave then revert it
//	how does wave not have this problem? go look... it counts down ticks instead of using scheduled tasks
//		solution 2:
//			something like:
//			BedrockManager::revertTimed(world, point, ticks)
//				model.timedBedrocks.push(new TimedBedrock(world, point, ticks));
//					then TimedBedrock uses onTick() event to count

//maybe show in my plugin review video go over all the attacks the fortress withstands
//fortress wall vs:
//	pickaxe
//	redstone (open door)
//	fire
//	explosion
//	piston
//	pearl
//	vehicle
//	endermen


//glass, door, wood, melon, clear under area, chest inside

//DONE: test generated blocks protect from creeper explosion



//-------------------------------//
//-------------------------------//



//------------------------------//
//			ideas				//
//------------------------------//


//PROBLEM: It's really hard/dangerous to edit a fortress
//SOLUTION: right click generator with book to create editToken.
//	player with valid editToken in inventory can edit generated blocks (left click with pick to break altered block)
//	expiration += 5 minutes for each right click of book on generator
//	editToken should be a book much like the manual
//		first page has plain english info about token
//		rest of book is encrypted token data (AES encrypted and salted by server. only ever read by server)
//		token data: Point generatorAnchor, DateTime created
//	maybe rename editToken to Fortress Build Rights at least in game
//	ISSUE: anyone with editToken could break through to generator (still better than bare handed because editToken expiration is controlled)
//		solution: even with editToken, you can't break into rooms you don't have access to
//			allow player with editToken to break generatedBlock if:
//				for all fortress rooms connected by a face to generatedBlock
//					player has permission to open at least one of that room's doors
//			merge rooms when first hole between rooms is made

//	problem: risk of block duplication on crash due to allowing editToken to work
//		scenario: protect block, save fortress data, break block, save map, crash while degeneration wave passing over block
//		onLoad: bedrock safety reverts wave to original block? (broken block still in player inventory)
//			because wave is passing over, bedrock safety will revert it
//				solution: editToken doesn't work while de/generation in progress
//					then bedrock safety won't revert broken block because its not bedrock anymore


//PROBLEM: torches fall off the wall when protection wave passes over (happens when torches are a protected block)
//	also water can break protected torches
//	so maybe don't allow protected torches?
//SOLUTION: for now, anything that can be broken by water should not be protectable
//	see if there's a generic way to check if a block can be broken by water
//	or maybe see if there's a generic way to check if a block's collision size uses the full block volume and don't protect unless it does

//DISRUPTOR NOT NEEDED?
//inorder for a fortress to change, the generator must be turned off for a little while
//does that provide enough of a way in already?
//probably still want disruptor to encourage army vs army fights

//-------------------------------//
//-------------------------------//












//TODO: look into "WARNING: BedrockSafety recorded bedrock as original material" bug (done?)
//	seems to happen when switching directions of generation part way through
//	not sure if I introduced it with fix for waveReverse<4layers bug
//	SOLUTION: figured out its the altered points behind the wave that are getting saved as bedrock because their not altered via BedrockManager.convert()
//		make altered points use BedrockManager instead of altering manually

//TODO: look into bug where switching generator on then off again after exactly 3 layers of wave have converted causes protected points to skip wave returning animation
//	fixed?


//TODO: make BlockRevertData store material as int not string

//TODO: consider displaying message to players trying to break protected block like "Fortress wall cannot be destroyed without fortress disruptor."


//DONE: (almost)
//ensure there will be no abandoned bedrock by keeping a map of materialByPoint for all wallPoints
//	and then onLoad any bedrock in materialByPoint that is not supposed to be bedrock can be reverted
//		supposed to be bedrock if BedrockManager has data for point or if it's an altered point
//Note: keep periodic save and saveWithWorld (NOT done?)
//Note: save to bedrockSafety.json onGenerate


//TODO: change "/fort stuck" back to "/stuck"

//CoreWave needs to be aware of shieldPoints so it can save (and so revert to) its original material
//	fixed by refactoring to use BedrockManager?

//CoreAnimator needs to be aware of shieldPoints so it can pretend they are their original material (for isProtectable check, etc?)
//	also getGenablePoints() needs to pretend as well. make it ask BedrockManager for material map




//TODO: make any container work as generator fuel chest (trapped chest especially)

//TODO: do something to fix issue where protected things like torches can be broken repeatedly very fast if you hold down left click




//TODO: finish shield bedrock feature (onExplode, turn exploded, generated points to bedrock for 2-4 seconds)
//	currently, just to test out the idea, it turns to bedrock permanently (or looks like it should in code anyway)

//TODO: make everything multi-world safe
//	explosion canceling
//	break protection in particular
//	basically everything in FortressesManager (I seem to recall the rest of the code should be ok already)

//maybe:
//cancel explosion, changed exploded wall blocks to bedrock, then set off another explosion
//don't cancel if all exploded generated blocks are already bedrock



//TODO: look into why outside explosions can break inside chests (through protected glass)
//	see explosionInOrOut branch

//TODO: make it robust enough to handle server crashes
//	maybe I can save onWorldSave? so I don't need to track potential altered points?
//		still need to track other potential bedrock I think (such as bedrock wave)
//things to try:
//	using ctrl+c to stop server while not generating
//	using ctrl+c to stop server while generating
//	changing worlds without deleting data
//	try it along side some other people's plugins

//TODO: look into permissions... seems like it should be really easy to integrate
//TODO: add mcStats: http://mcstats.org/learn-more/

//TODO: move anything rune specific out of BaseCore and into GeneratorCore
//	DONE at least for BaseCore, CoreAnimator, CoreMaterials, CoreParticles

//good plugin review and maker asks for more plugins to review. https://www.youtube.com/watch?v=u6MbqUbcp6Q
//another plugin reviewer SgtCaze https://www.youtube.com/channel/UCWjOFfpRybxLSYRCxPOqZ4Q

//--

//TODO: consider splitting FortressesManager into CoresManager and RunesManager

//TODO: consider making alteredPoints store BlockRevertData instead of Material







//TODO: consider splitting off GeneratorRunesManager functionality into classes:
//	DoorProtection (for protecting doors and handling white list)
//	ExplosionProtection (for protecting blocks from explosions and related eye candy)
//	PistonProtection (for protecting blocks from being moved by pistons)
// or maybe just one ProtectionManager?
//	Don't split off: rune create/break, public util methods, protected/altered/etc lists,
//	add GeneratorRunesManager.isProtected() method
//	have the split off classes be called by EventListener instead of through GeneratorRunesManager


//TODO: reduce memory usage (currently with 5 giant cubes plugin takes ~160 MB as compared to minecraft which takes ~160 to ~700 MB)
//Memory Saving:
//ways that make load slower: (because periodic saving means we care more about save time than load time)
//	rebuild claims and insideOutside (instead of save/load)? seems to save/load fast but maybe try it and see if it helps
//ways without downside:
//	hard:
//		maybe add death by age for things that require a lot of memory and then rebuild JIT
//	unknown:
//		stop particles when chunk is not loaded so we can free up the wallOutsidePairs memory

//spread out periodic save over time to prevent any lag? test new saving system under load and see how fast it is

//------------------------------//
//		first priority			//
//------------------------------//


//-------------------------------//
//-------------------------------//


//------------------------------//
//		next priority			//
//------------------------------//

//TODO: add potentialAlteredPoints and update + re-save it before generation (to make it robust enough to handle server crashes)
//	probably will save onWorldSave (if possible) so no need for this

//TODO: add mcStats: http://mcstats.org/learn-more/

//TODO: consider making it so when protected blocks are broken they turn to bedrock for between 2 and 4 seconds then back

//-------------------------------//
//-------------------------------//


//------------------------------//
//			optional			//
//------------------------------//

//TODO: consider making redstone power mean run not pause (now that redstone is part of the pattern it might be ok?)
//	still seems like it would make it harder to get generator to work at first
//TODO: try to add NTB tag to disruptor fuel (creeper heads) so not all creeper heads can be used
//TODO: maybe when running/paused it should display % generated (if not 0 nor 100)
//	instead add easing function to animation speed so animation never takes all that long
//TODO: consider tracking and updating manual books so that existing copies get updated when manual changes
//TODO: make unprotectable materials list configurable

//-------------------------------//
//-------------------------------//


//TODO: consider fixing boat/minecart/etc glitch for getting into fortress (factions does not fix minecart and probably not boat)
//	check if making floor/side double think prevents this glitch from working
//	Entity vehicle = player.getVehicle(); //null if player is not riding anything
//	try to reproduce boat glitch to get in via side of fortress (see https://www.youtube.com/watch?v=3qBGaqJ0yHc)
//	think of a way to fix minecart glitch for getting in through the floor
//	think of a way to fix boat glitch for getting in through the floor/side
//	maybe:
//		keep list of ridingPlayers (in vehicles)
//		on enter/leave vehicle:
//			cancel if player would be in a generated point
//		every half second:
// 			for each ridingPlayers
//				if player is in a generated point
//					teleport to lastKnownPoint
// 					break vehicle?
//				else
//					update lastKnownPoint (also update onEnterVehicle)


//use NBT tags on disruptor fuel items (see https://hypixel.net/threads/bukkit-spigot-adding-and-checking-for-nbt-tags-on-items.383877/)



//DONE: make common blocks protectable and test tick speed for large fortress de/generate
//	during generation? (not sure): about 30ms per tick for large fortress (about 1ms per tick for small)
//	TODO: consider making animator keep track of current layer instead of searching all layers until generatable block found
//		reset current layer on de/generate
//	then retest tick speed



//Minor:
//TODO: check if you can pick up protected water/lava with bucket
//TODO: think of a way to protect stuff inside fortress from explosions on the outside (maybe already done?)
//TODO: try to track down why getPointsConnected is being called 4 times during generation (especially the 2 heavier calls)
//	could just skip this one since now getGenPreData runs on another thread

// --- MVP ---

//protect inside from explosions

//TODO: make 'fort stuck' only work in range of generator (almost done but need to make it based on cuboid instead of range) (maybe done?)
//TODO: make signs on generator's base a global white list

//TODO: finish writing version of manual that includes all planned features before actually releasing MVP (just so I've thought it all out)
//TODO: robustness

//TODO: add mcStats: http://mcstats.org/learn-more/

// --- --- ---


//TODO: consider saving wallOutsidePairs for particles instead of rebuilding it (or better spread out the recalculation)
//TODO: consider fixing bug where if one type of slab is protected then all types of slabs are

//TODO: allow wall particles to be disabled via config
//TODO: allow debug messages to be disabled via config (maybe default to disabled but only if releaseBuild)

//TODO: think about making water/lava mote create impassible wall above it



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





//for beginning for post on bukkit.org once I'm ready to release:
/*
Fortress is a whole new approach to self-service protection. Instead of claiming chunks, players scan a fortress out
of ordinary blocks then protect the structure itself by building a rune (pattern of blocks). The blocks that make up
the structure are detected automatically and protected. Runes are fueled by glowstone.
For details, obsidian + book = manual.
//*/






//TODO: consider writing another plugin that adds remote shop rune
//obsidian pillar (2+ tall) with item in item frame on side of pillar and sign(s) on side of pillar
//the sign(s) populate with sign shop text and let you buy/sell
//	change sign text color to indicate when shop can't fulfill a buy/sell order
//choose which shop comes up based on best price
//	which price (buy/sell) based on item frame being on top or bottom of pillar
//	always prioritize shops that can fulfil buy/sell order over shops that can't
//maybe add delivery fee based on distance (no one receives the fee)
//	don't allow cross world delivery








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






//class Variance {
//	static class Vehicle {}
//	static class WaterVehicle extends Vehicle {}
//	static class Boat extends WaterVehicle {}
//	static class Submarine extends WaterVehicle {}
//	static class LandVehicle extends Vehicle {}
//	static class Car extends LandVehicle {}
//	static class Bike extends LandVehicle {}
//
//	void foo() {
//		this.<LandVehicle>bar(new Car());
//	}
//
//	<T> void bar(T t) {}
//}










