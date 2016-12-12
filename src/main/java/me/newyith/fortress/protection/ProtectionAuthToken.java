package me.newyith.fortress.protection;

import java.util.UUID;

public class ProtectionAuthToken {
	private final UUID uuid = UUID.randomUUID();

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}

		if (o instanceof ProtectionAuthToken) {
			ProtectionAuthToken otherAuthToken = (ProtectionAuthToken)o;
			return this.uuid.equals(otherAuthToken.uuid);
		} else {
			return false;
		}
	}
}
