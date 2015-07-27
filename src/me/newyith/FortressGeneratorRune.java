package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;

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
		this.setSignText("created", null, null);
	}

    public void onBroken() {
		this.setSignText("broken", null, null);
    }

	public void setPowered(boolean powered) {
		this.powered = powered;
		this.setSignText("powered:", "" + powered, null);
	}

	public boolean isPowered() {
		return this.powered;
	}

	public boolean setSignText(String line1, String line2, String line3) {
		Point signPoint = this.pattern.getSignPoint();
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
}
