package me.newyith.fortress.rune.generator;

import me.newyith.fortress.core.GeneratorCore;
import me.newyith.fortress.event.TickTimer;
import me.newyith.fortress.core.CoreMaterials;
import me.newyith.fortress.main.FortressPlugin;
import me.newyith.fortress.main.FortressesManager;
import me.newyith.fortress.util.Cuboid;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.*;

//fully written again (except particles manager which should go in a core)
public class GeneratorRune {
	private static class Model {
		private GeneratorRunePattern pattern = null;
		private GeneratorCore core = null;
		private int fuelTicksRemaining = 0;
		private boolean powered = false;
		private GeneratorState state = GeneratorState.NULL;

		private transient List<Long> powerToggleTimeStamps = null;

		@JsonCreator
		public Model(@JsonProperty("pattern") GeneratorRunePattern pattern,
					 @JsonProperty("core") GeneratorCore core,
					 @JsonProperty("state") GeneratorState state,
					 @JsonProperty("fuelTicksRemaining") int fuelTicksRemaining,
					 @JsonProperty("powered") boolean powered) {
			this.pattern = pattern;
			this.core = core;
			this.state = state;
			this.fuelTicksRemaining = fuelTicksRemaining;
			this.powered = powered;

			//rebuild transient fields
			powerToggleTimeStamps = new ArrayList<>();
		}
	}
	private Model model = null;

