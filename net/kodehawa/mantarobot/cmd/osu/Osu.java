package net.kodehawa.mantarobot.cmd.osu;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;

import com.osu.api.ciyfhx.Mod;
import com.osu.api.ciyfhx.OsuClient;
import com.osu.api.ciyfhx.User;
import com.osu.api.ciyfhx.UserScore;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;
import net.kodehawa.mantarobot.util.Utils;

public class Osu extends Command {

	private OsuClient osuClient = null;
	private Map<String, Object> map = new HashMap<String, Object>();

	public Osu()
	{
		setName("osu");
		setDescription("Retrieves osu! related things. Use the help argument to get details.");
	}

	@Override
	public void onCommand(String[] message, String content, MessageReceivedEvent evt) {
		osuClient = new OsuClient("fde2131d5cb0f60b38b027eeff5c85ed766b2abc");
		String noArgs = content.split(" ")[0];

		switch(noArgs){
		case "best":
			evt.getChannel().sendMessage(":speech_balloon: Retrieving information from osu! server...").queue(sentMessage ->
			{
				Timer timer = new Timer(); 
				Utils.instance().buildMessageTimer(timer, ":speech_balloon: Retrieving information from osu! server", 1500, sentMessage);
				sentMessage.editMessage(best(content)).queue();
				timer.cancel();
			});
			break;
		case "recent":
			evt.getChannel().sendMessage(":speech_balloon: Retrieving information from server...").queue(sentMessage ->
			{
				Timer timer = new Timer(); 	
				Utils.instance().buildMessageTimer(timer, ":speech_balloon: Retrieving information from osu! server", 1500, sentMessage);
				sentMessage.editMessage(recent(content)).queue();
				timer.cancel();
			});
			break;
		case "user":
			evt.getChannel().sendMessage(":speech_balloon: Retrieving information from osu! server...").queue(sentMessage ->
			{
				Timer timer = new Timer(); 
				Utils.instance().buildMessageTimer(timer, ":speech_balloon: Retrieving information from osu! server", 1500, sentMessage);
				sentMessage.editMessage(user(content)).queue();
				timer.cancel();
			});
			break;
		case "help":
			evt.getChannel().sendMessage("```ruby"
					+ "~>osu best <username> <mode> (0 = osu!, 1 = taiko, 2 = mania, 3 = ctb)\r"
					+ "~>osu recent <username> <mode> (0 = osu!, 1 = taiko, 2 = mania, 3 = ctb)\r"
					+ "~>osu user <username>  ```").queue();
			break;
		default:
			evt.getChannel().sendMessage("Incorrect usage! Use ~>osu help to get help on how to use this command!").queue();
			break;
		}
	}
		
	private String best(String beheadedMessage){
		String finalResponse = "";
		try{
			long start = System.currentTimeMillis();
			String beheaded1 = beheadedMessage.replace("best ", "");
			String[] args = beheaded1.split(" ");

			map.put("m", args[1]);
			
			User hey = osuClient.getUser(args[0], map);
			List<UserScore> userBest = osuClient.getUserBest(hey, map);
			StringBuilder sb = new StringBuilder();
			List<String> best = new CopyOnWriteArrayList<String>();
			
			int n = -1;
			int n1 = 0;
			DecimalFormat df = new DecimalFormat("####0.0");
			for (@SuppressWarnings("unused") UserScore u : userBest){
				n++;
				n1++;
				String mods1 = "";
				try{
					if(userBest.get(n).getEnabledMods().size() > 0){
						List<Mod> mods = userBest.get(n).getEnabledMods();
						int i = 0;
						StringBuilder sb1 = new StringBuilder();
						for (@SuppressWarnings("unused") Mod mod : mods){
							sb1.append(Utils.instance().getMod(mods.get(i)));
							i++;
						}
						mods1 = " / Mods: " + sb1.toString();
					}
				} catch(ArrayIndexOutOfBoundsException e){}
				
				best.add(n1 + ".- " + userBest.get(n).getBeatMap().getTitle().replace("'", "") + 
						" (\u2605"  + df.format(userBest.get(n).getBeatMap().getDifficultyRating()) + ") - " + userBest.get(n).getBeatMap().getCreator() 
						+ mods1
						+ "\r   Date: " + userBest.get(n).getDate() + " ~ Max Combo: " + userBest.get(n).getMaxCombo() +
						" ~ PP: " + df.format(userBest.get(n).getPP()) + " ~ Rank: " + userBest.get(n).getRank()  + "\r");
				sb.append(best.get(n));
			}
			long end = System.currentTimeMillis() - start;
		    finalResponse = "```ruby\n" + sb.toString() + " \rResponse time: " + end + "ms```";
		} catch(Exception e){
			e.printStackTrace();
			finalResponse = ":heavy_multiplication_x: Error retrieving results or no results found.";
		}
		
		return finalResponse;
	}
	
