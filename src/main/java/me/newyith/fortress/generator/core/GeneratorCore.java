package me.newyith.fortress.generator.core;

import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.model.BaseModel;
import me.newyith.fortress.util.model.Modelable;
import me.newyith.fortress.util.Point;

import java.util.Set;

public class GeneratorCore implements Modelable {
	public static class Model extends BaseModel {
		//anchor
		private transient Point anchor = null;
		private Point.Model anchorModel = null;
		public Point getAnchor() {
			if (anchor == null) {
				anchor = new Point(anchorModel);
			}
			return anchor;
		}
		public void setAnchor(Point p) {
			anchor = p;
			anchorModel = anchor.getModel();
		}

		public Model(Point anchor) {
			setAnchor(anchor);
		}
	}
	private Model model;

	public GeneratorCore(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return this.model;
	}

	//-----------------------------------------------------------------------

	public GeneratorCore(Point anchor) {
		this.model = new Model(anchor);
	}

	public Set<Point> getGeneratedPoints() {
		Debug.msg("//TODO: write GeneratorCore.getGeneratedPoints()");
		return null;
	}

}
