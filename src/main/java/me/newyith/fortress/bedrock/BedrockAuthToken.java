package me.newyith.fortress.bedrock;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.UUID;

@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
public class BedrockAuthToken {
	private final UUID uuid = UUID.randomUUID();

//	@Override
//	public boolean equals(Object o) {
//		if (o == this) {
//			return true;
//		}
//
//		if (o instanceof BedrockAuthToken) {
//			BedrockAuthToken otherAuthToken = (BedrockAuthToken)o;
//			return this.uuid.equals(otherAuthToken.uuid);
//		} else {
//			return false;
//		}
//	}
}
