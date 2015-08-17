package me.newyith.generator;

import me.newyith.event.TickTimer;
import me.newyith.memory.Memorable;
import me.newyith.memory.Memory;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FortressGeneratorRune implements Memorable {
    //saved
	private FortressGeneratorRunePattern pattern = null; //set by constructor
	private GeneratorCore core = null; //set by constructor
	private boolean powered = false;
	private int fuelTicksRemaining = 0;
	private FgState state = FgState.NULL;

	//not saved
	private FortressGeneratorParticlesManager particles = null; //set by constructor
	private int msPerFuelItem = 60*60*1000;
	private List<Long> powerToggleTimeStamps = new ArrayList<Long>();

	public void saveTo(Memory m) {
		m.save("pattern", pattern);
		m.save("core", core);
		m.save("powered", powered);
		m.save("fuelTicksRemaining", fuelTicksRemaining);
		m.save("state", state.ordinal());
	}

	public static FortressGeneratorRune loadFrom(Memory m) {
		FortressGeneratorRunePattern pattern = m.loadFortressGeneratorRunePattern("pattern");
		GeneratorCore core = m.loadGeneratorCore("core");
		boolean powered = m.loadBoolean("powered");
		int fuelTicksRemaining= m.loadInt("fuelTicksRemaining");
		FgState fgState = FgState.fromInt(m.loadInt("state"));
		return new FortressGeneratorRune(pattern, core, powered, fuelTicksRemaining, fgState);
	}

	private FortressGeneratorRune(FortressGeneratorRunePattern runePattern, GeneratorCore core, boolean powered, int fuelTicksRemaining, FgState state) {
		this.pattern = runePattern;
		this.core = core;
		this.powered = powered;
		this.fuelTicksRemaining = fuelTicksRemaining;
		this.state = state;
		this.particles = new FortressGeneratorParticlesManager(this);
	}

	//------------------------------------------------------------------------------------------------------------------

	public FortressGeneratorRune(FortressGeneratorRunePattern runePattern) {
		this.pattern = runePattern;
		this.particles = new FortressGeneratorParticlesManager(this);
		this.core = new GeneratorCore(this.pattern.anchorPoint);
	}

	// - Getters -

	public FortressGeneratorRunePattern getPattern() {
		return this.pattern;
	}

	public boolean isRunning() {
		return this.state == FgState.RUNNING;
	}

	private boolean isPaused() {
		return this.state == FgState.PAUSED;
	}

	private boolean isPowered() {
		return this.powered;
	}

	public Set<Point> getPoints() {
		return new HashSet<>(this.getPattern().getPoints());
	}

	public GeneratorCore getGeneratorCore() {
		return this.core;
	}

	public Set<Point> getLayerOutsideFortress() {
		return this.core.getLayerOutsideFortress();
	}

	public Set<Point> getGeneratedPoints() {
		return this.core.getGeneratedPoints();
	}

	// - Events -

	public void onTick() {
		tickFuel();
		this.particles.tick();
		this.core.tick();
	}

	public void onCreated(Player player) {
		this.moveBlockTo(Material.GOLD_BLOCK, pattern.runningPoint);
		this.moveBlockTo(Material.DIAMOND_BLOCK, pattern.anchorPoint);

		//initialize this.powered
		Point wirePoint = this.pattern.wirePoint;
		if (wirePoint != null) {
			this.powered = wirePoint.getBlock().getBlockPower() > 0;
		}

		this.updateState();

		boolean placed = this.core.onPlaced(player);
		if (!placed) {
			this.onCoreBroken();
		}
	}

	public void onBroken() {
		this.moveBlockTo(Material.DIAMOND_BLOCK, pattern.runningPoint);
		this.moveBlockTo(Material.GOLD_BLOCK, pattern.anchorPoint);
		this.setSignText("Broken", "", "");

		this.core.onBroken();
	}

	public void onCoreBroken() {
		FortressGeneratorRunesManager.doBreakRune(this);
	}

	public void setPowered(boolean powered) {
		if (this.powered != powered) {
			if (countRecentPowerToggles() > 10) {
				this.onCoreBroken();
			} else {
				powerToggleTimeStamps.add(System.currentTimeMillis()); //used by countRecentPowerToggles()
				this.powered = powered;
				this.updateState();
			}
		}
	}

	// - Handlers -

	private void tickFuel() {
		if (fuelTicksRemaining > 0 && isRunning()) {
			fuelTicksRemaining--;
		}

		if (fuelTicksRemaining <= 0) {
			tryReplenishFuel();
			this.updateState();
		}

		//always update sign in case amount of fuel in chest has changed
		updateFuelRemainingDisplay(fuelTicksRemaining * TickTimer.msPerTick);
	}
	private void tryReplenishFuel() {
		Chest chest = this.getChest();
		if (chest != null) {
			Inventory inv = chest.getInventory();
			if (inv.contains(Material.GLOWSTONE_DUST)) {
				inv.removeItem(new ItemStack(Material.GLOWSTONE_DUST, 1));
				chest.update(true);

				fuelTicksRemaining = msPerFuelItem / TickTimer.msPerTick;
				updateFuelRemainingDisplay(fuelTicksRemaining * TickTimer.msPerTick);
			}
		}
	}

	private void updateState() {
		if (fuelTicksRemaining == 0) {
			tryReplenishFuel();
		}

		if (fuelTicksRemaining > 0) {
			if (this.isPowered()) {
				this.setState(FgState.PAUSED);
			} else {
				this.setState(FgState.RUNNING);
			}
		} else {
			this.setState(FgState.NEEDS_FUEL);
		}
	}

	private void setState(FgState state) {
		if (this.state != state) {
			switch (state) {
				case RUNNING:
					this.setSignText("Running", "", null);
					this.moveBlockTo(Material.GOLD_BLOCK, this.getPattern().runningPoint);
					break;
				case PAUSED:
					this.setSignText("Paused", "", null);
					this.moveBlockTo(Material.GOLD_BLOCK, this.getPattern().pausePoint);
					break;
				case NEEDS_FUEL:
					this.setSignText("Needs Fuel", "(glowstone dust)", "");
					this.moveBlockTo(Material.GOLD_BLOCK, this.getPattern().fuelPoint);
					break;
				default:
					Debug.error("FortressGeneratorRune setState method couldn't find a case matching FgState: " + state);
			}

			this.state = state;
			if (this.core != null) {
				this.core.onStateChanged(state);
			} else {
				Debug.error("FGRune setState() core == null");
			}
		}
	}

	// - Utils -

	public int countFuelItemsRemaining() {
		int count = 0;
		Chest chest = this.getChest();
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
		Point chestPoint = this.pattern.chestPoint;
		if (chestPoint != null) {
			Block chestBlock = chestPoint.getBlock();

			if (chestBlock.getState() instanceof Chest) {
				Chest chest = (Chest)chestBlock.getState();
				return chest;
			}
		}
		return null;
	}

	private void updateFuelRemainingDisplay(long ms) {
		int glowstoneDustInChest = this.countFuelItemsRemaining();
		ms += msPerFuelItem * glowstoneDustInChest;

		long s = ms / 1000;
		long m = s / 60;
		long h = m / 60;
		long d = h / 24;
		h = h % 24;
		m = m % 60;
		s = s % 60;
		String str = "";
		if (d > 0) {
			str += d + "d ";
		}
		if (h > 0) {
			str += h + "h ";
		}
		if (m > 0) {
			str += m + "m ";
		}
		if (s > 0) {
			str += s + "s";
		}
		this.setSignText(null, null, str);
	}

	private boolean setSignText(String line1, String line2, String line3) {
		Point signPoint = this.pattern.signPoint;
		if (signPoint != null) {
			Block signBlock = signPoint.getBlock();
			if (signBlock != null) {
				Sign sign = (Sign)signBlock.getState();
				if (sign != null) {
					sign.setLine(0, "Fortress:");
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
			}
		}
		return false;
	}

	private void moveBlockTo(Material material, Point targetPoint) {
		Point materialPoint = null;
		ArrayList<Point> points = new ArrayList<>();
		points.add(pattern.anchorPoint);
		points.add(pattern.runningPoint);
		points.add(pattern.pausePoint);
		points.add(pattern.fuelPoint);
		for (Point p : points) {
			if (p.matches(material)) {
				materialPoint = p;
			}
		}

		if (materialPoint != null) {
			this.swapBlocks(materialPoint, targetPoint);
		}
	}

	private void swapBlocks(Point a, Point b) {
		Material aMat = a.getBlock().getType();
		Material bMat = b.getBlock().getType();
		a.getBlock().setType(bMat);
		b.getBlock().setType(aMat);
	}

	private int countRecentPowerToggles() {
		//set count recent power toggles and remove expired stamps
		long now = System.currentTimeMillis();
		int stampLifetimeMs = 5*1000;
		int count = 0;
		for (Iterator<Long> itr = powerToggleTimeStamps.iterator(); itr.hasNext(); ) {
			Long stamp = itr.next();
			if (now - stamp < stampLifetimeMs) {
				count++;
			} else {
				itr.remove();
			}
		}

		return count;
	}
}
