package me.newyith.fortress.disruption;

import com.google.common.collect.ImmutableSet;
import me.newyith.fortress.util.AuthToken;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.World;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DisruptionManagerForWorld {
	private static class Model {
		private final Set<DisruptionBatch> batches;
		private final Set<Point> disruptedPoints;
		@SuppressWarnings("unused")
		private final String worldName;
		@SuppressWarnings("unused")
		private final transient World world;

		@JsonCreator
		public Model(@JsonProperty("batches") Set<DisruptionBatch> batches,
					 @JsonProperty("disruptedPoints") Set<Point> disruptedPoints,
					 @JsonProperty("worldName") String worldName) {
			this.batches = batches;
			this.disruptedPoints = disruptedPoints;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public DisruptionManagerForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public DisruptionManagerForWorld(World world) {
		model = new Model(new HashSet<>(), new HashSet<>(), world.getName());
	}

	//-----------------------------------------------------------------------

	public Set<Point> disrupt(DisruptionBatch batch) {
		//fill newDisrupted (points in batch that aren't disrupted but now will be)
		Set<Point> newDisrupted = batch.getPoints().parallelStream()
				.filter(p -> !isDisrupted(p))
				.collect(Collectors.toSet());

		model.batches.add(batch);
		model.disruptedPoints.addAll(newDisrupted);

		return newDisrupted;
	}

	public Set<Point> undisrupt(DisruptionBatch batch) {
		model.batches.remove(batch);

		Set<Point> allRelatedBatchPoints = buildDisruptedPointsByAuthToken(batch.getAuthToken());
		Set<Point> shouldBeDisrupted = batch.getPoints().parallelStream()
				.filter(allRelatedBatchPoints::contains)
				.collect(Collectors.toSet());

		//fill newUndisrupted (points in batch that are disrupted but now won't be)
		Set<Point> newUndisrupted = batch.getPoints().parallelStream()
				.filter(p -> !shouldBeDisrupted.contains(p))
				.collect(Collectors.toSet());

		model.disruptedPoints.removeAll(newUndisrupted);
		batch.destroy();

		return newUndisrupted;
	}

	public void undisrupt(DisruptionAuthToken authToken) {
		ImmutableSet.copyOf(model.batches).stream() //copy to avoid concurrent modification exception
				.filter(batch -> batch.authorizedBy(authToken))
				.forEach(this::undisrupt);
	}

	public boolean isDisrupted(Point p) {
		return model.disruptedPoints.contains(p);
	}

	private Set<Point> buildDisruptedPointsByAuthToken(AuthToken authToken) {
		Set<Point> pointsDisruptedByAuthToken = model.batches.parallelStream()
				.filter(batch -> batch.authorizedBy(authToken))
				.flatMap(batch -> batch.getPoints().stream())
				.collect(Collectors.toSet());

		return pointsDisruptedByAuthToken;
	}
}