	@JsonCreator
	public GeneratorRune(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public GeneratorRune(GeneratorRunePattern pattern) {
		CoreMaterials coreMats = new CoreMaterials(pattern.getWorld(), pattern.getChestPoint());
		GeneratorCore core = new GeneratorCore(pattern.getWorld(), pattern.getAnchorPoint(), coreMats);
		GeneratorState state = GeneratorState.NULL;
		int fuelTicksRemaining = 0;
		boolean powered = false;
		model = new Model(pattern, core, state, fuelTicksRemaining, powered);
	}

	public void secondStageLoad() {
		/* rebuild version (currently saving it instead)
		model.core.updateInsideOutside(); //updateInsideOutside() needs to be called before onGeneratedChanged() so layerOutside is full
		//*/
		model.core.onGeneratedChanged(); //update which particles should be displayed (requires layerOutside already be filled)

		updatePoweredFromWorld(); //powered state may have changed if server crashed so recheck
	}

	//-----------------------------------------------------------------------

	// - Getters -

	public GeneratorRunePattern getPattern() {
		return model.pattern;
	}

	private boolean isRunning() {
		return model.state == GeneratorState.RUNNING;
	}

	private boolean isPaused() {
		return model.state == GeneratorState.PAUSED;
	}

	private boolean isPowered() {
		return model.powered;
	}

	public GeneratorCore getGeneratorCore() {
		return model.core;
	}

	public Set<Point> getLayerOutsideFortress() {
		return model.core.getLayerOutsideFortress();
	}

	public Set<Point> getGeneratedPoints() {
		return model.core.getGeneratedPoints();
	}

	public Cuboid getFortressCuboid() {
		Point anchor = model.pattern.getAnchorPoint();
		Vector min = new Point(anchor).toVector();
		Vector max = new Point(anchor).toVector();

		Set<Point> points = new HashSet<>();
		points.addAll(model.pattern.getPoints());
		points.addAll(getGeneratedPoints());
		for (Point p : points) {
			min.setX(Math.min(min.getX(), p.x()));
			min.setY(Math.min(min.getY(), p.y()));
			min.setZ(Math.min(min.getZ(), p.z()));
			max.setX(Math.max(max.getX(), p.x()));
			max.setY(Math.max(max.getY(), p.y()));
			max.setZ(Math.max(max.getZ(), p.z()));
		}

		//min-- and max++ (so as to include claimed points)
		Point minPoint = new Point(min).add(-1, -1, -1);
		Point maxPoint = new Point(max).add(1, 1, 1);

		return new Cuboid(minPoint, maxPoint, model.pattern.getWorld());
	}

	// - Events -

	public void onTick() {
		tickFuel();
		model.core.tick();
	}

	public void onCreated(Player player) {
		moveBlockTo(Material.GOLD_BLOCK, model.pattern.getRunningPoint());
		moveBlockTo(Material.DIAMOND_BLOCK, model.pattern.getAnchorPoint());

		updatePoweredFromWorld(); //initialize model.powered
		updateState();

		boolean placed = model.core.onCreated(player);
		if (!placed) {
			FortressesManager.breakRune(this);
		}
	}

	public void onBroken() {
		moveBlockTo(Material.DIAMOND_BLOCK, model.pattern.getRunningPoint());
		moveBlockTo(Material.GOLD_BLOCK, model.pattern.getAnchorPoint());
		setSignText("Broken", "", "");

		model.core.onBroken();
	}

	public void onPlayerRightClickWall(Player player, Block block, BlockFace face) {
		model.core.onPlayerRightClickWall(player, block, face);
	}

	private void updatePoweredFromWorld() {
		boolean powered = false;
		Point wirePoint = model.pattern.getWirePoint();
		if (wirePoint != null) {
			powered = wirePoint.getBlock(model.pattern.getWorld()).getBlockPower() > 0;
		}
		setPowered(powered);
	}

	public void setPowered(boolean powered) {
		if (model.powered != powered) {
			if (countRecentPowerToggles() > 10) {
				FortressesManager.breakRune(this);
			} else {
				model.powerToggleTimeStamps.add(System.currentTimeMillis()); //used by countRecentPowerToggles()
				model.powered = powered;
				updateState();
			}
		}
	}

	public void onSearchingChanged(boolean searching) {
		//TODO: comment out again (see next line)
		//* //commented out because flashing "Searching" for a fraction of a second looks bad
		if (searching) {
			setSignText("Searching", null, null);
		} else {
			//change back line1 to match state
			switch (model.state) {
				case RUNNING:
					setSignText("Running", null, null);
					break;
				case PAUSED:
					setSignText("Paused", null, null);
					break;
				case NEEDS_FUEL:
					setSignText("Needs Fuel", null, null);
					break;
				default:
					Debug.error("GeneratorRune::setState() couldn't find a case matching GeneratorState: " + model.state);
			}
		}
		//*/
	}

	// - Handlers -

	private void tickFuel() {
		if (model.fuelTicksRemaining > 0 && isRunning()) {
			model.fuelTicksRemaining--;
		}

		if (model.fuelTicksRemaining <= 0) {
			tryReplenishFuel();
			updateState();
		}

		//always update sign in case amount of fuel in chest has changed
		updateFuelRemainingDisplay(model.fuelTicksRemaining * TickTimer.msPerTick);
	}
	private void tryReplenishFuel() {
		Chest chest = getChest();
		if (chest != null) {
			Inventory inv = chest.getInventory();
			if (inv.contains(Material.GLOWSTONE_DUST)) {
				inv.removeItem(new ItemStack(Material.GLOWSTONE_DUST, 1));
				chest.update(true);

				model.fuelTicksRemaining = FortressPlugin.config_glowstoneDustBurnTimeMs / TickTimer.msPerTick;
				updateFuelRemainingDisplay(model.fuelTicksRemaining * TickTimer.msPerTick);
			}
		}
	}

	private void updateState() {
		if (model.fuelTicksRemaining == 0) {
			tryReplenishFuel();
		}

		if (model.fuelTicksRemaining > 0) {
			if (isPowered()) {
				setState(GeneratorState.PAUSED);
			} else {
				setState(GeneratorState.RUNNING);
			}
		} else {
			setState(GeneratorState.NEEDS_FUEL);
		}
	}

	private void setState(GeneratorState state) {
		if (model.state != state) {
//			World world = model.pattern.getWorld();
//			Location anchor = model.pattern.getAnchorPoint().toLocation(world);
			switch (state) {
				case RUNNING:
					setSignText("Running", "", null);
					moveBlockTo(Material.GOLD_BLOCK, model.pattern.getRunningPoint());
//					world.playSound(anchor, Sound.SHEEP_SHEAR, 5, 1); //5 (volume), 1 (pitch) is hopefully normal
					break;
				case PAUSED:
					setSignText("Paused", "", null);
					moveBlockTo(Material.GOLD_BLOCK, model.pattern.getPausePoint());
//					world.playSound(anchor, Sound.IRONGOLEM_THROW, 5, 1);
					break;
				case NEEDS_FUEL:
					setSignText("Needs Fuel", "(glowstone dust)", "");
					moveBlockTo(Material.GOLD_BLOCK, model.pattern.getFuelPoint());
					break;
				default:
					Debug.error("GeneratorRune::setState() couldn't find a case matching GeneratorState: " + state);
			}

			model.state = state;
			model.core.setActive(state == GeneratorState.RUNNING);
		}
	}

	// - Utils -

	public int countGlowstoneDustInChest() {
		int count = 0;
		Chest chest = getChest();
		if (chest != null) {
			Inventory inv = chest.getInventory();
			ItemStack[] items = inv.getContents();
			for (ItemStack item : items) {
				if (item != null && item.getType() == Material.GLOWSTONE_DUST) {
					count += item.getAmount();
				}
			}
		}
		return count;
	}

	private Chest getChest() {
		Point chestPoint = model.pattern.getChestPoint();
		if (chestPoint != null) {
			Block chestBlock = chestPoint.getBlock(model.pattern.getWorld());

			if (chestBlock.getState() instanceof Chest) {
				Chest chest = (Chest)chestBlock.getState();
				return chest;
			}
		}
		return null;
	}

	private void updateFuelRemainingDisplay(long ms) {
		int glowstoneDustInChest = countGlowstoneDustInChest();
		ms += FortressPlugin.config_glowstoneDustBurnTimeMs * glowstoneDustInChest;

		long s = ms / 1000;
		long m = s / 60;
		long h = m / 60;
		long d = h / 24;
		h = h % 24;
		m = m % 60;
		s = s % 60;
		StringBuilder str = new StringBuilder();
		if (d > 0) {
			str.append(d + "d ");
		}
		if (h > 0) {
			str.append(h + "h ");
		}
		if (m > 0) {
			str.append(m + "m ");
		}
		if (s > 0) {
			str.append(s + "s");
		}
		setSignText(null, null, str.toString());
	}

	private boolean setSignText(String line1, String line2, String line3) {
		Point signPoint = model.pattern.getSignPoint();
		if (signPoint != null) {
			Block signBlock = signPoint.getBlock(model.pattern.getWorld());
			if (signBlock != null) {
				BlockState blockState = signBlock.getState();
				if (blockState instanceof Sign) {
					Sign sign = (Sign)blockState;
					if (sign != null) {
						sign.setLine(0, "Generator:");
						if (line1 != null) {
							sign.setLine(1, line1);
						}
						if (line2 != null) {
							sign.setLine(2, line2);
						}
						if (line3 != null) {
							sign.setLine(3, line3);
						}
						sign.update();
						return true;
					}
				} else {
					//this can happen when a sign is exploded (generator broken check has to delay while explosion to finishes)
					Debug.warn("setSignText() failed to find sign at " + signPoint);
				}
			}
		}
		return false;
	}

	private void moveBlockTo(Material material, Point targetPoint) {
		Point materialPoint = null;
		ArrayList<Point> points = new ArrayList<>();
		points.add(model.pattern.getAnchorPoint());
		points.add(model.pattern.getRunningPoint());
		points.add(model.pattern.getPausePoint());
		points.add(model.pattern.getFuelPoint());
		for (Point p : points) {
			if (p.is(material, model.pattern.getWorld())) {
				materialPoint = p;
			}
		}

		if (materialPoint != null) {
			swapBlocks(materialPoint, targetPoint);
		}
	}

	private void swapBlocks(Point a, Point b) {
		World world = model.pattern.getWorld();
		Material aMat = a.getBlock(world).getType();
		Material bMat = b.getBlock(world).getType();
		a.getBlock(world).setType(bMat);
		b.getBlock(world).setType(aMat);
	}

	private int countRecentPowerToggles() {
		//set count recent power toggles and remove expired stamps
		long now = System.currentTimeMillis();
		int stampLifetimeMs = 5*1000;
		int count = 0;
		Iterator<Long> it = model.powerToggleTimeStamps.iterator();
		while (it.hasNext()) {
			Long stamp = it.next();
			if (now - stamp < stampLifetimeMs) {
				count++;
			} else {
				it.remove();
			}
		}

		return count;
	}

	@Override
	public boolean equals(Object ob) {
		boolean match = false;

		if (ob instanceof GeneratorRune) {
			GeneratorRune rune = (GeneratorRune) ob;
			match = model.pattern.getAnchorPoint() == rune.getPattern().getAnchorPoint();
		}

		return match;
	}

	@Override
	public int hashCode() {
		return model.pattern.getAnchorPoint().hashCode();
	}
}
