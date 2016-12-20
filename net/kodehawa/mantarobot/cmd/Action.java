package net.kodehawa.mantarobot.cmd;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.management.Command;
import net.kodehawa.mantarobot.util.StringArrayUtils;

public class Action extends Command {

	private CopyOnWriteArrayList<String> pats = new CopyOnWriteArrayList<>();
	private CopyOnWriteArrayList<String> bleach = new CopyOnWriteArrayList<>();
	private CopyOnWriteArrayList<String> hugs = new CopyOnWriteArrayList<>();
	public static CopyOnWriteArrayList<String> greeting = new CopyOnWriteArrayList<>();
	public static CopyOnWriteArrayList<String> tsunLines = new CopyOnWriteArrayList<>();
	/**
	 * Action module.
	 * One of the biggest modules by command quantity.
	 */
	public Action()
	{
		setName("action");
		setDescription("Action commands. Check ~>help action");
		setCommandType("user");
		new StringArrayUtils("greeting", greeting, false);
		new StringArrayUtils("tsunderelines", tsunLines, false);
		new StringArrayUtils("patting", pats, false);
		new StringArrayUtils("hugs", hugs, false);

		bleach.add("http://puu.sh/qyoDQ/9df29f6b30.jpg");
		bleach.add("http://data.whicdn.com/images/13651431/superthumb.jpg");
		bleach.add("https://i.ytimg.com/vi/IjgPHJTbfK4/maxresdefault.jpg");
		bleach.add("https://media0.giphy.com/media/fN96l0NwjjOGQ/200_s.gif");
		bleach.add("https://www.youtube.com/watch?v=5PIx19ha9MY");
		
		setExtendedHelp(
				"Commands that involve an action to you or to a specified user.\n"
				+ "Usage:\n"
				+ "~>action greet: Sends a random greeting message.\n"
				+ "~>action tsundere: Y-You baka!\n"
				+ "~>action bleach: Random image of someone drinking bleach.\n"
				+ "~>action pat [@user]: Pats the specified user.\n"
				+ "~>action hug [@user]: Hugs the specified user.\n"
				+ "~>action bloodsuck [@user]: Self explanatory.\n"
				+ "~>action meow [@user]: Meows to a user.\n"
				+ "~>action meow2: Meows.\n"
				+ "~>action facedesk: Facedesks.\n"
				+ "~>action nom: nom nom.\n"
				+ "Parameter explanation:\n"
				+ "[@user]: A user to mention.");
	}
	
	
	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
        guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
        String noArgs = beheadedMessage.split(" ")[0];
		Random rd = new Random();

		Random rand = new Random();
		switch(noArgs){
		case "pat": 
			List<User> menctions = receivedMessage.getMentionedUsers();
			StringBuilder pString = new StringBuilder();
			int patint = rand.nextInt(pats.size());
			for(User s : menctions){
				pString.append(s.getAsMention());
			}
			String pat = String.format(":speech_balloon: %s you have been patted by %s \n %s", pString, author.getAsMention(), pats.get(patint));
			channel.sendMessage(pat).queue();
			break;
		case "hug":
			List<User> hugMentions = receivedMessage.getMentionedUsers();
			StringBuilder hString = new StringBuilder();
			int hugint = rand.nextInt(hugs.size());
			
			for(User s : hugMentions){
				hString.append(s.getAsMention());
			}
			
			String hug = String.format(":speech_balloon: %s you have been hugged by %s \n %s", hString, author.getAsMention(), hugs.get(hugint));
			channel.sendMessage(hug).queue();
			break;
		case "bloodsuck":
			if(evt.getMessage().getMentionedUsers().isEmpty()){
				channel.sendMessage("http://puu.sh/qEYYH/e5094405a5.jpg").queue();
			} else{
				StringBuilder listString = new StringBuilder();

				for(User s : evt.getMessage().getMentionedUsers())	{
					listString.append(s.getAsMention());
				}
				
				String bs = String.format(":speech_balloon: http://puu.sh/qEYYH/e5094405a5.jpg \nSucks the blood of %s", listString);
				channel.sendMessage(bs).queue();
			}
			break;
		case "meow2": 
			channel.sendMessage(":speech_balloon: " + "Meeeeow.\n " + "http://puu.sh/rK5K7/034039286e.gif").queue();
			break;
		case "meow":
			if(!receivedMessage.getMentionedUsers().isEmpty()){
				List<User> mentions = receivedMessage.getMentionedUsers();
	            StringBuilder builder = new StringBuilder();
	            for (User user: mentions) {
	                builder.append(user.getName());
	            }
	            String mention = builder.toString().replace("MantaroBot", "");
				channel.sendMessage(":speech_balloon: " + "*meows at " + mention + ".*\n" + "http://puu.sh/rK5Nf/63d90628c2.gif").queue();
            } else{
				channel.sendMessage(":speech_balloon: " + "Who am I gonna meow at, silly?\n\nAnyway, I guess I'll have to meow you.\n\n*meows at " + evt.getAuthor().getAsMention() + " .*").queue();
            }
			break;
		case "facedesk":
			channel.sendMessage("http://puu.sh/rK6E7/0b745e5544.gif").queue();
			break;
		case "nom":
			channel.sendMessage("http://puu.sh/rK7t2/330182c282.gif").queue();
			break;
		case "greet":
		    int greetRandomizer = rd.nextInt(greeting.size());
	        channel = evt.getChannel();
		    channel.sendMessage(":speech_balloon: " + greeting.get(greetRandomizer)).queue();
		    break;
		case "tsundere":
	        int tsundereRandomizer = rd.nextInt(tsunLines.size());
			channel.sendMessage(":mega: " +  tsunLines.get(tsundereRandomizer)).queue();
			break;
		case "bleach":
	        int bleachRandomizer = rd.nextInt(bleach.size());
	        channel = evt.getChannel();
			channel.sendMessage(bleach.get(bleachRandomizer)).queue();
			break;
		default:
			channel.sendMessage(getExtendedHelp()).queue();
			break;		
		}
	}
}