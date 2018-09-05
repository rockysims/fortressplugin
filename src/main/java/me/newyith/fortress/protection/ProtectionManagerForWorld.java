package me.newyith.fortress.protection;

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

public class ProtectionManagerForWorld {
	private static class Model {
		private final Set<ProtectionBatch> batches;
		private final Set<Point> protectedPoints;
		private final String worldName;
		private final transient World world;

		@JsonCreator
		public Model(@JsonProperty("batches") Set<ProtectionBatch> batches,
					 @JsonProperty("protectedPoints") Set<Point> protectedPoints,
					 @JsonProperty("worldName") String worldName) {
			this.batches = batches;
			this.protectedPoints = protectedPoints;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public ProtectionManagerForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public ProtectionManagerForWorld(World world) {
		model = new Model(new HashSet<>(), new HashSet<>(), world.getName());
	}

	//-----------------------------------------------------------------------

	public Set<Point> protect(ProtectionBatch batch) {
//		Debug.start("protect()");
		//fill newProtected (points in batch that aren't protected but now will be)
		Set<Point> newProtected = batch.getPoints().parallelStream()
				.filter(p -> !isProtected(p))
				.collect(Collectors.toSet());

		model.batches.add(batch);
		model.protectedPoints.addAll(newProtected);
//		Debug.end("protect()");

		return newProtected;
	}

	public Set<Point> unprotect(ProtectionBatch batch) {
//		Debug.start("unprotect()");
		model.batches.remove(batch);

		Set<Point> allRelatedBatchPoints = buildProtectedPointsByAuthToken(batch.getAuthToken());
		Set<Point> shouldBeProtected = batch.getPoints().parallelStream()
				.filter(allRelatedBatchPoints::contains)
				.collect(Collectors.toSet());

		//fill newUnprotected (points in batch that are protected but now won't be)
		Set<Point> newUnprotected = batch.getPoints().parallelStream()
				.filter(p -> !shouldBeProtected.contains(p))
				.collect(Collectors.toSet());

		model.protectedPoints.removeAll(newUnprotected);
		batch.destroy();
//		Debug.end("unprotect()");

		return newUnprotected;
	}

	public void unprotect(ProtectionAuthToken authToken) {
//		Debug.start("unprotect(authToken)");
		ImmutableSet.copyOf(model.batches).stream() //copy to avoid concurrent modification exception
				.filter(batch -> batch.authorizedBy(authToken))
				.forEach(this::unprotect);
//		Debug.end("unprotect(authToken)");
	}

	public boolean isProtected(Point p) {
		return model.protectedPoints.contains(p);
	}

	public Set<Point> buildProtectedPointsByAuthToken(AuthToken authToken) {
//		Debug.start("buildProtectedPointsByAuthToken()");
		Set<Point> pointsProtectedByAuthToken = model.batches.parallelStream()
				.filter(batch -> batch.authorizedBy(authToken))
				.flatMap(batch -> batch.getPoints().stream())
				.collect(Collectors.toSet());
//		Debug.end("buildProtectedPointsByAuthToken()");

//		Debug.msg("pointsProtectedByAuthToken.size(): " + pointsProtectedByAuthToken.size());
//		Debug.msg("model.batches.size(): " + model.batches.size());

		return pointsProtectedByAuthToken;
	}
}
