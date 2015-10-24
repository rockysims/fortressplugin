package me.newyith.fortresstemp.sandbox.car;

import me.newyith.fortresstemp.sandbox.Model;
import me.newyith.fortresstemp.sandbox.wheel.WheelModel;

import java.util.HashSet;
import java.util.Set;

public class CarModel extends Model {
	public Set<WheelModel> wheels = new HashSet<>();
	public String name = "";
}
