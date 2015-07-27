package me.newyith;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
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
		this.pattern.setSignText("created");
	}

    public void onBroken() {
		this.pattern.setSignText("broken");
    }

	public void setPowered(boolean powered) {
		this.powered = powered;
		this.pattern.setSignText("powered:", "" + powered);
	}

	public boolean isPowered() {
		return this.powered;
	}
}
