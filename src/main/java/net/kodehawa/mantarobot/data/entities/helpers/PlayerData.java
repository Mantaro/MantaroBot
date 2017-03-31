package net.kodehawa.mantarobot.data.entities.helpers;

import lombok.Data;

import java.beans.Transient;

@Data
public class PlayerData {
	public long experience = 0;

	@Transient
	public void incrementExperience(){
		experience = experience++;
	}
}