package me.newyith.fortress.bedrock;

import java.util.UUID;

public class BedrockAuthToken {
	private final UUID uuid = UUID.randomUUID();

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (o instanceof BedrockAuthToken) {
			BedrockAuthToken otherAuthToken = (BedrockAuthToken)o;
			return this.uuid.equals(otherAuthToken.uuid);
		} else {
			return false;
		}
	}
}
