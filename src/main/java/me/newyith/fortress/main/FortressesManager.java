package me.newyith.fortress.main;

import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.util.Point;

import java.util.HashSet;
import java.util.Set;

public class FortressesManager {
	public static class Model {
		public boolean temp = true; //TODO: delete this line (once model contains other stuff)

		public Model() {

		}
	}
	private static Model model;

	public static void setModel(Model m) {
		model = m;
	}

	public static Model getModel() {
		if (model == null) {
			model = new Model();
		}
		return model;
	}

	//-----------------------------------------------------------------------

	public static void onTick() {

	}

	public static Set<GeneratorRune> getOtherGeneratorRunesNear(GeneratorRune centerRune) {
		//TODO: return generator runes where cuboid overlaps with centerRune (+2 blocks)
	}

	public static Set<GeneratorRune> getOtherGeneratorRunesInRange(Point center) {
		//TODO: update this to use fortress cuboid instead of fixed range

		Set<GeneratorRune> runesInRange = new HashSet<>();
		int x = center.xInt();
		int y = center.yInt();
		int z = center.zInt();

		//fill runesInRange
		for (GeneratorRune rune : runeInstances) {


			//set inRange
			boolean inRange = true;
			Point p = rune.getPattern().anchorPoint;
			inRange = inRange && Math.abs(p.xInt() - x) <= range;
			inRange = inRange && Math.abs(p.yInt() - y) <= range;
			inRange = inRange && Math.abs(p.zInt() - z) <= range;

			if (inRange) {
				runesInRange.add(rune);
			}
		}

		runesInRange.remove(getRune(center));
		return runesInRange;
	}

	FortressesManager.getOtherGeneratorRunesInRange(startPoint, FortressPlugin.config_generationRangeLimit);
}
