package me.newyith.fortresstemp.sandbox.model;

import me.newyith.fortresstemp.sandbox.Model;

import java.util.HashSet;
import java.util.Set;

public class CarModel extends Model {
	public Set<WheelModel> wheels = new HashSet<>();
	public String name = "";
}
