package me.newyith.fortress.protection;

import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.bedrock.BedrockBatch;
import me.newyith.fortress.bedrock.BedrockManagerNew;
import me.newyith.fortress.util.Debug;
import me.newyith.fortress.util.Point;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

//SKIP: maybe move responsibility for bedrock ripple and particles (CoreParticles) here
//	except particles need to appear only on the outside
//	and bedrock ripple should only search points currently generated by generator
//TODO: make sure wall particles works
//TODO: make sure bedrock ripple works

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
		Debug.start("protect()");
		//fill newProtected (points in batch that aren't protected but now will be)
		Set<Point> newProtected = batch.getPoints().parallelStream()
				.filter(p -> !isProtected(p))
				.collect(Collectors.toSet());

		model.batches.add(batch);
		model.protectedPoints.addAll(newProtected);
		Debug.end("protect()");

		return newProtected;
	}

	public Set<Point> unprotect(ProtectionBatch batch) {
		Debug.start("unprotect()");
		model.batches.remove(batch);

		Set<Point> allBatchPoints = buildBatchPoints();
		Set<Point> shouldBeProtected = batch.getPoints().parallelStream()
				.filter(allBatchPoints::contains)
				.collect(Collectors.toSet());

		//fill newUnprotected (points in batch that are protected but now won't be)
		Set<Point> newUnprotected = batch.getPoints().parallelStream()
				.filter(p -> !shouldBeProtected.contains(p))
				.collect(Collectors.toSet());

		model.protectedPoints.removeAll(newUnprotected);
		Debug.end("unprotect()");

		return newUnprotected;
	}

	public void unprotect(ProtectionAuthToken authToken) {
		Debug.start("unprotect(authToken)");
		model.batches.stream()
				.filter(batch -> batch.authorizedBy(authToken))
				.forEach(this::unprotect);
		Debug.end("unprotect(authToken)");
	}

	public boolean isProtected(Point p) {
		return model.protectedPoints.contains(p);
	}

	public Set<Point> buildProtectedPointsByAuthToken(ProtectionAuthToken authToken) {
		Debug.start("buildProtectedPointsByAuthToken()");
		Set<Point> pointsProtectedByAuthToken = model.batches.parallelStream()
				.filter(batch -> batch.authorizedBy(authToken))
				.map(ProtectionBatch::getPoints)
				.reduce(new HashSet<>(), (carry, batchPoints) -> {
					carry.addAll(batchPoints);
					return carry;
				});
		Debug.end("buildProtectedPointsByAuthToken()");

		return pointsProtectedByAuthToken;
	}

	private Set<Point> buildBatchPoints() {
		Debug.start("buildBatchPoints()");
		Set<Point> allBatchPoints = model.batches.parallelStream()
				.map(ProtectionBatch::getPoints)
				.reduce(new HashSet<>(), (carry, batchPoints) -> {
					carry.addAll(batchPoints);
					return carry;
				});
		Debug.end("buildBatchPoints()");

		return allBatchPoints;
	}
}
