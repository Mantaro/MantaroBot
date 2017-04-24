package net.kodehawa.mantarobot.data.entities.helpers;

import lombok.Data;

import java.beans.Transient;

@Data
public class PlayerData {
	public long experience = 0;
	private String marriedWith = null;
	private Long marriedSince = null;
	private String description = null;

	@Transient
	public boolean isMarried() {
		return marriedWith != null;
	}
}