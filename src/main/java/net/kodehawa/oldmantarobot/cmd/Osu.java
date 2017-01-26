package net.kodehawa.oldmantarobot.cmd;

import com.osu.api.ciyfhx.Mod;
import com.osu.api.ciyfhx.OsuClient;
import com.osu.api.ciyfhx.User;
import com.osu.api.ciyfhx.UserScore;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.oldmantarobot.core.Mantaro;
import net.kodehawa.oldmantarobot.util.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Osu extends Module {

	private static final Logger LOGGER = LoggerFactory.getLogger("Osu");
	private Map<String, Object> map = new HashMap<>();
	private OsuClient osuClient = null;

	public Osu() {
		super(Category.GAMES);
		this.registerCommands();
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

			int n = -1;
			int n1 = 0;
			DecimalFormat df = new DecimalFormat("####0.0");
			for (@SuppressWarnings("unused") UserScore u : userBest) {
				n++;
				n1++;
				String mods1 = "";
				try {
					if (userBest.get(n).getEnabledMods().size() > 0) {
						List<Mod> mods = userBest.get(n).getEnabledMods();
						int i = 0;
						StringBuilder sb1 = new StringBuilder();
						for (@SuppressWarnings("unused") Mod mod : mods) {
							sb1.append(GeneralUtils.instance().getMod(mods.get(i)));
							i++;
						}
						mods1 = " / Mods: " + sb1.toString();
					}
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}

				best.add(n1 + ".- " + userBest.get(n).getBeatMap().getTitle().replace("'", "") +
					" (\u2605" + df.format(userBest.get(n).getBeatMap().getDifficultyRating()) + ") - " + userBest.get(n).getBeatMap().getCreator()
					+ mods1
					+ "\n   Date: " + userBest.get(n).getDate() + " ~ Max Combo: " + userBest.get(n).getMaxCombo() +
					" ~ PP: " + df.format(userBest.get(n).getPP()) + " ~ Rank: " + userBest.get(n).getRank() + "\n");
				sb.append(best.get(n));
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

			int n = -1;
			int n1 = 0;
			DecimalFormat df = new DecimalFormat("####0.0");
			String mods1 = "";
			for (@SuppressWarnings("unused") UserScore u : userRecent) {
				try {
					if (userRecent.get(n).getEnabledMods().size() > 0) {
						List<Mod> mods = userRecent.get(n).getEnabledMods();
						int i = 0;
						StringBuilder sb1 = new StringBuilder();

						for (@SuppressWarnings("unused") Mod mod : mods) {
							sb1.append(GeneralUtils.instance().getMod(mods.get(i)));
							i++;
						}
						mods1 = " / Mods: " + sb1.toString();
					}
				} catch (ArrayIndexOutOfBoundsException ignored) {
				}

				n1++;
				n++;

				recent.add(n1 + ".- " + userRecent.get(n).getBeatMap().getTitle().replace("'", "") + " (\u2605"
					+ df.format(userRecent.get(n).getBeatMap().getDifficultyRating()) + ") - " + userRecent.get(n).getBeatMap().getCreator()
					+ mods1
					+ "\n Date: " + userRecent.get(n).getDate() + " ~ Max Combo: " + userRecent.get(n).getMaxCombo() +
					"\n");

				sb.append(recent.get(n));
			}

			long end = System.currentTimeMillis() - start;
			finalMessage = "```ruby\n" + sb.toString() + " \nResponse time: " + end + "ms```";
		} catch (Exception e) {
			e.printStackTrace();
			finalMessage = "\u274C Error retrieving results or no results found. (" + e.getMessage() + ")";
		}
		return finalMessage;
	}

	@Override
	public void registerCommands() {
		osuClient = new OsuClient(Mantaro.getConfig().values().get("osuapikey").toString());

		super.register("osu", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
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
						event.getChannel().sendMessage(help()).queue();
						break;
				}
			}

			@Override
			public String help() {
				return "Retrieves information from the osu!api.\n"
					+ "Usage: \n"
					+ "~>osu best [player]: Retrieves best scores of the user specified in the specified gamemode.\n"
					+ "~>osu recent [player]: Retrieves recent scores of the user specified in the specified gamemode.\n"
					+ "~>osu user [player]: Retrieves information about a osu! player.\n"
					+ "Parameter description:\n"
					+ "[player]: The osu! player to look info for.\n";
			}
		});
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
