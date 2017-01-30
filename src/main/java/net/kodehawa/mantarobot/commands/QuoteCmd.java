package net.kodehawa.mantarobot.commands;

import com.google.gson.Gson;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.*;

public class QuoteCmd extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("QuoteCmd");

	private static String toJson(Map<String, LinkedHashMap<String, List<String>>> map) {
		return new Gson().toJson(map);
	}

	private final Random rand = new Random();

	public QuoteCmd() {
		super(Category.MISC);
		quote();
	}

	@SuppressWarnings({"unused", "unchecked"})
	private void quote() {
		super.register("quote", new SimpleCommand() {
			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.USER;
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return baseEmbed(event, "Quote command")
					.setDescription("> Usage:\n"
						+ "~>quote add [number]: Adds a quote with content defined by the number. For example 1 will quote the last message.\n"
						+ "~>quote random: Gets a random quote. \n"
						+ "~>quote read [number]: Gets a quote matching the number. \n"
						+ "~>quote addfrom [phrase] Adds a quote based in text search criteria.\n"
						+ "~>quote getfrom [phrase]: Searches for the first quote which matches your search criteria and prints it.\n"
						+ "> Parameters:\n"
						+ "[number]: Message number to quote. For example 1 will quote the last message.\n"
						+ "[phrase]: A part of the quote phrase.")
					.setColor(Color.DARK_GRAY)
					.build();
			}

			@Override
			protected void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				Guild guild = event.getGuild();
				User author = event.getAuthor();
				TextChannel channel = event.getChannel();
				Message receivedMessage = event.getMessage();
				List<Message> messageHistory;
				try {
					messageHistory = channel.getHistory().retrievePast(100).complete();
				} catch (Exception e) {
					e.printStackTrace(); //TODO LOG THAT SHIT
					//TODO Also log to Discord that shit exploded on the Backend.
					return;
				}

				String noArgs = content.split(" ")[0];
				String phrase = content.replace(noArgs + " ", "");
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				switch (noArgs) {
					case "add":
						try {
							int i = Integer.parseInt(phrase);
							Message m = messageHistory.get(i);

							String[] sContent = {
								m.getAuthor().getName(),
								m.getAuthor().getAvatarUrl(), m.getChannel().getName(),
								m.getGuild().getName(), String.valueOf(System.currentTimeMillis())
							};

							if (MantaroData.getQuotes().get().quotes.containsKey(guild.getId())) {
								LinkedHashMap<String, List<String>> temp = new LinkedHashMap<>();
								MantaroData.getQuotes().get().quotes.get(
									guild.getId()).put(m.getContent(), Arrays.asList(sContent)
								);

							} else {
								LinkedHashMap<String, List<String>> temp = new LinkedHashMap<>();
								temp.put(
									m.getContent(), Arrays.asList(sContent)
								);
								MantaroData.getQuotes().get().quotes.put(guild.getId(), temp);
							}

							MantaroData.getQuotes().update();

							Date quoteDate = new Date(System.currentTimeMillis());
							EmbedBuilder builder = new EmbedBuilder();
							builder.setAuthor(m.getAuthor().getName() + " said:", null, m.getAuthor().getEffectiveAvatarUrl())
								.setThumbnail(m.getAuthor().getEffectiveAvatarUrl())
								.setColor(m.getGuild().getMember(m.getAuthor()).getColor())
								.setDescription("Quote made on server " + m.getGuild().getName()
									+ " in channel " + "#" + m.getChannel().getName())
								.addField("Content", m.getContent(), false)
								.setFooter("Date: " + dateFormat.format(quoteDate), null);
							channel.sendMessage(builder.build()).queue();
						} catch (Exception e) {
							channel.sendMessage("\u274C Error while adding quote: " + e.getCause() + e.getMessage()).queue();
							e.printStackTrace(); //TODO LOG THAT SHIT
						}
						break;
					case "random":
						List<String> keys = new ArrayList<>(MantaroData.getQuotes().get().quotes.get(event.getGuild().getId()).keySet());
						int quoteN = rand.nextInt(keys.size());
						List<String> quoteElements = MantaroData.getQuotes().get().quotes.get(event.getGuild().getId()).get(keys.get(quoteN));

						EmbedBuilder embedBuilder = new EmbedBuilder();
						Date dat = new Date(Long.parseLong(quoteElements.get(4)));
						embedBuilder.setAuthor(quoteElements.get(0) + " said:", null, quoteElements.get(1))
							.setThumbnail(quoteElements.get(1))
							.setColor(Color.CYAN)
							.setDescription("Quote made on server " + quoteElements.get(3)
								+ " in channel " + "#" + quoteElements.get(2))
							.addField("Content", keys.get(quoteN), false)
							.setFooter("Date: " + dateFormat.format(dat), null);
						channel.sendMessage(embedBuilder.build()).queue();

						break;
					case "read":
						int i = Integer.parseInt(phrase);
						List<String> keys1 = new ArrayList<>(MantaroData.getQuotes().get().quotes.get(event.getGuild().getId()).keySet());
						List<String> quoteElements2 = MantaroData.getQuotes().get().quotes.get(event.getGuild().getId()).get(keys1.get(i));
						EmbedBuilder embedBuilder2 = new EmbedBuilder();
						Date date1 = new Date(Long.parseLong(quoteElements2.get(4)));
						embedBuilder2.setAuthor(quoteElements2.get(0) + " said:", null, quoteElements2.get(1))
							.setThumbnail(quoteElements2.get(1))
							.setColor(Color.CYAN)
							.setDescription("Quote made on server " + quoteElements2.get(3)
								+ " in channel " + "#" + quoteElements2.get(2))
							.addField("Content", keys1.get(i), false)
							.setFooter("Date: " + dateFormat.format(date1), null);
						channel.sendMessage(embedBuilder2.build()).queue();

						break;
					case "addfrom":
						int i1 = -1;
						Message m = event.getMessage();
						for (Message m1 : messageHistory) {
							i1++;
							if (m1.getContent().contains(phrase) && !m1.getContent().startsWith(MantaroData.getData().get().defaultPrefix)) {
								m = messageHistory.get(i1);
								break;
							}
						}

						String[] sContent = {
							m.getAuthor().getName(),
							m.getAuthor().getAvatarUrl(), m.getChannel().getName(),
							m.getGuild().getName(), String.valueOf(System.currentTimeMillis())
						};

						if (MantaroData.getQuotes().get().quotes.containsKey(guild.getId())) {
							LinkedHashMap<String, List<String>> temp = new LinkedHashMap<>();
							MantaroData.getQuotes().get().quotes.get(guild.getId()).put(m.getContent(), Arrays.asList(sContent));
						} else {
							LinkedHashMap<String, List<String>> temp = new LinkedHashMap<>();
							temp.put(m.getContent(), Arrays.asList(sContent));
							MantaroData.getQuotes().get().quotes.put(guild.getId(), temp);
						}

						MantaroData.getQuotes().update();

						Date quoteDate = new Date(System.currentTimeMillis());
						EmbedBuilder builder = new EmbedBuilder();
						builder.setAuthor(m.getAuthor().getName() + " said:", null, m.getAuthor().getEffectiveAvatarUrl())
							.setThumbnail(m.getAuthor().getEffectiveAvatarUrl())
							.setColor(m.getGuild().getMember(m.getAuthor()).getColor())
							.setDescription("Quote made on server " + m.getGuild().getName()
								+ " in channel " + "#" + m.getChannel().getName())
							.addField("Content", m.getContent(), false)
							.setFooter("Date: " + dateFormat.format(quoteDate), null);
						channel.sendMessage(builder.build()).queue();

						break;
					case "getfrom":
						List<String> quotes = new ArrayList(MantaroData.getQuotes().get().quotes.get(event.getGuild().getId()).keySet());
						for (int i2 = 0; i2 < quotes.size() - 1; i2++) {
							if (quotes.get(i2).contains(phrase)) {
								List<String> quoteE = MantaroData.getQuotes().get().quotes.get(event.getGuild().getId()).get(quotes.get(i2));
								Date date = new Date(Long.parseLong(quoteE.get(4)));
								EmbedBuilder builder2 = new EmbedBuilder();
								builder2.setAuthor(quoteE.get(0) + " said:", null, quoteE.get(1))
									.setThumbnail(quoteE.get(1))
									.setColor(Color.CYAN)
									.setDescription("Quote made on server " + quoteE.get(3)
										+ " in channel " + "#" + quoteE.get(2))
									.addField("Content", quotes.get(i2), false)
									.setFooter("Date: " + dateFormat.format(date), null);
								channel.sendMessage(builder2.build()).queue();
								break;
							}
						}

						if (MantaroData.getConfig().get().owners.contains(event.getAuthor().getId()))
							event.getChannel().sendMessage(Utils.paste(Utils.instance().toPrettyJson(toJson(MantaroData.getQuotes().get().quotes)))).queue();
						else event.getChannel().sendMessage("What are you trying to do, silly.").queue();

						break;
					case "debug":
						if (MantaroData.getConfig().get().owners.contains(event.getAuthor().getId()))
							event.getChannel().sendMessage(Utils.paste(Utils.instance().toPrettyJson(toJson(MantaroData.getQuotes().get().quotes)))).queue();
						else event.getChannel().sendMessage("What are you trying to do, silly.").queue();

						break;
					default:
						onHelp(event);
						break;
				}
			}
		});
	}
}
