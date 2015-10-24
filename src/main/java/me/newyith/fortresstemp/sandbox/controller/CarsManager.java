package me.newyith.fortresstemp.sandbox.controller;

import me.newyith.fortresstemp.sandbox.model.CarsManagerModel;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CarsManager {
	private CarsManagerModel model;
	private Set<Car> cars = new HashSet<>();

	public CarsManager(CarsManagerModel model) {
		this.model = model;
		this.cars = model.cars.stream().map(Car::new).collect(Collectors.toSet());
	}
}
