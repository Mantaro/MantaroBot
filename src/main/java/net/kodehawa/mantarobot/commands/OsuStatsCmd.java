package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.osu.api.ciyfhx.Mod;
import com.osu.api.ciyfhx.OsuClient;
import com.osu.api.ciyfhx.User;
import com.osu.api.ciyfhx.UserScore;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.osu.OsuMod;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.json.JSONException;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Module
public class OsuStatsCmd {
	private final ExecutorService pool = Executors.newCachedThreadPool();
	private final Map<String, Object> map = new HashMap<>();
	private String mods1 = "";
	private OsuClient osuClient = new OsuClient(MantaroData.config().get().osuApiKey);

	private String best(String content) {
		String finalResponse;
		try {
			long start = System.currentTimeMillis();
			String beheaded1 = content.replace("best ", "");
			String[] args = beheaded1.split(" ");
			map.put("m", 0);

			User hey = osuClient.getUser(args[0], map);
			MantaroBot.getInstance().getStatsClient().gauge("osu_user_ping", System.currentTimeMillis() - start);
			List<UserScore> userBest = osuClient.getUserBest(hey, map);
			MantaroBot.getInstance().getStatsClient().gauge("osu_score_ping", System.currentTimeMillis() - start);
			StringBuilder sb = new StringBuilder();
			StringBuilder sb1 = new StringBuilder();

			for (UserScore userScore : userBest) {
				if (userScore.getEnabledMods().size() > 0) {
					for (Mod mod : userScore.getEnabledMods()) {
						sb1.append(OsuMod.get(mod).getAbbreviation());
					}

					mods1 = "Mods: " + sb1.toString();
					sb1.setLength(0);
				}

				sb.append(String.format("# %s -> %s\n | ####### | [%dpp] | Rank: %s -> Max Combo: %d",
						userScore.getBeatMap().getTitle().replace("'", ""), mods1,
						(int) userScore.getPP(), userScore.getRank(), userScore.getMaxCombo()))
				.append("\n");
			}

			finalResponse = "```md\n" + sb.toString() + "```";
		} catch (Exception e) {
			if (e instanceof JSONException) finalResponse = EmoteReference.ERROR + "No results found.";
			else {
				finalResponse = EmoteReference.ERROR + "Error while looking for results.";
				SentryHelper.captureException("Error retrieving results from osu!API", e, OsuStatsCmd.class);
			}
		}

		return finalResponse;
	}

	@Subscribe
	public void osustats(CommandRegistry cr) {
		cr.register("osustats", new SimpleTreeCommand(Category.GAMES) {
			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "osu! command")
						.setDescription("**Retrieves information from the osu!API**.")
						.addField("Usage", "`~>osustats best <player>` - **Retrieves best scores of the user specified in the specified gamemode**.\n"
										+ "`~>osustats recent <player>` - **Retrieves recent scores of the user specified in the specified gamemode.**\n"
										+ "`~>osustats user <player>` - **Retrieves information about a osu! player**.\n"
								, false)
						.addField("Parameters", "`player` - **The osu! player to look info for.**", false)
						.build();
			}
		}.addSubCommand("best", new SubCommand() {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content) {
				event.getChannel().sendMessage(EmoteReference.STOPWATCH + "Retrieving information from osu! server...").queue(sentMessage -> {
					Future<String> task = pool.submit(() -> best(content));
					try {
						sentMessage.editMessage(task.get(16, TimeUnit.SECONDS)).queue();
					} catch (Exception e) {
						if (e instanceof TimeoutException) {
							task.cancel(true);
							sentMessage.editMessage(EmoteReference.ERROR + "Request timeout. Maybe osu! API is slow?").queue();
						} else {
							SentryHelper.captureException("Error retrieving results from osu!API", e, OsuStatsCmd.class);
						}
					}
				});
			}
		}).addSubCommand("recent", new SubCommand() {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content) {
				event.getChannel().sendMessage(EmoteReference.STOPWATCH + "Retrieving information from server...").queue(sentMessage -> {
					Future<String> task = pool.submit(() -> recent(content));
					try {
						sentMessage.editMessage(task.get(16, TimeUnit.SECONDS)).queue();
					} catch (Exception e) {
						if (e instanceof TimeoutException) {
							task.cancel(true);
							sentMessage.editMessage(EmoteReference.ERROR + "Request timeout. Maybe osu! API is slow?").queue();
						} else log.warn("Exception thrown while fetching data", e);
					}
				});
			}
		}).addSubCommand("user", new SubCommand() {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content) {
				event.getChannel().sendMessage(user(content)).queue();
			}
		}));

		cr.registerAlias("osustats", "osu");
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
				if (n1 > 9)
					break;
				n1++;
				if (u.getEnabledMods().size() > 0) {
					List<Mod> mods = u.getEnabledMods();
					StringBuilder sb1 = new StringBuilder();
					mods.forEach(mod -> sb1.append(OsuMod.get(mod).getAbbreviation()));
					mods1 = " Mods: " + sb1.toString();
				}

				recent.add(
					String.format("# %s -> %s\n | (★%s) - %s | Date: %s -> Max Combo: %d\n", u.getBeatMap().getTitle().replace("'", ""), mods1,
						df.format(u.getBeatMap().getDifficultyRating()), u.getBeatMap().getCreator(), u.getDate(), u.getMaxCombo()));
			}

			recent.forEach(sb::append);
			long end = System.currentTimeMillis() - start;
			finalMessage = "```md\n" + sb.toString() + " \n<Response time: " + end + "ms>```";

		} catch (Exception e) {
			if (e instanceof JSONException) finalMessage = EmoteReference.ERROR + "No results found.";
			else {
				finalMessage = EmoteReference.ERROR + "Error while looking for results.";
				SentryHelper.captureException("Error retrieving results from osu!API", e, OsuStatsCmd.class);
			}
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
			EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle("Error.", null)
				.setColor(Color.RED)
				.addField("Description", "Error retrieving results or no results found. (" + e.getMessage() + ")", false);
			finalMessage = builder.build();
		}
		return finalMessage;
	}

}