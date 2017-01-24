package net.kodehawa.mantarobot.cmd;

import bsh.Interpreter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.module.SimpleCommand;
import net.kodehawa.mantarobot.util.StringArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Owner extends Module {

	private synchronized void shutdown(GuildMessageReceivedEvent event){
		Mantaro.getSelf().getRegisteredListeners().forEach(listener -> Mantaro.getSelf().removeEventListener(listener));
		System.gc();
		event.getChannel().sendMessage("*goes to sleep*").queue();
		System.exit(0);
	}

	private static Logger LOGGER = LoggerFactory.getLogger("Owner");
	public static GuildMessageReceivedEvent tempEvt = null;

	public Owner() {
		super(Category.MODERATION);
		this.registerCommands();
	}

	@Override
	public void registerCommands() {
		super.register("add", "Adds a item to a list.", new SimpleCommand() {
			@Override
			public CommandType commandType() {
				return CommandType.OWNER;
			}

			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(Mantaro.OWNER_ID)) {
					String noArgs = content.split(" ")[0];
					switch (noArgs) {
						case "greeting":
							String greet = content.replace("greeting" + " ", "");
							Action.greeting.add(greet);
							new StringArrayUtils("greeting", Action.greeting, true, true);
							event.getChannel().sendMessage(":speech_balloon:" + "Added to greeting list: " + greet);
							break;
						case "tsun":
							String tsun = content.replace("tsun" + " ", "");
							Action.tsunLines.add(tsun);
							new StringArrayUtils("tsunderelines", Action.tsunLines, true, true);
							event.getChannel().sendMessage(":speech_balloon:" + "Added to tsundere list: " + tsun);
							break;
						default:
							event.getChannel().sendMessage(":speech_balloon:" + "Silly master, use ~>add greeting or ~>add tsun");
							break;
					}
				} else {
					event.getChannel().sendMessage(":heavy_multiplication_x:" + "How did you even know?");
				}
			}

			@Override
			public String help() {
				return "";
			}

		});

		super.register("eval", "Evaluates arbitrary code.", new SimpleCommand() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!event.getAuthor().getId().equals(Mantaro.OWNER_ID)) {
					return;
				}
				tempEvt = event;
				try {
					Interpreter interpreter = new Interpreter();
					String evalHeader =
							"import *; "
									+ "private Mantaro bot = new Mantaro(); "
									+ "private JDAImpl self = Mantaro.getSelf(); "
									+ "private GuildMessageReceivedEvent evt = net.kodehawa.mantarobot.cmd.Owner.tempEvt;";
					Object toSendTmp = interpreter.eval(evalHeader + content.replaceAll("#", "().") + ";");
					EmbedBuilder embed = new EmbedBuilder();
					if (toSendTmp != null) {
						String toSend = toSendTmp.toString();
						embed.setAuthor("Executed eval with success", null, event.getAuthor().getAvatarUrl())
								.setDescription("Returned: " + toSend)
								.setFooter("Asked by: " + event.getAuthor().getName(), null);
					} else {
						embed.setAuthor("Executed eval with success", null, event.getAuthor().getAvatarUrl())
								.setDescription("No returns.")
								.setFooter("Asked by: " + event.getAuthor().getName(), null);
					}
					event.getChannel().sendMessage(embed.build()).queue();
				} catch (Exception e) {
					event.getChannel().sendMessage("There was a problem with code evaluation. Check logs.").queue();
					LOGGER.warn("Problem evaluating code!", e);
					e.printStackTrace();
				}
			}

			@Override
			public String help() {
				return "";
			}

			@Override
			public CommandType commandType() {
				return CommandType.OWNER;
			}
		});

		super.register("shutdown", "Shuts down the bot.", new SimpleCommand() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (!event.getAuthor().getId().equals(Mantaro.OWNER_ID)) {
					event.getChannel().sendMessage("Seems like you cannot do that, you silly <3").queue();
					return;
				}

				try {
					shutdown(event);
				} catch (Exception e) {
					LOGGER.warn("Couldn't shut down." + e.toString(), e);
				}
			}

			@Override
			public String help() {
				return "";
			}

			@Override
			public CommandType commandType() {
				return CommandType.OWNER;
			}
		});
	}
}
