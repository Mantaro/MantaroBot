package net.kodehawa.mantarobot.data;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.List;

public class Config {
	public String alsecret;
	public String bugreportChannel;
	public String carbonToken;
	public String consoleChannel = "266231083341840385";
	public String crossBotHost;
	public int crossBotPort;
	public boolean crossBotServer = false;
	public String dbDb = "mantaro";
	public String dbHost = "localhost";
	public int dbPort = 28015;
	public String dbotsToken;
	public String dbotsorgToken;
	public String osuApiKey;
	public List<String> owners = new ArrayList<>();
	public String prefix = "~>";
	public String remoteNode;
	public String token;
	public String weatherAppId;

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
