package net.kodehawa.oldmantarobot.cmd;

import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandType;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.oldmantarobot.core.Mantaro;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Utils extends Module {

	private static final Logger LOGGER = LoggerFactory.getLogger("Utils");
	private final Resty resty = new Resty();

	public Utils() {
		super(Category.MISC);
		this.registerCommands();
	}

	@Override
	public void registerCommands() {

		super.register("translate", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				try {
					Mantaro.getSelf().getPresence().setGame(Game.of("even more"));
					TextChannel channel = event.getChannel();

					if (!content.isEmpty()) {
						String sourceLang = args[0];
						String targetLang = args[1];
						String textToEncode = content.replace(args[0] + " " + args[1] + " ", "");
						String textEncoded = "";
						String translatorUrl2;

						try {
							textEncoded = URLEncoder.encode(textToEncode, "UTF-8");
						} catch (UnsupportedEncodingException e1) {
							e1.printStackTrace();
						}

						String translatorUrl = String.format("https://translate.google.com/translate_a/single?client=at&dt=t&dt=ld&dt=qca&dt=rm&dt=bd&dj=1&hl=es-ES&ie=UTF-8&oe=UTF-8&inputm=2&otf=2&iid=1dd3b944-fa62-4b55-b330-74909a99969e&sl=%1s&tl=%2s&dt=t&q=%3s", sourceLang, targetLang, textEncoded);

						try {
							resty.identifyAsMozilla();
							translatorUrl2 = resty.text(translatorUrl).toString();

							JSONObject jObject = new JSONObject(translatorUrl2);
							JSONArray data = jObject.getJSONArray("sentences");

							for (int i = 0; i < data.length(); i++) {
								JSONObject entry = data.getJSONObject(i);
								System.out.println(entry);
								channel.sendMessage(":speech_balloon: " + "Translation for " + textToEncode + ": " + entry.getString("trans")).queue();
							}

							System.out.println(translatorUrl2);
						} catch (IOException e) {
							LOGGER.warn("Something went wrong when translating.", e);
							channel.sendMessage(":heavy_multiplication_x:" + "Something went wrong when translating... :c").queue();
						}
					} else {
						channel.sendMessage(help()).queue();
					}
				} catch (Exception e) {
					LOGGER.warn("Something went wrong while processing translation elements.", e);
					e.printStackTrace();
				}
			}

			@Override
			public String help() {
				return "Translates any given sentence.\n"
					+ "**Usage example:**\n"
					+ "~>translate [sourcelang] [outputlang] [sentence].\n"
					+ "**Parameter explanation**\n"
					+ "[sourcelang] The language the sentence is written in. Use codes (english = en)\n"
					+ "[outputlang] The language you want to translate to (french = fr, for example)\n"
					+ "[sentence] The sentence to translate.";
			}
		});
	}
}
