package net.kodehawa.mantarobot.data;

import lombok.Data;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.List;

@Data
public class Config {
	public String alsecret;
	public String alClient;
	public String bugreportChannel;
	public boolean cacheGames = false;
	public String carbonToken;
	public String cleverbotKey;
	public String cleverbotUser;
	public int connectionWatcherPort = 26000;
	public String consoleChannel = "266231083341840385";
	public String crossBotHost;
	public int crossBotPort;
	public boolean crossBotServer = false;
	public String dbDb = "mantaro";
	public String dbHost = "localhost";
	public int dbPort = 28015;
	public String dbotsToken;
	public String dbotsorgToken;
	public int maxJdaReconnectDelay = 3; //3 seconds
	public String osuApiKey;
	public List<String> owners = new ArrayList<>();
	public String[] prefix = {"~>", "->", "m."};
	public String remoteNode;
	public int shardWatcherTimeout = 1500; //wait 1500ms for the handlers to run
	public int shardWatcherWait = 600000; //run once every 600 seconds (10 minutes)
	public String sqlPassword;
	public String token;
	public String weatherAppId;
	public boolean isPremiumBot = false;

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
