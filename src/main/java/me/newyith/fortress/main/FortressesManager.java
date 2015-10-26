package me.newyith.fortress.main;

import me.newyith.fortress.generator.rune.GeneratorRune;
import me.newyith.fortress.util.model.BaseModel;
import me.newyith.fortress.util.model.GeneratorRuneSet;
import me.newyith.fortress.util.Point;

import java.util.HashSet;
import java.util.Set;

public class FortressesManager { //implements Modelable statically
	public static class Model extends BaseModel {
		GeneratorRuneSet.Model generatorRunes = null;

		public Model(GeneratorRuneSet generatorRunes) {
			this.generatorRunes = generatorRunes.getModel();
		}
	}
	private static Model model;
	private static GeneratorRuneSet generatorRunes = new GeneratorRuneSet();

	public static void setModel(Model m) {
		model = m;
		generatorRunes = new GeneratorRuneSet(m.generatorRunes);
	}

	public static Model getModel() {
		if (model == null) {
			model = new Model(generatorRunes);
		}
		return model;
	}

	//-----------------------------------------------------------------------

	public static void onTick() {

	}






	public static Set<GeneratorRune> getGeneratorRunesNear(Point center) {
		//TODO: return generator runes where cuboid overlaps with center
		return null;
	}

	//TODO: finish writing this method
	//	when getting nearby generators during generation, we need all potentially conflicting generators (not just known ones)
	//		so we need this method to get nearby by range
	//	when getting nearby generators during /fort stuck, we need all generators player might be inside of
	//		so also write getGeneratorRunesNear()
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