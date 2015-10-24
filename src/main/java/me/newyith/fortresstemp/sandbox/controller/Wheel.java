package me.newyith.fortresstemp.sandbox.controller;

import me.newyith.fortresstemp.sandbox.model.WheelModel;

public class Wheel {
	private WheelModel model;

	public Wheel(WheelModel model) {
		this.model = model;
	}

	public Wheel(int speed) {
		this.model = new WheelModel();
		this.model.speed = speed;
	}

	public WheelModel getModel() {
		return model;
	}
}
