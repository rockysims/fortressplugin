package me.newyith.fortress.generator;

import me.newyith.fortress.util.Point;

import java.util.List;
import java.util.Set;

public class GenPrepData {
	public List<Set<Point>> generatableLayers;
	public Set<Point> layerAroundWall;
	public Set<Point> pointsInside;
	public Set<Point> layerOutside;

	public GenPrepData(List<Set<Point>> generatableLayers, Set<Point> layerAroundWall, Set<Point> pointsInside, Set<Point> layerOutside) {
		this.generatableLayers = generatableLayers;
		this.layerAroundWall = layerAroundWall;
		this.pointsInside = pointsInside;
		this.layerOutside = layerOutside;
	}
}
