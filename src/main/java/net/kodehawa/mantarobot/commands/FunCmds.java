package net.kodehawa.mantarobot.commands;

import net.kodehawa.mantarobot.commands.rpg.TextChannelGround;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Commands;
import net.kodehawa.mantarobot.modules.RegisterCommand;
import net.kodehawa.mantarobot.modules.commands.Category;
import net.kodehawa.mantarobot.modules.commands.CommandPermission;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Random;

@RegisterCommand.Class
public class FunCmds {

	@RegisterCommand
	public static void coinflip(CommandRegistry cr) {
		cr.register("coinflip", Commands.newSimple(Category.FUN)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				int times;
				if (args.length == 0 || content.length() == 0) times = 1;
				else {
					try {
						times = Integer.parseInt(args[0]);
						if (times > 1000) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Whoah there! The limit is 1,000 coinflips").queue();
							return;
						}
					} catch (NumberFormatException nfe) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify an Integer for the amount of " +
							"repetitions").queue();
						return;
					}
				}

				final int[] heads = {0};
				final int[] tails = {0};
				thiz.doTimes(times, () -> {
					if (new Random().nextBoolean()) heads[0]++;
					else tails[0]++;
				});
				String flips = times == 1 ? "time" : "times";
				event.getChannel().sendMessage(EmoteReference.PENNY + " Your result from **" + times + "** " + flips + " yielded " +
					"**" + heads[0] + "** heads and **" + tails[0] + "** tails").queue();
			})
			.help((thiz, event) ->
				thiz.helpEmbed(event, "Coinflip command")
					.setDescription("Flips a coin with a defined number of repetitions")
					.build())
			.build());
	}

	@RegisterCommand
	public static void dice(CommandRegistry cr) {
		cr.register("roll", Commands.newSimple(Category.FUN)
			.permission(CommandPermission.USER)
			.code((thiz, event, content, args) -> {
				int roll;
				try {
					roll = Integer.parseInt(args[0]);
				} catch (Exception e) {
					roll = 1;
				}
				if (roll >= 100) roll = 100;
				event.getChannel().sendMessage(EmoteReference.DICE + "You got **" + diceRoll(roll) + "** with a total of **" + roll
					+ "** repetitions.").queue();
				TextChannelGround.of(event).dropItemWithChance(6, 5);
			})
			.help((thiz, event) ->
				thiz.helpEmbed(event, "Dice command")
					.setDescription("Roll a 6-sided dice a specified number of times")
					.build()
			)
			.build()
		);
	}

	private static int diceRoll(int repetitions) { //why the fuck was this shit synchronized? blame kodehawa
		int num = 0;
		int roll;
		for (int i = 0; i < repetitions; i++) {
			roll = new Random().nextInt(6) + 1;
			num = num + roll;
		}
		return num;
	}
}
