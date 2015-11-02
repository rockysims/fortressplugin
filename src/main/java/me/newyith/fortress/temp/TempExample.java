package me.newyith.fortress.temp;

import me.newyith.fortress.util.Debug;
import org.codehaus.jackson.annotate.JsonProperty;

public class TempExample {
	private static TempExample instance = null;
	public static TempExample getInstance() {
		if (instance == null) {
			instance = new TempExample();
		}
		return instance;
	}
	public static void setInstance(TempExample newInstance) {
		instance = newInstance;
	}

	//--- fields (saved and transient) ---

	private String data = "data";
	private transient String dataDerivative = null;

	@JsonProperty("data")
	private void setData(String data) {
		Debug.msg("TempExample.setData() called via @JsonProperty annotation");
		this.data = data;
		buildTransients();
	}

	private void buildTransients() {
		dataDerivative = data + "Derivative";
	}




	public static void print() {
		Debug.msg("TempExample.print() dataDerivative: " + instance.dataDerivative);
	}
}
