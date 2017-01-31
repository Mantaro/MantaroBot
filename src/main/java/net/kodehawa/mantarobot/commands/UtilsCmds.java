package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.Data;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.YoutubeMp3Info;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;

import java.awt.Color;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class UtilsCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("UtilsCmds");
	private final Resty resty = new Resty();

	public UtilsCmds() {
		super(Category.MISC);
		translate();
		birthday();
		ytmp3();
	}

	private void birthday() {
		super.register("birthday", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				TextChannel channel = event.getChannel();
				String userId = event.getMessage().getAuthor().getId();
				SimpleDateFormat format1 = new SimpleDateFormat("dd-MM-yyyy");
				Date bd1;
				//So they don't input something that isn't a date...
				try {
					bd1 = format1.parse(args[0]);
				} catch (Exception e) {
					if (args[0] != null)
						channel.sendMessage("\u274C" + args[0] + " is not a valid date or I cannot parse it.").queue();
					return;
				}

				MantaroData.getData().get().users.computeIfAbsent(userId, k -> new Data.UserData()).birthdayDate = format1.format(bd1);
				MantaroData.getData().update();
				channel.sendMessage("\uD83D\uDCE3 Added birthday date.").queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Birthday")
					.setDescription("Sets your birthday date.\n"
						+ "**Usage:**\n"
						+ "~>birthday [date]. Sets your birthday date. Only useful if the server enabled this functionality"
						+ "**Parameter explanation:**\n"
						+ "[date]. A date in dd-mm-yyyy format (13-02-1998 for example)")
					.setColor(Color.DARK_GRAY)
					.build();
			}
		});
	}

	private void translate() {
		super.register("translate", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					TextChannel channel = event.getChannel();

					if (!content.isEmpty()) {
						String sourceLang = args[0];
						String targetLang = args[1];
						String textToEncode = content.replace(args[0] + " " + args[1] + " ", "");
						String textEncoded = "";
						String translatorUrl2;

						try {
							textEncoded = URLEncoder.encode(textToEncode, "UTF-8");
						} catch (UnsupportedEncodingException ignored) {
						}

						String translatorUrl = String.format("https://translate.google.com/translate_a/single?client=at&dt=t&dt=ld&dt=qca&dt=rm&dt=bd&dj=1&hl=es-ES&ie=UTF-8&oe=UTF-8&inputm=2&otf=2&iid=1dd3b944-fa62-4b55-b330-74909a99969e&sl=%1s&tl=%2s&dt=t&q=%3s", sourceLang, targetLang, textEncoded);

						try {
							resty.identifyAsMozilla();
							translatorUrl2 = resty.text(translatorUrl).toString();

							JSONObject jObject = new JSONObject(translatorUrl2);
							JSONArray data = jObject.getJSONArray("sentences");

							for (int i = 0; i < data.length(); i++) {
								JSONObject entry = data.getJSONObject(i);
								channel.sendMessage(":speech_balloon: " + "Translation for " + textToEncode + ": " + entry.getString("trans")).queue();
							}
						} catch (IOException e) {
							LOGGER.warn("Something went wrong when translating.", e);
							channel.sendMessage(":heavy_multiplication_x:" + "Something went wrong when translating... :c").queue();
						}
					} else {
						onHelp(event);
					}
				} catch (Exception e) {
					//trans not found intensifies?
					LOGGER.warn("Something went wrong while processing translation elements.", e);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Translation command")
					.setDescription("Translates any given sentence.\n"
						+ "**Usage example:**\n"
						+ "~>translate [sourcelang] [outputlang] [sentence].\n"
						+ "**Parameter explanation**\n"
						+ "[sourcelang] The language the sentence is written in. Use codes (english = en)\n"
						+ "[outputlang] The language you want to translate to (french = fr, for example)\n"
						+ "[sentence] The sentence to translate.")
					.setColor(Color.BLUE)
					.build();
			}

		});
	}

	private void ytmp3() {
		super.register("ytmp3", new SimpleCommand() {
			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				YoutubeMp3Info info = YoutubeMp3Info.forLink(content, event);

				if (info == null) return; //I think we already logged this in the YoutubeMp3Info class

				if (info.error != null) {
					//TODO PRINT ERROR TO USERS
					return;
				}

				EmbedBuilder builder = new EmbedBuilder()
					.setAuthor(info.title, info.link, event.getAuthor().getEffectiveAvatarUrl())
					.setFooter("Powered by youtubeinmp3.com API", null);

				try {
					int length = Integer.parseInt(info.length);
					builder.addField("Length",
						String.format(
							"%02d minutes, %02d seconds",
							SECONDS.toMinutes(length),
							length - MINUTES.toSeconds(SECONDS.toMinutes(length))
						),
						false
					);
				} catch (Exception ignored) {
				}

				event.getChannel().sendMessage(builder
					.addField("Download","[Click Here!]("+info.link+")", false)
					.build()
				).queue();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}

}
