package me.newyith.fortress.util;

import java.util.UUID;

public class BaseUUID {
	private final UUID uuid = UUID.randomUUID();

	public UUID getUuid() {
		return uuid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BaseUUID baseUUID = (BaseUUID) o;

		return !(uuid != null ? !uuid.equals(baseUUID.uuid) : baseUUID.uuid != null);

	}

	@Override
	public int hashCode() {
		return uuid != null ? uuid.hashCode() : 0;
	}
}
