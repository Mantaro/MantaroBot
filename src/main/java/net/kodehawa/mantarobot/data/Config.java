package net.kodehawa.mantarobot.data;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.List;

public class Config {

	public String alsecret = null;
	public String carbonToken = null;
	public String dbotsToken = null;
	public String dbotsorgToken = null;
	public String osuApiKey = null;
	public List<String> owners = new ArrayList<>();
	public String token = null;
	public String weatherAppId = null;

	public boolean isOwner(Member member) {
		return isOwner(member.getUser());
	}

	private boolean isOwner(User user) {
		return isOwner(user.getId());
	}

	private boolean isOwner(String id) {
		return owners.contains(id);
	}
}
