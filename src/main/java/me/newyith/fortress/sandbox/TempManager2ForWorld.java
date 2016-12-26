package me.newyith.fortress.sandbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.newyith.fortress.bedrock.BedrockAuthToken;
import me.newyith.fortress.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class TempManager2ForWorld {
	private static class Model {
		private BedrockAuthToken bedrockAuthToken;
		private final String worldName;
		private final transient World world;

		@JsonCreator
		public Model(@JsonProperty("bedrockAuthToken") BedrockAuthToken bedrockAuthToken,
					 @JsonProperty("worldName") String worldName) {
			this.bedrockAuthToken = bedrockAuthToken;
			this.worldName = worldName;

			//rebuild transient fields
			this.world = Bukkit.getWorld(worldName);
		}
	}
	private Model model = null;

	@JsonCreator
	public TempManager2ForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TempManager2ForWorld(World world) {
		model = new Model(new BedrockAuthToken(), world.getName());
	}

	//-----------------------------------------------------------------------

	public void init(BedrockAuthToken bedrockAuthToken) {
		model.bedrockAuthToken = bedrockAuthToken;
		Debug.msg("init 2 for world " + model.world.getName());
	}
}
