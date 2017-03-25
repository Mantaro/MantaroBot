package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.TextChannelGround;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Random;

public class FunCmds extends Module {

	public FunCmds() {
		super(Category.FUN);
		dice();
	}

	private void dice() {
		super.register("dice", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				int roll;
				try {
					roll = Integer.parseInt(args[0]);
				} catch (Exception e) {
					roll = 1;
				}
				if (roll >= 100) roll = 100;
				event.getChannel().sendMessage(EmoteReference.DICE + "You scored **" + diceRoll(roll, event) + "** with a total of **" + roll
					+ "** repetitions.").queue();
				TextChannelGround.of(event).dropItemWithChance(6, 5);
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Dice command")
					.setDescription("Rolls a 6-sided dice with a defined number of repetitions")
					.build();
			}

		});
	}

	private synchronized int diceRoll(int repetitions, GuildMessageReceivedEvent event) {
		int num = 0;
		int roll;
		for (int i = 0; i < repetitions; i++) {
			roll = new Random().nextInt(6) + 1;
			num = num + roll;
		}
		return num;
	}

}
