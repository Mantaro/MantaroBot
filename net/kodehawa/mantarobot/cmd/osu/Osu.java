package net.kodehawa.mantarobot.cmd.osu;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.osu.api.ciyfhx.OsuClient;
import com.osu.api.ciyfhx.User;
import com.osu.api.ciyfhx.UserScore;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.management.Command;

public class Osu extends Command {

	private OsuClient osuClient = null;
	private Map<String, Object> map = new HashMap<String, Object>();

	public Osu()
	{
		setName("osu");
		setDescription("");
	}

	@Override
	public void onCommand(String[] message, String beheadedMessage, MessageReceivedEvent evt) {
		osuClient = new OsuClient("fde2131d5cb0f60b38b027eeff5c85ed766b2abc");
		
		if(beheadedMessage.startsWith("best"))
		{
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
			for (@SuppressWarnings("unused") UserScore u : userBest)
			{
				n = n + 1;
				n1 = n1 + 1;
				best.add("[" + n1 + "].- <" + userBest.get(n).getBeatMap().getTitle().replace("'", "") + "> (☆"  + df.format(userBest.get(n).getBeatMap().getDifficultyRating()) + ") - " + userBest.get(n).getBeatMap().getCreator() 
						+ "\r   Date: " + userBest.get(n).getDate() + " ~ Max Combo: " + userBest.get(n).getMaxCombo() +
						" ~ PP: " + df.format(userBest.get(n).getPP()) + " ~ Rank: " + userBest.get(n).getRank()  + "\r");
				sb.append(best.get(n));
			}

		    
			evt.getChannel().sendMessage("```ruby\n" + sb.toString() + "```").queue();
		}
		else if(beheadedMessage.startsWith("recent"))
		{
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
			for (@SuppressWarnings("unused") UserScore u : userRecent)
			{
				n = n + 1;
				n1 = n1 + 1;
				recent.add("[" + n1 + "].- " + userRecent.get(n).getBeatMap().getTitle().replace("'", "") + " (☆"  + df.format(userRecent.get(n).getBeatMap().getDifficultyRating()) + ") - " + userRecent.get(n).getBeatMap().getCreator() 
						+ "\r Date: " + userRecent.get(n).getDate() + " ~ Max Combo: " + userRecent.get(n).getMaxCombo() +
						"\r");
				sb.append(recent.get(n));
			}

		    
			evt.getChannel().sendMessage("```ruby\n" + sb.toString() + "```").queue();
		}
		else if(beheadedMessage.startsWith("user"))
		{
			String beheaded1 = beheadedMessage.replace("user ", "");
			
			String[] args = beheaded1.split(" ");
			
			map.put("m", 0);
			
			User hey = osuClient.getUser(args[0], map);
			DecimalFormat df = new DecimalFormat("####0");
				evt.getChannel().sendMessage("```xl\n"+ "Username: " + hey.getUsername() + " (#" + hey.getUserID() + ")" + "\rCountry: " + hey.getCountry() 
				+ "\rRank: " + df.format(hey.getPPRank()) + " | Country Rank: " + df.format(hey.getPPCountryRank()) +	
				"\rAccuracy: " + df.format(hey.getAccuracy()) + "%\rPP: " + df.format(hey.getPPRaw()) + "\r" + "Level: " + df.format(hey.getLevel())
				+ "\rRanked Score: " + hey.getRankedScore() + "\rA, S, SS: " + hey.getCountRankA() + " | " + hey.getCountRankS() + " | " + hey.getCountRankSS() 
				+ "```").queue();
		}
		else if(beheadedMessage.startsWith("help"))
		{
			evt.getChannel().sendMessage("```ruby\n                          osu! Mantaro module help\r\r"
					+ "~>osu best <username> <mode> (0 = osu!, 1 = taiko, 2 = mania, 3 = ctb)\r"
					+ "~>osu recent <username> <mode> (0 = osu!, 1 = taiko, 2 = mania, 3 = ctb)\r"
					+ "~>osu user <username>  ```").queue();
		}
		else
		{
			evt.getChannel().sendMessage("Incorrect usage! Use ~>osu help to get help on how to use this command!").queue();
		}
	}
}
