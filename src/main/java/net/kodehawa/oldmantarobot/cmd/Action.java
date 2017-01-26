package net.kodehawa.oldmantarobot.cmd;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.oldmantarobot.util.StringArrayUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Action extends Module {

	static CopyOnWriteArrayList<String> bleach = new CopyOnWriteArrayList<>();
	static CopyOnWriteArrayList<String> greeting = new CopyOnWriteArrayList<>();
	static CopyOnWriteArrayList<String> hugs = new CopyOnWriteArrayList<>();
	static CopyOnWriteArrayList<String> pats = new CopyOnWriteArrayList<>();
	static CopyOnWriteArrayList<String> tsunLines = new CopyOnWriteArrayList<>();

	/**
	 * Action module.
	 * One of the biggest commands by command quantity.
	 */
	public Action() {
		super(Category.ACTION);
		this.registerCommands();
		new StringArrayUtils("greeting", greeting, false);
		new StringArrayUtils("tsunderelines", tsunLines, false);
		new StringArrayUtils("patting", pats, false);
		new StringArrayUtils("hugs", hugs, false);

		bleach.add("http://puu.sh/qyoDQ/9df29f6b30.jpg");
		bleach.add("http://data.whicdn.com/images/13651431/superthumb.jpg");
		bleach.add("https://i.ytimg.com/vi/IjgPHJTbfK4/maxresdefault.jpg");
		bleach.add("https://media0.giphy.com/media/fN96l0NwjjOGQ/200_s.gif");
		bleach.add("https://www.youtube.com/watch?v=5PIx19ha9MY");
	}

	@Override
	public void registerCommands() {
		super.register("pat", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				Random rand = new Random();
				List<User> mentions = receivedMessage.getMentionedUsers();
				StringBuilder pString = new StringBuilder();
				int patint = rand.nextInt(pats.size());
				for (User s : mentions) {
					pString.append(s.getAsMention());
				}
				String pat = String.format("\uD83D\uDCAC %s you have been patted by %s \n %s", pString, author.getAsMention(), pats.get(patint));
				channel.sendMessage(pat).queue();
			}

			@Override
			public String help() {
				return "~>pat [@user]: Pats the specified user.\n"
					+ "Parameter explanation:\n"
					+ "[@user]: A user to mention.";
			}

		});

		super.register("hug", new SimpleCommand() {
			@Override
			public String help() {
				return "~>hug [@user]: Self explanatory.\n"
					+ "Parameter explanation:\n"
					+ "[@user]: A user to mention.";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				Random rand = new Random();
				List<User> hugMentions = receivedMessage.getMentionedUsers();
				StringBuilder hString = new StringBuilder();
				int hugint = rand.nextInt(hugs.size());

				for (User s : hugMentions) hString.append(s.getAsMention());

				String hug = String.format("\uD83D\uDCAC %s you have been hugged by %s \n %s", hString, author.getAsMention(), hugs.get(hugint));
				channel.sendMessage(hug).queue();
			}



			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("bloodsuck", new SimpleCommand() {
			@Override
			public String help() {
				return "~>bloodsuck [@user]: Self explanatory.\n"
					+ "Parameter explanation:\n"
					+ "[@user]: A user to mention.";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				if (event.getMessage().getMentionedUsers().isEmpty()) {
					channel.sendMessage("http://puu.sh/qEYYH/e5094405a5.jpg").queue();
				} else {
					StringBuilder listString = new StringBuilder();

					for (User s : event.getMessage().getMentionedUsers()) {
						listString.append(s.getAsMention());
					}

					String bs = String.format("\uD83D\uDCAC http://puu.sh/qEYYH/e5094405a5.jpg \nSucks the blood of %s", listString);
					channel.sendMessage(bs).queue();
				}
			}



			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("meow", new SimpleCommand() {
			@Override
			public String help() {
				return "~>meow [@user]: Meows or meows at someone.\n"
					+ "Parameter explanation:\n"
					+ "[@user]: (Optional) A user to mention.";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				if (!receivedMessage.getMentionedUsers().isEmpty()) {
					List<User> mentions = receivedMessage.getMentionedUsers();
					StringBuilder builder = new StringBuilder();
					for (User user : mentions) {
						builder.append(user.getName());
					}
					String mention = builder.toString().replace("MantaroBot", "");
					channel.sendMessage("\uD83D\uDCAC " + "*meows at " + mention + ".*\n" + "http://puu.sh/rK5Nf/63d90628c2.gif").queue();
				} else {
					channel.sendMessage("\uD83D\uDCAC " + "Meeeeow.\n " + "http://puu.sh/rK5K7/034039286e.gif").queue();
				}
			}



			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("greet", new SimpleCommand() {
			@Override
			public String help() {
				return "~>greet: Sends a random greeting message.\n";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				Random rd = new Random();
				int greetRandomizer = rd.nextInt(greeting.size());
				event.getChannel().sendMessage("\uD83D\uDCAC " + greeting.get(greetRandomizer)).queue();
			}



			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("tsundere", new SimpleCommand() {
			@Override
			public String help() {
				return "~>greet: Sends a random greeting message.\n";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				event.getChannel().sendMessage("\uD83D\uDCE3 " + tsunLines.get(new Random().nextInt(tsunLines.size()))).queue();
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("action", new SimpleCommand() {
			@Override
			public String help() {
				return "Commands that involve an action to you or to a specified user.\n"
					+ "Usage:\n"
					+ "~>action bleach: Random image of someone drinking bleach.\n"
					+ "~>action facedesk: Facedesks.\n"
					+ "~>action nom: nom nom.\n";
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				String noArgs = content.split(" ")[0];
				TextChannel channel = event.getChannel();
				Random rd = new Random();
				switch (noArgs) {
					case "facedesk":
						channel.sendMessage("http://puu.sh/rK6E7/0b745e5544.gif").queue();
						break;
					case "nom":
						channel.sendMessage("http://puu.sh/rK7t2/330182c282.gif").queue();
						break;
					case "bleach":
						int bleachRandomizer = rd.nextInt(bleach.size());
						channel.sendMessage(bleach.get(bleachRandomizer)).queue();
						break;
					default:
						channel.sendMessage(help()).queue();
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}
}