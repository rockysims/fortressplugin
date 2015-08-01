package me.newyith.generator;

import me.newyith.event.TickTimer;
import me.newyith.memory.Memorable;
import me.newyith.memory.Memory;
import me.newyith.util.Debug;
import me.newyith.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class FortressGeneratorRune implements Memorable {
    private FortressGeneratorRunePattern pattern = null; //set by constructor
	private boolean powered = false;
	private int fuelTicksRemaining = 0;
	private FgState state = FgState.PAUSED;
	private int msPerFuelItem = 15*1000;

	enum FgState {
		RUNNING,
		PAUSED,
		NEEDS_FUEL;

		public static FgState fromInt(int ordinal) {
			FgState fgState = FgState.PAUSED;

			if (FgState.RUNNING.ordinal() == ordinal) {
				fgState = FgState.RUNNING;
			} else if (FgState.PAUSED.ordinal() == ordinal) {
				fgState = FgState.PAUSED;
			} else if (FgState.NEEDS_FUEL.ordinal() == ordinal) {
				fgState = FgState.NEEDS_FUEL;
			} else {
				Debug.msg("ERROR: FgState.fromInt(" + ordinal + ") did not match any state.");
			}

			return fgState;
		}
	}

	public void saveTo(Memory m) {
		m.save("pattern", pattern);
		m.save("powered", powered);
		m.save("fuelTicksRemaining", fuelTicksRemaining);
		m.save("state", state.ordinal());
	}

	public static FortressGeneratorRune loadFrom(Memory m) {
		FortressGeneratorRunePattern pattern = m.loadFortressGeneratorRunePattern("pattern");
		boolean powered = m.loadBoolean("powered");
		int fuelTicksRemaining= m.loadInt("fuelTicksRemaining");
		FgState fgState = FgState.fromInt(m.loadInt("state"));
		return new FortressGeneratorRune(pattern, powered, fuelTicksRemaining, fgState);
	}

	private FortressGeneratorRune(FortressGeneratorRunePattern runePattern, boolean powered, int fuelTicksRemaining, FgState state) {
		this.pattern = runePattern;
		this.powered = powered;
		this.fuelTicksRemaining = fuelTicksRemaining;
		this.state = state;
	}

	//------------------------------------------------------------------------------------------------------------------

	public FortressGeneratorRune(FortressGeneratorRunePattern runePattern) {
		this.pattern = runePattern;
	}

	// - Getters -

	public FortressGeneratorRunePattern getPattern() {
		return this.pattern;
	}

	private boolean isRunning() {
		return this.state == FgState.RUNNING;
	}

	private boolean isPaused() {
		return this.state == FgState.PAUSED;
	}

	private boolean isPowered() {
		return this.powered;
	}

	// - Events -

	public void onTick() {
		tickFuel();
	}

	public void onCreated() {
		this.moveBlockTo(Material.GOLD_BLOCK, pattern.runningPoint);
		this.moveBlockTo(Material.DIAMOND_BLOCK, pattern.anchorPoint);
		this.updateState();
	}

	public void onBroken() {
		this.moveBlockTo(Material.DIAMOND_BLOCK, pattern.runningPoint);
		this.moveBlockTo(Material.GOLD_BLOCK, pattern.anchorPoint);
		this.setSignText("Broken", "", "");
	}

	public void setPowered(boolean powered) {
		if (this.powered != powered) {
			this.powered = powered;
			this.updateState();
		}
	}

	// - Handlers -

	private void tickFuel() {
		if (fuelTicksRemaining > 0 && isRunning()) {
			fuelTicksRemaining--;
			updateFuelRemainingDisplay(fuelTicksRemaining * TickTimer.msPerTick);
		}

		if (fuelTicksRemaining <= 0 && !isPaused()) {
			tryReplenishFuel();
			this.updateState();
		}
	}
	private void tryReplenishFuel() {
		Point chestPoint = this.pattern.chestPoint;
		if (chestPoint != null) {
			Block chestBlock = chestPoint.getBlock();

			if (chestBlock.getState() instanceof Chest) {
				Chest chest = (Chest)chestBlock.getState();

				Inventory inv = chest.getInventory();
				if (inv.contains(Material.GLOWSTONE_DUST)) {


					inv.removeItem(new ItemStack(Material.GLOWSTONE_DUST, 1));
					chest.update(true);


					fuelTicksRemaining = msPerFuelItem / TickTimer.msPerTick;
					updateFuelRemainingDisplay(fuelTicksRemaining * TickTimer.msPerTick);
				}
			}
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
			}

			this.state = state;
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
			if (this.isPowered()) {
				this.setState(FgState.PAUSED);
			} else {
				this.setState(FgState.NEEDS_FUEL);
			}
		}
	}

	// - Utils -

	private void updateFuelRemainingDisplay(long ms) {
		//TODO: ms += msPerFuelItem * glowstoneDustInChest;

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
		str += s + "s";
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
		ArrayList<Point> points = new ArrayList<Point>();
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
}
