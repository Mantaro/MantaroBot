package net.kodehawa.mantarobot.commands.osu;

import com.osu.api.ciyfhx.Mod;
import com.osu.api.ciyfhx.OsuClient;
import com.osu.api.ciyfhx.User;
import com.osu.api.ciyfhx.UserScore;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OsuCmd extends Module{
	private static final Logger LOGGER = LoggerFactory.getLogger("osu!");
	private Map<String, Object> map = new HashMap<>();
	private OsuClient osuClient = null;
	String mods1 = "";

	public OsuCmd(){
		super(Category.GAMES);
		osuClient = new OsuClient(MantaroData.getConfig().get().osuApiKey);
		osu();
	}

	private void osu(){
		super.register("osu", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				String noArgs = content.split(" ")[0];
				switch (noArgs) {
					case "best":
						event.getChannel().sendMessage("\uD83D\uDCAC Retrieving information from osu! server...").queue(sentMessage ->
						{
							Future<String> task = Async.getThreadPool().submit(() -> best(content));
							try {
								sentMessage.editMessage(task.get(16, TimeUnit.SECONDS)).queue();
								task.cancel(true);
							} catch (Exception e) {
								if (e instanceof TimeoutException)
									sentMessage.editMessage("\u274C Request timeout. Maybe osu! API is slow?").queue();
								else
									LOGGER.warn("[osu] Exception thrown while fetching data", e);
								e.printStackTrace();
							}
						});
						break;
					case "recent":
						event.getChannel().sendMessage("\uD83D\uDCAC Retrieving information from server...").queue(sentMessage ->
						{
							Future<String> task = Async.getThreadPool().submit(() -> recent(content));
							try {
								sentMessage.editMessage(task.get(16, TimeUnit.SECONDS)).queue();
								task.cancel(true);
							} catch (Exception e) {
								if (e instanceof TimeoutException)
									sentMessage.editMessage("\u274C Request timeout. Maybe osu! API is slow?").queue();
								else
									LOGGER.warn("[osu] Exception thrown while fetching data", e);
									e.printStackTrace();
							}
						});
						break;
					case "user":
						event.getChannel().sendMessage(user(content)).queue();
						break;
					default:
						event.getChannel().sendMessage(help(event)).queue();
						break;
				}
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "osu! command")
						.setDescription("Retrieves information from the osu!api.\n"
								+ "Usage: \n"
								+ "~>osu best [player]: Retrieves best scores of the user specified in the specified gamemode.\n"
								+ "~>osu recent [player]: Retrieves recent scores of the user specified in the specified gamemode.\n"
								+ "~>osu user [player]: Retrieves information about a osu! player.\n"
								+ "Parameter description:\n"
								+ "[player]: The osu! player to look info for.")
						.build();
			}
		});
	}

	private String best(String content) {
		String finalResponse;
		try {
			long start = System.currentTimeMillis();
			String beheaded1 = content.replace("best ", "");
			String[] args = beheaded1.split(" ");
			map.put("m", 0);

			User hey = osuClient.getUser(args[0], map);
			List<UserScore> userBest = osuClient.getUserBest(hey, map);
			StringBuilder sb = new StringBuilder();
			List<String> best = new CopyOnWriteArrayList<>();

			int n1 = 0;
			DecimalFormat df = new DecimalFormat("####0.0");
			for(UserScore userScore : userBest){
				n1++;
				if (userScore.getEnabledMods().size() > 0){
					List<Mod> mods = userScore.getEnabledMods();
					StringBuilder sb1 = new StringBuilder();
					mods.forEach(mod -> sb1.append(OsuMod.get(mod).getAbbreviation()));
					mods1 = " / Mods: " + sb1.toString();
				}

				best.add(n1 + ".- " + userScore.getBeatMap().getTitle().replace("'", "") +
						" (\u2605" + df.format(userScore.getBeatMap().getDifficultyRating()) + ") - " + userScore.getBeatMap().getCreator()
						+ mods1
						+ "\n   Date: " + userScore.getDate() + " ~ Max Combo: " + userScore.getMaxCombo() +
						" ~ PP: " + df.format(userScore.getPP()) + " ~ Rank: " + userScore.getRank() + "\n");
				best.forEach(sb::append);
			}

			long end = System.currentTimeMillis() - start;
			finalResponse = "```ruby\n" + sb.toString() + " \nResponse time: " + end + "ms```";
		} catch (Exception e) {
			e.printStackTrace();
			finalResponse = "\u274C Error retrieving results or no results found. (" + e.getMessage() + ")";
		}

		return finalResponse;
	}

	private String recent(String content) {
		String finalMessage;
		try {
			long start = System.currentTimeMillis();
			String beheaded1 = content.replace("recent ", "");
			String[] args = beheaded1.split(" ");
			map.put("m", 0);
			User hey = osuClient.getUser(args[0], map);
			List<UserScore> userRecent = osuClient.getUserRecent(hey, map);
			StringBuilder sb = new StringBuilder();
			List<String> recent = new CopyOnWriteArrayList<>();
			int n1 = 0;
			DecimalFormat df = new DecimalFormat("####0.0");
			for (UserScore u : userRecent) {
				StringBuilder sb1 = new StringBuilder();
				userRecent.forEach(user -> {
					if (u.getEnabledMods().size() > 0) {
						List<Mod> mods = u.getEnabledMods();
						mods.forEach(mod -> sb1.append(OsuMod.get(mod).getAbbreviation()));
					}
				});

				n1++;

				recent.add(n1 + ".- " + u.getBeatMap().getTitle().replace("'", "") + " (\u2605"
						+ df.format(u.getBeatMap().getDifficultyRating()) + ") - " + u.getBeatMap().getCreator()
						+ "Mods: " + sb1.toString()
						+ "\n Date: " + u.getDate() + " ~ Max Combo: " + u.getMaxCombo() +
						"\n");
			}

			recent.forEach(sb::append);
			long end = System.currentTimeMillis() - start;
			finalMessage = "```ruby\n" + sb.toString() + " \nResponse time: " + end + "ms```";

		} catch (Exception e) {
			e.printStackTrace();
			finalMessage = "\u274C Error retrieving results or no results found. (" + e.getMessage() + ")";
		}
		return finalMessage;
	}
	private MessageEmbed user(String content) {
		MessageEmbed finalMessage;
		try {
			long start = System.currentTimeMillis();
			String beheaded1 = content.replace("user ", "");

			String[] args = beheaded1.split(" ");

			map.put("m", 0);

			User osuClientUser = osuClient.getUser(args[0], map);
			DecimalFormat dfa = new DecimalFormat("####0.00"); //For accuracy
			DecimalFormat df = new DecimalFormat("####0"); //For everything else
			long end = System.currentTimeMillis() - start;
			EmbedBuilder builder = new EmbedBuilder();
			builder.setAuthor("osu! statistics for " + osuClientUser.getUsername(), "https://osu.ppy.sh/" + osuClientUser.getUserID(), "https://a.ppy.sh/" + osuClientUser.getUserID())
					.setColor(Color.GRAY)
					.addField("Rank", "#" + df.format(osuClientUser.getPPRank()), true)
					.addField(":flag_" + osuClientUser.getCountry().toLowerCase() + ": Country Rank", "#" + df.format(osuClientUser.getPPCountryRank()), true)
					.addField("PP", df.format(osuClientUser.getPPRaw()) + "pp", true)
					.addField("Accuracy", dfa.format(osuClientUser.getAccuracy()) + "%", true)
					.addField("Level", df.format(osuClientUser.getLevel()), true)
					.addField("Ranked Score", df.format(osuClientUser.getRankedScore()), true)
					.addField("SS", df.format(osuClientUser.getCountRankSS()), true)
					.addField("S", df.format(osuClientUser.getCountRankS()), true)
					.addField("A", df.format(osuClientUser.getCountRankA()), true)
					.setFooter("Response time: " + end + "ms.", null);
			finalMessage = builder.build();
		} catch (Exception e) {
			e.printStackTrace();
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle("Error.")
					.setColor(Color.RED)
					.addField("Description", "Error retrieving results or no results found. (" + e.getMessage() + ")", false);
			finalMessage = builder.build();
		}
		return finalMessage;
	}

}
