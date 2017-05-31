package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.async.Async;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.Unirest;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.anime.AnimeData;
import net.kodehawa.mantarobot.commands.anime.CharacterData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Command;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.events.PostLoadEvent;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.net.URLEncoder;
import java.util.stream.Collectors;

@Slf4j
@Module
public class AnimeCmds {
	public static String authToken;

	@Command
	public static void anime(CommandRegistry cr) {
		cr.register("anime", new SimpleCommand(Category.FUN) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				try {
					String connection = String.format("https://anilist.co/api/anime/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
					String json = Utils.wget(connection, event);
					AnimeData[] type = GsonDataManager.GSON_PRETTY.fromJson(json, AnimeData[].class);

					if (type.length == 1) {
						animeData(event, type[0]);
						return;
					}

					DiscordUtils.selectList(event, type, anime -> String.format("**[%s (%s)](%s)**",
						anime.getTitle_english(), anime.getTitle_japanese(), "http://anilist.co/anime/" + anime.getId()),
						s -> baseEmbed(event, "Type the number of the anime you want to select.")
								.setDescription(s)
								.setThumbnail("https://anilist.co/img/logo_al.png")
								.setFooter("Information provided by Anilist.", event.getAuthor().getAvatarUrl())
								.build(),
						anime -> animeData(event, anime));
				} catch (Exception e) {
					if (e instanceof JsonSyntaxException) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "No results or the API query was unsuccessful").queue();
						return;
					}

					if (e instanceof NullPointerException) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "We got a wrong API result for this specific search. Maybe try another one?").queue();
						return;
					}
					event.getChannel().sendMessage(EmoteReference.ERROR + "**Houston, we have a problem!**\n\n > We received a ``" + e.getClass().getSimpleName() + "`` while trying to process the command. \nError: ``" + e.getMessage() + "``").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Anime command")
					.setDescription("**Get anime info from AniList (For anime characters use ~>character).**")
						.addField("Usage", "`~>anime <animename>` - **Retrieve information of an anime based on the name.**", false)
						.addField("Parameters",
								"`animename` - **The name of the anime you are looking for. Keep queries similar to their english names!**", false)
					.setColor(Color.PINK)
					.build();
			}
		});

		cr.registerAlias("anime", "animu");
	}

	/**
	 * returns the new AniList access token.
	 */
	public static void authenticate() {
		String aniList = "https://anilist.co/api/auth/access_token";
		String CLIENT_ID = MantaroData.config().get().getAlClient();
		try {
			authToken = Unirest.post(aniList)
				.header("User-Agent", "Mantaro")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.body("grant_type=client_credentials&client_id=" + CLIENT_ID + "&client_secret=" + MantaroData.config().get().alsecret)
				.asJson()
				.getBody()
				.getObject().getString("access_token");
			log.info("Updated auth token.");
		} catch (Exception e) {
			log.warn("Problem while updating auth token! <@155867458203287552>, check nohup.out.");
			log.warn("Problem while updating auth token! <@155867458203287552> check it out", e);
		}
	}

	@Command
	public static void character(CommandRegistry cr) {
		cr.register("character", new SimpleCommand(Category.FUN) {
			@Override
			public void call(GuildMessageReceivedEvent event, String content, String[] args) {
				try {
					String url = String.format("https://anilist.co/api/character/search/%1s?access_token=%2s", URLEncoder.encode(content, "UTF-8"), authToken);
					String json = Utils.wget(url, event);
					CharacterData[] character = GsonDataManager.GSON_PRETTY.fromJson(json, CharacterData[].class);

					if (character.length == 1) {
						characterData(event, character[0]);
						return;
					}

					DiscordUtils.selectList(event, character, character1 -> String.format("**[%s %s](%s)**",
						character1.name_last == null ? "" : character1.name_last , character1.name_first,
							"http://anilist.co/character/" + character1.getId()),
						s -> baseEmbed(event, "Type the number of the character you want to select.")
								.setDescription(s)
								.setThumbnail("https://anilist.co/img/logo_al.png")
								.setFooter("Information provided by Anilist.", event.getAuthor().getAvatarUrl())
								.build(),
						character1 -> characterData(event, character1));
				} catch (Exception e) {
					if (e instanceof JsonSyntaxException) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "No results!").queue();
						return;
					}
					log.warn("Problem processing data.", e);
					event.getChannel().sendMessage(EmoteReference.ERROR + "**We have a problem!**\n\n > I got ``" + e.getClass().getSimpleName() + "`` while trying to process this command. \nError: ``" + e.getMessage() + "``").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Character command")
						.setDescription("**Get character info from AniList (For anime use `~>anime`).**")
						.addField("Usage", "`~>character <name>` - **Retrieve information of a charactrer based on the name.**", false)
						.addField("Parameters",
								"`name` - **The name of the character you are looking for. Keep queries similar to their romanji names!**", false)
						.setColor(Color.PINK)
						.build();
			}
		});

		cr.registerAlias("character", "char");
	}

	@Command
	public static void onPostLoad(PostLoadEvent e) {
		Async.task("AniList Login Task", AnimeCmds::authenticate, 1900);
	}

	private static void animeData(GuildMessageReceivedEvent event, AnimeData type) {
		String ANIME_TITLE = type.getTitle_english();
		String RELEASE_DATE = StringUtils.substringBefore(type.getStart_date(), "T");
		String END_DATE = StringUtils.substringBefore(type.getEnd_date(), "T");
		String ANIME_DESCRIPTION = type.getDescription().replaceAll("<br>", "\n");
		String AVERAGE_SCORE = type.getAverage_score();
		String IMAGE_URL = type.getImage_url_lge();
		String TYPE = Utils.capitalize(type.getSeries_type());
		String EPISODES = type.getTotal_episodes().toString();
		String DURATION = type.getDuration().toString();
		String GENRES = type.getGenres().stream().collect(Collectors.joining(", "));

		//Start building the embedded message.
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(Color.LIGHT_GRAY)
			.setAuthor("Anime information for " + ANIME_TITLE, "http://anilist.co/anime/"
				+ type.getId(), type.getImage_url_sml())
			.setFooter("Information provided by AniList", null)
			.setThumbnail(IMAGE_URL)
			.addField("Description: ", ANIME_DESCRIPTION.length() <= 1024 ? ANIME_DESCRIPTION : ANIME_DESCRIPTION.substring(0, 1020) + "...", false)
			.addField("Release date: ", "`" + RELEASE_DATE + "`", true)
			.addField("End date: ", "`" + END_DATE + "`", true)
			.addField("Average score: ", "`" + AVERAGE_SCORE + "/100" + "`", true)
			.addField("Type",  "`" + TYPE + "`", true)
			.addField("Episodes",  "`" + EPISODES + "`", true)
			.addField("Episode Duration", "`" + DURATION + " minutes." + "`", true)
			.addField("Genres", "`" + GENRES + "`", false);
		event.getChannel().sendMessage(embed.build()).queue();
	}

	private static void characterData(GuildMessageReceivedEvent event, CharacterData character) {
		String JAP_NAME = character.getName_japanese() == null ? "" : "\n(" + character.getName_japanese() + ")";
		String CHAR_NAME = character.getName_first() + " " + character.getName_last() + JAP_NAME;
		String ALIASES = character.getName_alt() == null ? "No aliases" : "Also known as: " + character.getName_alt();
		String IMAGE_URL = character.getImage_url_med();
		String CHAR_DESCRIPTION = character.getInfo().isEmpty() ? "No info."
			: character.getInfo().length() <= 1024 ? character.getInfo() : character.getInfo().substring(0, 1020 - 1) + "...";
		EmbedBuilder embed = new EmbedBuilder();
		embed.setColor(Color.LIGHT_GRAY)
			.setThumbnail(IMAGE_URL)
			.setAuthor("Information for " + CHAR_NAME, "http://anilist.co/character/" + character.getId(), IMAGE_URL)
			.setDescription(ALIASES)
			.addField("Information", CHAR_DESCRIPTION, true)
			.setFooter("Information provided by AniList", null);

		event.getChannel().sendMessage(embed.build()).queue();
	}
}
