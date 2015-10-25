package me.newyith.fortress.util.particle;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;


public class ThreeBitColor {
	private static Map<Short, ThreeBitColor> durabilityToColor = null;

	private boolean r = false;
	private boolean g = false;
	private boolean b = false;

	private ThreeBitColor(int r, int g, int b) {
		this.r = r == 1;
		this.g = g == 1;
		this.b = b == 1;
	}

	public static ThreeBitColor fromDyeItem(ItemStack dyeItemStack) {
		if (durabilityToColor == null) {
			durabilityToColor = new HashMap<>();

			/*
			000 black (ink sack) 0
			001 blue (lapis) 4
			010 green (cactus green dye) 2
			011 teal (cyan dye) 6
			100 red (rose red dye) 1
			101 purple (purple dye) 5
			110 yellow (dandylion yellow dye) 11
			111 white (bone meal) 15
			*/
			durabilityToColor.put((short)0, new ThreeBitColor(0, 0, 0));
			durabilityToColor.put((short)4, new ThreeBitColor(0, 0, 1));
			durabilityToColor.put((short)2, new ThreeBitColor(0, 1, 0));
			durabilityToColor.put((short)6, new ThreeBitColor(0, 1, 1));
			durabilityToColor.put((short)1, new ThreeBitColor(1, 0, 0));
			durabilityToColor.put((short)5, new ThreeBitColor(1, 0, 1));
			durabilityToColor.put((short)11, new ThreeBitColor(1, 1, 0));
			durabilityToColor.put((short)15, new ThreeBitColor(1, 1, 1));
		}

		short durability = dyeItemStack.getDurability();
		if (durabilityToColor.containsKey(durability)) {
			return durabilityToColor.get(durability);
		} else {
			return new ThreeBitColor(0, 0, 0);
		}
	}

	public int r() {
		return r?255:0;
	}

	public int g() {
		return g?255:0;
	}

	public int b() {
		return b?255:0;
	}
}
