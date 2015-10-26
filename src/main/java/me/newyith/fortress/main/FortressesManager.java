package me.newyith.fortress.main;

import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.util.BaseModel;
import me.newyith.fortress.util.ModelableSet;
import me.newyith.fortress.util.Point;

import java.util.HashSet;
import java.util.Set;

public class FortressesManager { //implements Modelable statically
	public static class Model extends BaseModel {
		public boolean temp = true; //TODO: delete this line (once model contains other stuff)

		public Model() {

		}
	}
	private static Model model;





	private static Set<GeneratorRune> generatorRunes; //TODO: figure out how to add this to model
	private static ModelableSet<GeneratorRune> generatorRunes;






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
		return null;
	}

	public static Set<GeneratorRune> getGeneratorRunesInRange(Point center, int range) {
		Set<GeneratorRune> runesInRange = new HashSet<>();
		int x = center.xInt();
		int y = center.yInt();
		int z = center.zInt();

		//fill runesInRange
		for (GeneratorRune rune : generatorRunes) {


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
}
