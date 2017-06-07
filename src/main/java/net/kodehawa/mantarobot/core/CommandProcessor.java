package net.kodehawa.mantarobot.core;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.helpers.GuildData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.sql.SQLAction;
import net.kodehawa.mantarobot.utils.sql.SQLDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

public class CommandProcessor {
	public static final CommandRegistry REGISTRY = new CommandRegistry();

	private static final Logger LOGGER = LoggerFactory.getLogger("CommandProcessor");

	public CommandProcessor() {
		try {
			SQLDatabase.getInstance().run((conn) -> {
				try {
					conn.prepareStatement("CREATE TABLE IF NOT EXISTS CMDLOG (" +
						"id int NOT NULL AUTO_INCREMENT," +
						"cmd varchar(15)," +
						"args varchar(2000)," +
						"userid varchar(20)," +
						"channelid varchar(20)," +
						"guildid varchar(20)," +
						"date bigint," +
						"successful int," +
						"PRIMARY KEY (id)" +
						");").executeUpdate();
					conn.prepareStatement("ALTER TABLE CMDLOG AUTO_INCREMENT=1").executeUpdate();
				} catch (SQLException e) {
					SQLAction.getLog().error(null, e);
				}
			}).queue();
		} catch (SQLException e) {
			SQLAction.getLog().error(null, e);
		}

	}

	public void log(String cmd, String args, GuildMessageReceivedEvent event, int successful) {
		try {
			SQLDatabase.getInstance().run((conn) -> {
				try {
					PreparedStatement statement = conn.prepareStatement("INSERT INTO CMDLOG " +
						"(cmd, args, userid, channelid, guildid, date, successful) VALUES(" +
						"?," +
						"?," +
						"?," +
						"?," +
						"?," +
						"?," +
						"?" +
						");");
					statement.setString(1, cmd);
					statement.setString(2, args);
					statement.setString(3, event.getAuthor().getId());
					statement.setString(4, event.getChannel().getId());
					statement.setString(5, event.getGuild().getId());
					statement.setLong(6, System.currentTimeMillis());
					statement.setInt(7, successful);
					statement.execute();
				} catch (SQLException e) {
					SQLAction.getLog().error(null, e);
				}
			}).queue();
		} catch (SQLException e) {
			SQLAction.getLog().error(null, e);
		}
	}

	public boolean run(GuildMessageReceivedEvent event) {
		if (MantaroBot.getLoadState() != LoadState.POSTLOAD) return false;
		if (MantaroData.db().getMantaroData().getBlackListedUsers().contains(event.getAuthor().getId())) return false;
		String rawCmd = event.getMessage().getRawContent();
		String[] prefix = MantaroData.config().get().prefix;
		String customPrefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();

		String usedPrefix = null;
		for(String s : prefix){
			if(rawCmd.startsWith(s)) usedPrefix = s;
		}

		if (usedPrefix != null && rawCmd.startsWith(usedPrefix)) rawCmd = rawCmd.substring(usedPrefix.length());
		else if (customPrefix != null && rawCmd.startsWith(customPrefix)) rawCmd = rawCmd.substring(customPrefix.length());
		else if (usedPrefix == null) return false;

		String[] parts = splitArgs(rawCmd, 2);
		String cmdName = parts[0], content = parts[1];

		if (!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_EMBED_LINKS)) {
			event.getChannel().sendMessage(EmoteReference.STOP + "I require the permission ``Embed Links``. All Commands will be refused until you give me that permission.\n" +
				"http://i.imgur.com/Ydykxcy.gifv Refer to this on instructions on how to give the bot the permissions. " +
				"Also check all the other roles the bot has have that permissions and remember to check channel-specific permissions. Thanks you.").queue();
			return false;
		}

		try {
			REGISTRY.process(event, cmdName, content);
			LOGGER.trace("Command invoked: {}, by {}#{} with timestamp {}", cmdName, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), new Date(System.currentTimeMillis()));
			log(cmdName, content, event, 1);
		} catch (Exception e) {
			log(cmdName, content, event, 0);
			throw e;
		}
		return true;
	}
}