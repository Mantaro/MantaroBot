package net.kodehawa.mantarobot.cmd;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.StringArrayUtils;

public class Action extends Module {

	private CopyOnWriteArrayList<String> pats = new CopyOnWriteArrayList<>();
	private CopyOnWriteArrayList<String> bleach = new CopyOnWriteArrayList<>();
	private CopyOnWriteArrayList<String> hugs = new CopyOnWriteArrayList<>();
	static CopyOnWriteArrayList<String> greeting = new CopyOnWriteArrayList<>();
	static CopyOnWriteArrayList<String> tsunLines = new CopyOnWriteArrayList<>();
	/**
	 * Action module.
	 * One of the biggest modules by command quantity.
	 */
	public Action()
	{
		super.setCategory(Category.ACTION);
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
	public void registerCommands(){
		super.register("pat", "Pats a user", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				Random rand = new Random();
				List<User> mentions = receivedMessage.getMentionedUsers();
				StringBuilder pString = new StringBuilder();
				int patint = rand.nextInt(pats.size());
				for(User s : mentions){
					pString.append(s.getAsMention());
				}
				String pat = String.format(":speech_balloon: %s you have been patted by %s \n %s", pString, author.getAsMention(), pats.get(patint));
				channel.sendMessage(pat).queue();
			}

			@Override
			public String help() {
				return 	"~>pat [@user]: Pats the specified user.\n"
						+ "Parameter explanation:\n"
						+ "[@user]: A user to mention.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("hug", "Hugs an user", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				Random rand = new Random();
				List<User> hugMentions = receivedMessage.getMentionedUsers();
				StringBuilder hString = new StringBuilder();
				int hugint = rand.nextInt(hugs.size());

				for(User s : hugMentions){
					hString.append(s.getAsMention());
				}

				String hug = String.format(":speech_balloon: %s you have been hugged by %s \n %s", hString, author.getAsMention(), hugs.get(hugint));
				channel.sendMessage(hug).queue();
			}

			@Override
			public String help() {
				return 	"~>hug [@user]: Self explanatory.\n"
						+ "Parameter explanation:\n"
						+ "[@user]: A user to mention.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("bloodsuck", "Sucks the blood of an user", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				if(event.getMessage().getMentionedUsers().isEmpty()){
					channel.sendMessage("http://puu.sh/qEYYH/e5094405a5.jpg").queue();
				} else{
					StringBuilder listString = new StringBuilder();

					for(User s : event.getMessage().getMentionedUsers())	{
						listString.append(s.getAsMention());
					}

					String bs = String.format(":speech_balloon: http://puu.sh/qEYYH/e5094405a5.jpg \nSucks the blood of %s", listString);
					channel.sendMessage(bs).queue();
				}
			}

			@Override
			public String help() {
				return 	"~>bloodsuck [@user]: Self explanatory.\n"
						+ "Parameter explanation:\n"
						+ "[@user]: A user to mention.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("meow", "Meows at someone, or just meows.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				if(!receivedMessage.getMentionedUsers().isEmpty()){
					List<User> mentions = receivedMessage.getMentionedUsers();
					StringBuilder builder = new StringBuilder();
					for (User user: mentions) {
						builder.append(user.getName());
					}
					String mention = builder.toString().replace("MantaroBot", "");
					channel.sendMessage(":speech_balloon: " + "*meows at " + mention + ".*\n" + "http://puu.sh/rK5Nf/63d90628c2.gif").queue();
				} else{
					channel.sendMessage(":speech_balloon: " + "Meeeeow.\n " + "http://puu.sh/rK5K7/034039286e.gif").queue();
				}
			}

			@Override
			public String help() {
				return "~>meow [@user]: Meows or meows at someone.\n"
						+ "Parameter explanation:\n"
						+ "[@user]: (Optional) A user to mention.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("greet", "Greets someone.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				Random rd = new Random();
				int greetRandomizer = rd.nextInt(greeting.size());
				channel = event.getChannel();
				channel.sendMessage(":speech_balloon: " + greeting.get(greetRandomizer)).queue();
			}

			@Override
			public String help() {
				return "~>greet: Sends a random greeting message.\n";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("tsundere", "Y-You baaaaka!.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				Random rd = new Random();
				channel = event.getChannel();
				int tsundereRandomizer = rd.nextInt(tsunLines.size());
				channel.sendMessage(":mega: " +  tsunLines.get(tsundereRandomizer)).queue();
			}

			@Override
			public String help() {
				return "~>greet: Sends a random greeting message.\n";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});

		super.register("action", "Misc actions.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				String noArgs = content.split(" ")[0];
				channel = event.getChannel();
				Random rd = new Random();
				switch(noArgs){
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
			public String help() {
				return "Commands that involve an action to you or to a specified user.\n"
						+ "Usage:\n"
						+ "~>action bleach: Random image of someone drinking bleach.\n"
						+ "~>action facedesk: Facedesks.\n"
						+ "~>action nom: nom nom.\n";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}
}