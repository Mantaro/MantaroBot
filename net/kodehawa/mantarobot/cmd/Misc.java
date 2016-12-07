package net.kodehawa.mantarobot.cmd;

import java.lang.management.ManagementFactory;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.StringArrayUtils;
import net.kodehawa.mantarobot.util.Utils;

public class Misc extends Command {

	private List<String> lyrics = new ArrayList<>();
	private CopyOnWriteArrayList<String> nobleQuotes = new CopyOnWriteArrayList<>();
	private CopyOnWriteArrayList<String> facts = new CopyOnWriteArrayList<>();
	private ArrayList<User> users = new ArrayList<>();

	public Misc()
	{
		setName("misc");
		setCommandType("user");
		setDescription("Miscellaneous funny/useful commands. Get more info using ~>help misc");
		setExtendedHelp(
				"Miscellaneous funny/useful commands. Ranges from funny commands and random colors to bot hardware information\n"
				+ "Usage:\n"
				+ "~>misc rob @user: Rob random amount of money from a user.\n"
				+ "~>misc lottery: Get random amounts of money! Only usable every 20m per person.\n"
				+ "~>misc reverse: Reverses any given sentence.\n"
				+ "~>misc bp: Brain power lyrics.\n"
				+ "~>misc randomfact: Random fact.\n"
				+ "~>misc noble: Random Lost Pause quote.\n"
				+ "~>misc rndcolor: Gives you a random hex color.\n"
				+ "~>misc hwinfo: Gives extended information about the bot hardware usage.\n"
				+ "Parameter explanation:\n"
				+ "*@user*: A user to mention.");
		
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
		
		new StringArrayUtils("facts", facts, false);
		new StringArrayUtils("noble", nobleQuotes, false);
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		String mentioned = "";
		try{
			mentioned = evt.getMessage().getMentionedUsers().get(0).getAsMention();
		} catch(IndexOutOfBoundsException e){}
        guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
		Random rand = new Random();
        
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
				finalMessage.append(help).append("\n\n");
			}
			channel.sendMessage(finalMessage.toString()).queue();
			break;
		case "rndcolor":
			String s = String.format(":speech_balloon: Your random color is %s", randomColor());
			channel.sendMessage(s).queue();
			break;
		case "noble":
			int nobleQuote = rand.nextInt(nobleQuotes.size());
	        channel = evt.getChannel();
			channel.sendMessage(":speech_balloon: " + nobleQuotes.get(nobleQuote) + " -Noble").queue();
			break;
		case "randomfact":
			int factrand = rand.nextInt(facts.size());
	        channel = evt.getChannel();
			channel.sendMessage(":speech_balloon: " + facts.get(factrand)).queue();
			break;
		case "hwinfo":
			DecimalFormat df = new DecimalFormat("####0.0000");
	        df.setRoundingMode(RoundingMode.UP);
			long memoryUsage = Runtime.getRuntime().totalMemory()/(1024)/1024;
			int avaliableProcessors = Runtime.getRuntime().availableProcessors();
			double cpuUsage = Double.parseDouble(df.format(Utils.pm.getCpuUsage()));
			channel.sendMessage(
					"Bot server information:\n"
					+ "Threads: " + ManagementFactory.getThreadMXBean().getThreadCount() + "\n"
					+ "Memory Usage: " + String.valueOf(memoryUsage)+"MB" + "\n"
					+ "Avaliable JVM Memory: " + Runtime.getRuntime().freeMemory()/(1024)/1024 +"MB\n"
					+ "CPU Cores: " + String.valueOf(avaliableProcessors)+"\n"
					+ "CPU Usage: " + String.valueOf(cpuUsage +"%")).queue();
		default:
			channel.sendMessage(":heavy_multiplication_x: Incorrect usage. For info on how to use the command do ~>help misc");
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
