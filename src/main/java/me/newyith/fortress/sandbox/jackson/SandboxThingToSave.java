package me.newyith.fortress.sandbox.jackson;

import me.newyith.fortress.util.Debug;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class SandboxThingToSave {
	private static SandboxThingToSave instance = null;
	public static SandboxThingToSave getInstance() {
		if (instance == null) {
			instance = new SandboxThingToSave();
		}
		return instance;
	}
	public static void setInstance(SandboxThingToSave newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private String datum = "datum";
		private transient Set<String> dataDerivative = null;

		public Model() {
			onLoaded();
		}

		private void onLoaded() {
			//rebuild transient fields
			dataDerivative = new HashSet<>();
			for (int i = 0; i < 3; i++) {
				dataDerivative.add(datum + i);
			}
		}
	}
	private Model model = new Model();

	@JsonProperty("model")
	private void setModel(Model model) {
		this.model = model;
		model.onLoaded();
		Debug.msg("dataDerivative: " + model.dataDerivative.toString());
	}

	//-----------------------------------------------------------------------


}
