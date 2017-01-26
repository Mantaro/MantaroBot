package net.kodehawa.oldmantarobot.cmd;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Random extends Module {

	public Random() {
		super(Category.FUN);
		this.registerCommands();
	}

	private String dateGMT(String timezone) throws ParseException, NullPointerException {
		SimpleDateFormat dateGMT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		dateGMT.setTimeZone(TimeZone.getTimeZone(timezone));
		SimpleDateFormat dateLocal = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		Date date1 = dateLocal.parse(dateGMT.format(new Date()));
		return DateFormat.getInstance().format(date1);
	}

	public void registerCommands() {
		super.register("lewd", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage("Y-You lewdie!\r\nhttp://puu.sh/rzVEe/c8272e7c84.png").queue();
			}

			@Override
			public String help() {
				return "Returns a image of a girl holding a paper that says lewd";
			}

		});

		super.register("time", new SimpleCommand() {
			@Override
			public String help() {
				return "Retrieves time in a specific timezone.\n"
					+ "~>time [timezone]. Retrieves the time in the specified timezone.\n"
					+ "**Parameter specification**\n"
					+ "[timezone] A valid timezone between GMT-12 and GMT+14";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();

				try {
					if (content.startsWith("GMT")) {
						channel.sendMessage("\uD83D\uDCE3 The time is: " + dateGMT(content) + " in " + content).queue();
					} else {
						channel.sendMessage("\u274C " + "You didn't specify a valid timezone").queue();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}
}