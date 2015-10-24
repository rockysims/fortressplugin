package me.newyith.fortresstemp.sandbox.car;

import me.newyith.fortresstemp.sandbox.wheel.Wheel;

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
