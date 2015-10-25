package me.newyith.fortress.main;

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
}
