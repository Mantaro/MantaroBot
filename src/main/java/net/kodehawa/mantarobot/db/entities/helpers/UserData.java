package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;

@Data
public class UserData {
	private String birthday;
	private String timezone;
	private int reminderN;
	private String premiumKey;
	private boolean hasReceivedFirstKey; //Placeholder here for rethonk plz
}
