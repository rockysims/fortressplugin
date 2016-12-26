package me.newyith.fortress.protection;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.UUID;

//@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
public class ProtectionAuthToken {
	private final UUID uuid = UUID.randomUUID();

//	@Override
//	public boolean equals(Object o) {
//		if (o == this) {
//			return true;
//		}
//
//		if (o instanceof ProtectionAuthToken) {
//			ProtectionAuthToken otherAuthToken = (ProtectionAuthToken)o;
//			return this.uuid.equals(otherAuthToken.uuid);
//		} else {
//			return false;
//		}
//	}
}
