package net.kodehawa.mantarobot.cmd;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.cmd.guild.Parameters;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.StringArrayUtils;

public class Quote extends Module {

	public static CopyOnWriteArrayList<String> quotes = new CopyOnWriteArrayList<>();

	public Quote()
	{
		new StringArrayUtils("quote", quotes, false, true);
		this.registerCommands();
	}

	@SuppressWarnings("unused")
	@Override
	public void registerCommands(){
		super.register("quote", "Adds or retrieves quotes.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, MessageReceivedEvent event) {
				Random rand = new Random();
				guild = event.getGuild();
				author = event.getAuthor();
				channel = event.getChannel();
				receivedMessage = event.getMessage();
				receivedMessage.getRawContent();
				List<Message> messageHistory= null;
				try {
					messageHistory = channel.getHistory().retrievePast(100).complete();
				} catch (Exception e) {
					e.printStackTrace();
				}

				String noArgs = content.split(" ")[0];
				String phrase = content.replace(noArgs + " ", "");
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				switch(noArgs){
					default:
						channel.sendMessage(help()).queue();
						break;
					case "add":
						try{
							int i = Integer.parseInt(phrase);
							Message m = messageHistory.get(i);
							//[0]content====[1]authorname====[2]authoravatar====[3]channelname====[4]guildname====[5]quotetime, will be read like this to create the embed if called later.
							String out = m.getContent() + "====" + m.getAuthor().getName() + "====" + m.getAuthor().getAvatarUrl() +
									"====" + m.getChannel().getName() + "====" + m.getGuild().getName() + "====" + System.currentTimeMillis();
							quotes.add(out);
							Date quoteDate = new Date(System.currentTimeMillis());
							new StringArrayUtils("quote", quotes, true, true);
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
						} catch (Exception e){
							channel.sendMessage(":heavy_multiplication_x: Error while adding quote: " + e.getCause() +  e.getMessage()).queue();
							e.printStackTrace();
							break;
						}
					case "random":
						int quoteN = rand.nextInt(quotes.size() - 1);
						String[] quoteElements = quotes.get(quoteN).split("====");
						EmbedBuilder embedBuilder = new EmbedBuilder();
						Date dat = new Date(Long.parseLong(quoteElements[5]));
						embedBuilder.setAuthor(quoteElements[1] + " said:", null, quoteElements[2])
								.setThumbnail(quoteElements[2])
								.setColor(Color.CYAN)
								.setDescription("Quote made on server " + quoteElements[4]
										+ " in channel " + "#" + quoteElements[3])
								.addField("Content", quoteElements[0], false)
								.setFooter("Date: " + dateFormat.format(dat), null);
						channel.sendMessage(embedBuilder.build()).queue();
						break;
					case "read":
						int i = Integer.parseInt(phrase);
						String[] quoteElements2 = quotes.get(i).split("====");
						System.out.println(quoteElements2[5]);
						EmbedBuilder embedBuilder2 = new EmbedBuilder();
						System.out.println(quoteElements2[2]);
						Date date1 = new Date(Long.parseLong(quoteElements2[5]));
						embedBuilder2.setAuthor(quoteElements2[1] + " said:", null, quoteElements2[2])
								.setThumbnail(quoteElements2[2])
								.setColor(Color.CYAN)
								.setDescription("Quote made on server " + quoteElements2[4]
										+ " in channel " + "#" + quoteElements2[3])
								.addField("Content", quoteElements2[0], false)
								.setFooter("Date: " + dateFormat.format(date1), null);
						channel.sendMessage(embedBuilder2.build()).queue();
						break;
					case "addfrom":
						int i1 = 0;
						Message m = event.getMessage();
						for(Message m1 : messageHistory){
							if(m1.getContent().contains(phrase) && !m1.getContent().startsWith(Parameters.getPrefixForServer("default")) && !m1.getContent().startsWith(Parameters.getPrefixForServer(m1.getGuild().getId()))){
								m = messageHistory.get(i1);
								break;
							}
							i1++;
						}
						//[0]content====[1]authorname====[2]authoravatar====[3]channelname====[4]guildname====[5]quotetime, will be read like this to create the embed if called later.
						String out = m.getContent() + "====" + m.getAuthor().getName() + "====" + m.getAuthor().getAvatarUrl() +
								"====" + m.getChannel().getName() + "====" + m.getGuild().getName() + "====" + System.currentTimeMillis();
						quotes.add(out);
						Date quoteDate = new Date(System.currentTimeMillis());
						new StringArrayUtils("quote", quotes, true, true);
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
						for(int i2 = 0; i2 < quotes.size() - 1; i2++){
							if(quotes.get(i2).contains(phrase)){
								String[] quoteE = quotes.get(i2).split("====");
								Date date = new Date(Long.parseLong(quoteE[5]));
								EmbedBuilder builder2 = new EmbedBuilder();
								builder2.setAuthor(quoteE[1] + " said:", null, quoteE[2])
										.setThumbnail(quoteE[2])
										.setColor(Color.CYAN)
										.setDescription("Quote made on server " + quoteE[4]
												+ " in channel " + "#" + quoteE[3])
										.addField("Content", quoteE[0], false)
										.setFooter("Date: " + dateFormat.format(date), null);
								channel.sendMessage(builder2.build()).queue();
								break;
							}
						}
				}
			}

			@Override
			public String help() {
				return  "> Usage:\n"
						+ "~>quote add [number]: Adds a quote with content defined by the number. For example 1 will quote the last message.\n"
						+ "~>quote random: Gets a random quote. \n"
						+ "~>quote read [number]: Gets a quote matching the number. \n"
						+ "~>quote addfrom [phrase] Adds a quote based in text search criteria.\n"
						+ "~>quote getfrom [phrase]: Searches for the first quote which matches your search criteria and prints it.\n"
						+ "> Parameters:\n"
						+ "[number]: Message number to quote. For example 1 will quote the last message.\n"
						+ "[phrase]: A part of the quote phrase.";
			}

			@Override
			public CommandType commandType() {
				return CommandType.USER;
			}
		});
	}
}