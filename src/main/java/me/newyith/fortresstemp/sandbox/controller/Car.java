package me.newyith.fortresstemp.sandbox.controller;

import me.newyith.fortresstemp.sandbox.model.CarModel;

import java.util.List;
import java.util.stream.Collectors;

public class Car {
	private CarModel model;
	private List<Wheel> wheels;

	public Car(CarModel model) {
		this.model = model;
		this.wheels = model.wheels.stream().map(Wheel::new).collect(Collectors.toList());
	}
}
