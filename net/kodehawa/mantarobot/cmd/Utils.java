package net.kodehawa.mantarobot.cmd;

import static net.kodehawa.mantarobot.module.CommandType.*;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import org.json.JSONArray;
import org.json.JSONObject;
import us.monoid.web.Resty;

import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Utils extends Module {

	private final Mantaro mantaro = Mantaro.instance();
	private final Resty resty = new Resty();

	public Utils(){
		this.registerCommands();
	}
	
	@Override
	public void registerCommands(){
		super.register("help", "Display this help.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				guild = event.getGuild();
				channel = event.getChannel();
				author = event.getAuthor();
				Member member = guild.getMember(author);
				if(content.isEmpty()){
					StringBuilder builderuser = new StringBuilder();
					StringBuilder builderadmin = new StringBuilder();
					for(String cmd : Module.modules.keySet()){
						if(!Module.moduleDescriptions.get(cmd).isEmpty() && Module.modules.get(cmd).commandType().equals(USER))
							builderuser.append(cmd).append(": ").append(Module.moduleDescriptions.get(cmd)).append("\n");
						else if(!Module.moduleDescriptions.get(cmd).isEmpty() && Module.modules.get(cmd).commandType().equals(ADMIN))
							builderadmin.append(cmd).append(": ").append(Module.moduleDescriptions.get(cmd)).append("\n");
					}

					event.getAuthor().openPrivateChannel().queue(
							s -> {
								channel.sendMessage(":envelope: Delivered!").queue();
								EmbedBuilder embed = new EmbedBuilder();
								embed.setColor(Color.PINK)
										.setDescription(":exclamation: Command help. For extended help use this command with a command name as argument (For example ~>help yandere).\n"
												+ ":exclamation: Remember: *all* commands as for now use the " + Parameters.getPrefixForServer(guild.getId())  +" custom prefix on **this** server and " + Parameters.getPrefixForServer("default") + " as global prefix."
												+ " So put that before the command name to execute it.\n"
												+ ":star: Mantaro version: " + Mantaro.instance().getMetadata("build")
												+ Mantaro.instance().getMetadata("date") + "_J" + JDAInfo.VERSION + "\n\n"
												+ "**User commands:**\n"
												+ builderuser.toString() +"\n");
								s.sendMessage(embed.build()).queue(
										success ->
										{
											if(member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MESSAGE_MANAGE)
													|| member.hasPermission(Permission.BAN_MEMBERS) || member.hasPermission(Permission.KICK_MEMBERS))
											{
												EmbedBuilder embed1 = new EmbedBuilder();
												embed1.setColor(Color.PINK)
														.setDescription("**Admin commands:**\n"
																+ builderadmin.toString());
												s.sendMessage(embed1.build()).queue();
											}
										});
							}
					);
				} else{ if(Module.modules.containsKey(content)){
					if(!Module.modules.get(content).help().isEmpty())
						channel.sendMessage(Module.modules.get(content).help()).queue();
					else
						channel.sendMessage(":heavy_multiplication_x: No extended help set for this command.").queue();
					}
				}
			}

			@Override
			public String help() {
				return "";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
		super.register("translate", "Translates a given sentence", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				channel = event.getChannel();

				if(!content.isEmpty()){
					String sourceLang = args[0];
					String targetLang = args[1];
					String textToEncode = content.replace(args[0] + " " + args[1] + " ", "");
					String textEncoded = "";
					String translatorUrl2;

					try {
						textEncoded = URLEncoder.encode(textToEncode, "UTF-8");
					} catch (UnsupportedEncodingException e1){
						e1.printStackTrace();
					}

					String translatorUrl = String.format("https://translate.google.com/translate_a/single?client=at&dt=t&dt=ld&dt=qca&dt=rm&dt=bd&dj=1&hl=es-ES&ie=UTF-8&oe=UTF-8&inputm=2&otf=2&iid=1dd3b944-fa62-4b55-b330-74909a99969e&sl=%1s&tl=%2s&dt=t&q=%3s", sourceLang, targetLang, textEncoded);

					try {
						resty.identifyAsMozilla();
						translatorUrl2 = resty.text(translatorUrl).toString();

						JSONObject jObject = new JSONObject(translatorUrl2);
						JSONArray data = jObject.getJSONArray("sentences");

						for(int i = 0; i < data.length(); i++) {
							JSONObject entry = data.getJSONObject(i);
							System.out.println(entry);
							channel.sendMessage(":speech_balloon: " + "Translation for " + textToEncode +": " + entry.getString("trans")).queue();
						}

						System.out.println(translatorUrl2);
					} catch (IOException e) {
						e.printStackTrace();
						channel.sendMessage(":heavy_multiplication_x:" + "Something went wrong when translating... :c").queue();
					}
				} else {
					channel.sendMessage(help()).queue();
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

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}
}
