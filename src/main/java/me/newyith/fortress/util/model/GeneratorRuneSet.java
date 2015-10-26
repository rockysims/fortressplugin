package me.newyith.fortress.util.model;

import me.newyith.fortress.generator.rune.GeneratorRune;

import java.util.HashSet;
import java.util.Set;

public class GeneratorRuneSet { //extends ModelableSet<GeneratorRune> {
	public static class Model extends BaseModel {
		public Set<GeneratorRune.Model> items = new HashSet<>();

		public Model(Set<GeneratorRune> items) {
			items.forEach((item) -> this.items.add(item.getModel()));
		}
	}
	private Model model;
	private Set<GeneratorRune> items = new HashSet<>();

	public GeneratorRuneSet(Model model) {
		this.model = model;
		model.items.forEach((m) -> this.items.add(new GeneratorRune(m)));
	}

	public Model getModel() {
		return this.model;
	}

	//-----------------------------------------------------------------------

	public GeneratorRuneSet() {
		this.model = new Model(items);
	}
}
