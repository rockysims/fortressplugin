package me.newyith.fortress.generator.core;

import me.newyith.fortress.generator.rune.GeneratorState;
import me.newyith.fortress.util.Point;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class GeneratorCore extends BaseCore {
	private static class Model {
		private Point anchor = null;

		@JsonCreator
		public Model(@JsonProperty("anchor") Point anchor) {
			this.anchor = anchor;

			//rebuild transient fields
		}
	}
	private Model model = null;

	@JsonCreator
	public GeneratorCore(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public GeneratorCore(Point anchor) {
		model = new Model(anchor);
	}

	//-----------------------------------------------------------------------

	public void onStateChanged(GeneratorState state) {
		//TODO: write
	}

	//method was in FortressGeneratorRune but now belongs here (basically just passed event along to particles manager)
	public void onGeneratedChanged() {
		//TODO: write
		//particles.onGeneratedChanges();
	}
}