	private String recent(String beheadedMessage){
		String finalMessage = "";
		try{
			long start = System.currentTimeMillis();
			String beheaded1 = beheadedMessage.replace("recent ", "");
			String[] args = beheaded1.split(" ");
			map.put("m", Integer.parseInt(args[1]));
			User hey = osuClient.getUser(args[0], map);
			List<UserScore> userRecent = osuClient.getUserRecent(hey, map);
			StringBuilder sb = new StringBuilder();
			List<String> recent = new CopyOnWriteArrayList<String>();
			 
			int n = -1;
			int n1 = 0;
			DecimalFormat df = new DecimalFormat("####0.0");
			String mods1 = "";
			for (@SuppressWarnings("unused") UserScore u : userRecent){
				try{
					if(userRecent.get(n).getEnabledMods().size() > 0){
						List<Mod> mods = userRecent.get(n).getEnabledMods();
						int i = 0;
						StringBuilder sb1 = new StringBuilder();
						
						for (@SuppressWarnings("unused") Mod mod : mods){
							sb1.append(Utils.instance().getMod(mods.get(i)));
							i++;
						}
						mods1 = " / Mods: " + sb1.toString();
					}
				} catch(ArrayIndexOutOfBoundsException e){}
				
				n1++;
				recent.add(n1 + ".- " + userRecent.get(n).getBeatMap().getTitle().replace("'", "") + " (\u2605"  
						+ df.format(userRecent.get(n).getBeatMap().getDifficultyRating()) + ") - " + userRecent.get(n).getBeatMap().getCreator()
						+ mods1
						+ "\r Date: " + userRecent.get(n).getDate() + " ~ Max Combo: " + userRecent.get(n).getMaxCombo() +
						"\r");
				
				sb.append(recent.get(n));
				n++;
			}
			long end = System.currentTimeMillis() - start;
			finalMessage = "```ruby\n" + sb.toString() + " \rResponse time: " + end + "ms```";
		} catch (Exception e){
			e.printStackTrace();
			finalMessage = ":heavy_multiplication_x: Error retrieving results or no results found.";
		}
		return finalMessage;
	}
	
	private String user(String beheadedMessage){
		String finalMessage = "";
		try{
			long start = System.currentTimeMillis();
			String beheaded1 = beheadedMessage.replace("user ", "");
			
			String[] args = beheaded1.split(" ");
			
			map.put("m", 0);
			
			User hey = osuClient.getUser(args[0], map);
			DecimalFormat df = new DecimalFormat("####0");
			long end = System.currentTimeMillis() - start;
			finalMessage = "```xl\n"+ "Username: " + hey.getUsername() + " (#" + hey.getUserID() + ")" + "\rCountry: " + hey.getCountry() 
			+ "\rRank: " + df.format(hey.getPPRank()) + " | Country Rank: " + df.format(hey.getPPCountryRank()) +	
			"\rAccuracy: " + df.format(hey.getAccuracy()) + "%\rPP: " + df.format(hey.getPPRaw()) + "\r" + "Level: " + df.format(hey.getLevel())
			+"\rRanked Score: " + hey.getRankedScore() + "\rA, S, SS: " + hey.getCountRankA() + " | " + hey.getCountRankS() + " | " + hey.getCountRankSS() 
			+"\rResponse time: " + end + "ms```";
		} catch (Exception e){
			e.printStackTrace();
			finalMessage = ":heavy_multiplication_x: Error retrieving results or no results found.";
		}
		return finalMessage;
	}
}
