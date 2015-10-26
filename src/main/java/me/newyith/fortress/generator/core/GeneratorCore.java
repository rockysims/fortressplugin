package me.newyith.fortress.generator.core;

import me.newyith.fortress.util.BaseModel;
import me.newyith.fortress.util.Modelable;
import me.newyith.fortress.util.Point;

public class GeneratorCore implements Modelable {
	public static class Model extends BaseModel {
		Point.Model anchor = null;

		public Model(Point anchor) {
			this.anchor = anchor.getModel();
		}
	}
	private Model model;
	private Point anchor;

	public GeneratorCore(Model model) {
		this.model = model;
		this.anchor = new Point(model.anchor);
	}

	public Model getModel() {
		return this.model;
	}

	//-----------------------------------------------------------------------

	public GeneratorCore(Point anchor) {
		this.model = new Model(anchor);
	}
}
