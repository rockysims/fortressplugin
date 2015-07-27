package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class FortressGeneratorRune {
    private FortressGeneratorRunePattern pattern;
	private boolean powered = false;

    public FortressGeneratorRune(FortressGeneratorRunePattern runePattern) {
        this.pattern = runePattern;
    }

	public FortressGeneratorRunePattern getPattern() {
		return  this.pattern;
	}

	public void onCreated() {
		this.setSignText("created", "", "");
		//TODO: make this more robust. maybe this.setState("running") or something
		this.swapBlocks(pattern.anchorPoint, pattern.runningPoint);
	}

    public void onBroken() {
		this.setSignText("broken", "", "");
    }

	public void setPowered(boolean powered) {
		this.powered = powered;
		this.setSignText("powered:", "" + powered, "");

		//TODO: make this more robust. maybe this.setState("paused") or something
		if (powered) {
			this.swapBlocks(pattern.pausePoint, pattern.runningPoint);
		} else {
			this.swapBlocks(pattern.runningPoint, pattern.pausePoint);
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
