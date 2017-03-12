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
	public String bugreportChannel = null;
	public int crossBotPort = 0;
	public String crossBotHost = null;
	public boolean crossBotServer = true;

	public boolean isOwner(Member member) {
		return isOwner(member.getUser());
	}

	public boolean isOwner(User user) {
		return isOwner(user.getId());
	}

	public boolean isOwner(String id) {
		return owners.contains(id);
	}
}
