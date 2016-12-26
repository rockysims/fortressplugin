package me.newyith.fortress.util;

import java.util.UUID;

public class AuthToken {
	private final UUID uuid = UUID.randomUUID();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AuthToken that = (AuthToken) o;

		return !(uuid != null ? !uuid.equals(that.uuid) : that.uuid != null);
	}

	@Override
	public int hashCode() {
		return uuid != null ? uuid.hashCode() : 0;
	}
}
