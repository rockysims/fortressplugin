package me.newyith;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import java.util.ArrayList;

public class FortressGeneratorRune implements Memorable {
    private FortressGeneratorRunePattern pattern = null; //set by constructor
	private boolean powered = false;

	public void saveTo(Memory m) {
		m.save("pattern", pattern);
		m.save("powered", powered);
	}

	public static FortressGeneratorRune loadFrom(Memory m) {
		FortressGeneratorRunePattern pattern = m.loadFortressGeneratorRunePattern("pattern");
		boolean powered = m.loadBoolean("powered");
		return new FortressGeneratorRune(pattern, powered);
	}

	private FortressGeneratorRune(FortressGeneratorRunePattern runePattern, boolean powered) {
		this.pattern = runePattern;
		this.powered = powered;
	}

	//------------------------------------------------------------------------------------------------------------------

	public FortressGeneratorRune(FortressGeneratorRunePattern runePattern) {
		this.pattern = runePattern;
	}

	public FortressGeneratorRunePattern getPattern() {
		return this.pattern;
	}

	public void onCreated() {
		this.setSignText("created", "", "");

		this.moveBlockTo(Material.GOLD_BLOCK, pattern.runningPoint);
		this.moveBlockTo(Material.DIAMOND_BLOCK, pattern.anchorPoint);
	}

    public void onBroken() {
		this.setSignText("broken", "", "");

		this.moveBlockTo(Material.GOLD_BLOCK, pattern.anchorPoint);
		this.moveBlockTo(Material.DIAMOND_BLOCK, pattern.runningPoint);
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

	public void setPowered(boolean powered) {
		this.powered = powered;
		this.setSignText("powered:", "" + powered, "");

		if (powered) {
			this.moveBlockTo(Material.GOLD_BLOCK, pattern.pausePoint);
		} else {
			this.moveBlockTo(Material.GOLD_BLOCK, pattern.runningPoint);
		}
	}

	public boolean isPowered() {
		return this.powered;
	}

	public boolean setSignText(String line1, String line2, String line3) {
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

	private void swapBlocks(Point a, Point b) {
		Material aMat = a.getBlock().getType();
		Material bMat = b.getBlock().getType();
		a.getBlock().setType(bMat);
		b.getBlock().setType(aMat);
	}
}
