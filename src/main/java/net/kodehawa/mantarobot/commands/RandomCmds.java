package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

public class RandomCmds extends Module {

	public RandomCmds(){
		super(Category.FUN);
		lewd();
		time();
		dice();
	}

	private void lewd(){
		super.register("lewd", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage("Y-You lewdie!\r\nhttp://puu.sh/rzVEe/c8272e7c84.png").queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Lewd")
						.setDescription("Y-You lewdie")
						.build();
			}
		});
	}

	private void time(){
		super.register("time", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					if (content.startsWith("GMT")) {
						event.getChannel().sendMessage("\uD83D\uDCE3 The time is: " + dateGMT(content) + " in " + content).queue();
					} else {
						event.getChannel().sendMessage("\u274C " + "You didn't specify a valid timezone").queue();
					}
				} catch (Exception ignored) {}
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Time")
						.setDescription("Retrieves time in a specific timezone.\n"
								+ "~>time [timezone]. Retrieves the time in the specified timezone.\n"
								+ "**Parameter specification**\n"
								+ "[timezone] A valid timezone between GMT-12 and GMT+14")
						.build();
			}
		});
	}

	private void dice(){
		super.register("dice", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage(":game_die: You scored **" + diceRoll(Integer.parseInt(args[0]), event) + "** with a total of **" + args[0]
					+ "** repetitions.").queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Dice command")
						.setDescription("Rolls a 6-sided dice with a defined number of repetitions")
						.build();
			}
		});
	}

	private int diceRoll(int repetitions, GuildMessageReceivedEvent event)
	{
		int num = 0;
		int roll;
		for(int i = 0; i < repetitions; i++)
		{
			roll = new Random().nextInt(6)+1;
			num = num + roll;
		}
		return num;
	}

	private String dateGMT(String timezone) throws ParseException, NullPointerException {
		SimpleDateFormat dateGMT = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		dateGMT.setTimeZone(TimeZone.getTimeZone(timezone));
		SimpleDateFormat dateLocal = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		Date date1 = dateLocal.parse(dateGMT.format(new Date()));
		return DateFormat.getInstance().format(date1);
	}
}
