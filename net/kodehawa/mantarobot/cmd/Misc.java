package net.kodehawa.mantarobot.cmd;

import java.lang.management.ManagementFactory;
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

	private List<String> lyrics = new ArrayList<String>();
	private CopyOnWriteArrayList<String> nobleQuotes = new CopyOnWriteArrayList<String>();
	private CopyOnWriteArrayList<String> facts = new CopyOnWriteArrayList<String>();
	ArrayList<User> users = new ArrayList<>();
	double cpuUsage = Utils.pm.getCpuUsage();

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
		List<User> mentions = evt.getMessage().getMentionedUsers();
        guild = evt.getGuild();
        author = evt.getAuthor();
        channel = evt.getChannel();
        receivedMessage = evt.getMessage();
		Random rand = new Random();

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
				finalMessage.append(help+"\n\n");
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
			long heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()/(1024^2);
			long nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed()/(1024^2);
			int avaliableProcessors = Runtime.getRuntime().availableProcessors();
			cpuUsage = Double.parseDouble(df.format(Utils.pm.getCpuUsage()));
			channel.sendMessage(
					"Bot server infomration:\n"
					+ "Threads: " + ManagementFactory.getThreadMXBean().getThreadCount() + "\n"
					+ "Memory Usage: " + String.valueOf(heapMemoryUsage + nonHeapMemoryUsage)+"MB" + "\n"
					+ "Avaliable JVM Memory: " + Runtime.getRuntime().freeMemory()/(1024^2) +"MB\n"
					+ "CPU Cores: " + String.valueOf(avaliableProcessors)+"\n"
					+ "CPU Usage: " + String.valueOf(cpuUsage+"%")).queue(
							sentMessage ->
							{
								Timer timer = new Timer();
								TimerTask timertask = new TimerTask(){
									int i = 0;
									
									@Override
									public void run(){
										cpuUsage = Double.parseDouble(df.format(Utils.pm.getCpuUsage()));
										if(i <= 5)
											sentMessage.editMessage(
															"Bot server information (Live update every 5 seconds for 25 seconds):\n"
															+ "Threads: " + ManagementFactory.getThreadMXBean().getThreadCount() + "\n"
															+ "Memory Usage: " + String.valueOf(heapMemoryUsage + nonHeapMemoryUsage + "MB" + "\n")
															+ "Avaliable JVM Memory: " + Runtime.getRuntime().freeMemory()/(1024^2) +"MB\n"
															+ "CPU Cores: " + String.valueOf(avaliableProcessors) +"\n"
															+ "CPU Usage: " + String.valueOf(cpuUsage+"%")
															).queue();
										else{
											timer.cancel();
										}
										i++;
									}
								};
								timer.schedule(timertask, 1500, 5000);
							});
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
