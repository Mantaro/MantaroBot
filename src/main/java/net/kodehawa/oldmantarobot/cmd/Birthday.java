package net.kodehawa.oldmantarobot.cmd;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.oldmantarobot.util.HashMapUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Birthday extends Module {

	public static HashMap<String, String> bd = new HashMap<>();
	private String FILE_SIGN = "d41d8cd98f00b204e9800998ecf8427e";

	public Birthday() {
		super(Category.MISC);
		this.registerCommands();
		new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, false);
	}

	@Override
	public void registerCommands() {
		super.register("birthday", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				String userId = event.getMessage().getAuthor().getId();
				SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
				Date bd1 = null;
				//So they don't input something that isn't a date...
				try {
					bd1 = format1.parse(args[0]);
				} catch (Exception e) {
					if (args[0] != null)
						channel.sendMessage(":heavy_multiplication_x:" + args[0] + " is not a valid date or I cannot parse it.").queue();
					e.printStackTrace();
				}

				if (bd1 != null) {
					if (!bd.containsKey(userId)) {
						String finalBirthday = format1.format(bd1);

						bd.put(event.getGuild().getId() + ":" + userId, finalBirthday);
						new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, true);
						channel.sendMessage(":mega: Added birthday date.").queue();
					} else {
						String finalBirthday = format1.format(bd1);

						bd.remove(userId);
						bd.put(event.getGuild().getId() + ":" + userId, finalBirthday);
						new HashMapUtils("mantaro", "bd", bd, FILE_SIGN, true);
						channel.sendMessage(":mega: Changed birthday date.").queue();
					}
				}
			}

			@Override
			public String help() {
				return "Sets your birthday date.\n"
					+ "**Usage:**\n"
					+ "~>birthday [date]. Sets your birthday date. Only useful if the server enabled this functionality"
					+ "**Parameter explanation:**\n"
					+ "[date]. A date in dd-mm-yyyy format (13-02-1998 for example)";
			}
		});
	}
}