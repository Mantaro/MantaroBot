package net.kodehawa.mantarobot.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Misc extends Command {

	private List<String> lyrics = new ArrayList<String>();
	ArrayList<User> users = new ArrayList<>();
	
	public Misc()
	{
		setName("misc");
		setCommandType("user");
		setDescription("Miscellaneous funny commands. Possible arguments: rob (mention)/lottery/reverse (sentence)/brainpower");
		lyrics.add(":mega: Are you ready?");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("JO-oooooooooooo AAE-O-A-A-U-U-A-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA");
		lyrics.add("O-oooooooooo AAAAE-A-A-I-A-U-");
		lyrics.add("E-eee-ee-eee AAAAE-A-E-I-E-A-");
		lyrics.add("JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA-");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		List<User> mentions = evt.getMessage().getMentionedUsers();
        guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
		
        StringBuilder mentioned = new StringBuilder();
        
        for (User user: mentions){
            mentioned.append(user.getName());
            break;
        }
        
        String noArgs = beheadedMessage.split(" ")[0];
		switch(noArgs){
		case "rob":
			Random r = new Random();
			int woah = r.nextInt(1200);
			channel.sendMessage(":speech_balloon: " + "You robbed **" + woah + "USD** from " + mentioned).queue();
			break;
		case "lottery":
			User user = author;
			if(!users.contains(user)){
				Random r1 = new Random();
				int lottery = r1.nextInt(5000);
				channel.sendMessage(":speech_balloon: " + "You won **" + lottery + "USD**, congrats!").queue();
				users.add(user);
			} else{
				channel.sendMessage(":speech_balloon: " + "Try again in later! (10 minutes since you ran the command)").queue();
			}

			if(users.contains(user)){
				TimerTask timerTask = new TimerTask(){ 
			         public void run(){ 
			        	 users.remove(user);
						 this.cancel();
					} 
			     }; 
				 Timer timer = new Timer(); 
			     timer.scheduleAtFixedRate(timerTask, 600000, 1);
			}
			break;
		case "reverse":
			String stringToReverse = beheadedMessage.replace("reverse ", "");
			String reversed = new StringBuilder(stringToReverse).reverse().toString();
			channel.sendMessage(reversed).queue();
			break;
		case "bp":
			StringBuilder finalMessage = new StringBuilder();
			for (String help : lyrics){
				finalMessage.append(help+"\r\n");
			}
			channel.sendMessage(finalMessage.toString()).queue();
			break;
		case "rndcolor":
			String s = String.format(":speech_balloon: Your random color is %s", randomColor());
			channel.sendMessage(s).queue();
			break;
		}
	}
	
	/**
	 * @return a random hex color.
	 */
	private String randomColor(){
		String[] letters = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
	    String color = "#";
	    for (int i = 0; i < 6; i++ ) {
	        color += letters[(int) Math.floor(Math.random() * 16)];
	    }
	    return color;
	}
}
