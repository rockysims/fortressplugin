package me.newyith.fortressOrig.sandbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.newyith.fortressOrig.bedrock.BedrockAuthToken;
import me.newyith.fortressOrig.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class TempManagerForWorld {
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
	public TempManagerForWorld(@JsonProperty("model") Model model) {
		this.model = model;
	}

	public TempManagerForWorld(World world) {
		model = new Model(new BedrockAuthToken(), world.getName());
	}

	//-----------------------------------------------------------------------

	public void init(BedrockAuthToken bedrockAuthToken) {
		model.bedrockAuthToken = bedrockAuthToken;
		Debug.msg("init for world " + model.world.getName());
	}
}
