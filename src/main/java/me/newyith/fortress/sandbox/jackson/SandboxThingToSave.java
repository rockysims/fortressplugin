package me.newyith.fortress.sandbox.jackson;

import me.newyith.fortress.util.Debug;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

public class SandboxThingToSave {
	private static SandboxThingToSave instance = null;
	public static SandboxThingToSave getInstance() {
		if (instance == null) {
			instance = new SandboxThingToSave("getInstanceDatum");
		}
		return instance;
	}
	public static void setInstance(SandboxThingToSave newInstance) {
		instance = newInstance;
	}

	//-----------------------------------------------------------------------

	private static class Model {
		private String datum = null;
		private transient Set<String> dataDerivative = null;

		@JsonCreator
		public Model(@JsonProperty("datum") String datum) {
			Debug.msg("SandboxThingToSave JsonCreator called with datum: " + datum);
			this.datum = datum;

			//rebuild transient fields
			dataDerivative = new HashSet<>();
			for (int i = 0; i < 3; i++) {
				dataDerivative.add(datum + ":" + i);
			}
		}
	}
	private Model model = null;

	@JsonCreator
	public SandboxThingToSave(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public SandboxThingToSave(String datum) {
		model = new Model(datum);
	}

	//-----------------------------------------------------------------------

	public void setDatum(String datum) {
		model.datum = datum;
	}

	public void print(String s) {
		Debug.msg(s);
		Debug.msg("datum: " + model.datum);
		Debug.msg("dataDerivative: " + String.join(", ", model.dataDerivative));
	}












//	private String datum = "";
//	private transient Set<String> dataDerivative = new HashSet<>();
//
//	@JsonCreator
//	public SandboxStaticThingToSave(@JsonProperty("datum") String datum) {
//		Debug.msg("SandboxThingToSave JsonCreator called with datum: " + datum);
//		this.datum = datum;
//		refreshTransients();
//	}
//
//	//needed because this class is static
//	public SandboxStaticThingToSave() {
//		this.datum = "defaultData";
//	}
//
//	private void refreshTransients() {
//		dataDerivative = new HashSet<>();
//		for (int i = 0; i < 3; i++) {
//			dataDerivative.add(datum + i);
//		}
//	}
//
//	public void setDatum(String datum) {
//		this.datum = datum;
//		refreshTransients();;
//
//
//	public void print(String s) {
//		Debug.msg(s);
//		Debug.msg("datum: " + datum);
//		Debug.msg("dataDerivative: " + String.join(", ", dataDerivative));
//	}
}
