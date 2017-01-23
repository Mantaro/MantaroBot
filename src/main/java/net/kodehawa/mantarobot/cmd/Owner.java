package net.kodehawa.mantarobot.cmd;

import bsh.Interpreter;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.listeners.Listener;
import net.kodehawa.mantarobot.log.Log;
import net.kodehawa.mantarobot.log.Type;
import net.kodehawa.mantarobot.module.Callback;
import net.kodehawa.mantarobot.module.Category;
import net.kodehawa.mantarobot.module.CommandType;
import net.kodehawa.mantarobot.module.Module;
import net.kodehawa.mantarobot.util.StringArrayUtils;

public class Owner extends Module {

	public static MessageReceivedEvent tempEvt = null;

	public Owner() {
		super.setCategory(Category.MODERATION);
		this.registerCommands();
	}

	@Override
	public void registerCommands() {
		super.register("add", "Adds a item to a list.", new Callback() {
			@Override
			public CommandType commandType() {
				return CommandType.OWNER;
			}

			@Override
			public String help() {
				return "";
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

		});

		super.register("eval", "Evaluates arbitrary code.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(Mantaro.OWNER_ID)) {
					try {
						Interpreter interpreter = new Interpreter();
						String evalHeader =
							"import *; "
								+ "private Mantaro bot = Mantaro.instance(); "
								+ "private JDAImpl self = Mantaro.instance().getSelf(); "
								+ "private MessageReceivedEvent evt = net.kodehawa.mantarobot.cmd.Owner.tempEvt;";
						Object toSendTmp = interpreter.eval(evalHeader + content.replaceAll("#", "().") + ";");
						if (toSendTmp != null) {
							String toSend = toSendTmp.toString();
							event.getChannel().sendMessage(toSend).queue();
						}
					} catch (Exception e) {
						Log.instance().print("Problem evaluating code!", this.getClass(), Type.WARNING, e);
						e.printStackTrace();
					}
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

		super.register("shutdown", "Shuts down the bot.", new Callback() {
			@Override
			public void onCommand(String[] args, String content, GuildMessageReceivedEvent event) {
				if (event.getAuthor().getId().equals(Mantaro.OWNER_ID)) {
					event.getChannel().sendMessage("Gathering information...").queue();
					try {
						Thread.sleep(500);
					} catch (InterruptedException ignored) {
					}

					event.getChannel().sendMessage("Gathered.").queue();

					event.getChannel().sendMessage("Starting bot shutdown...").queue();
					try {
						Action.tsunLines.clear();
						System.gc();
						Thread.sleep(500);
					} catch (InterruptedException ignored) {
					}

					event.getChannel().sendMessage("*goes to sleep*").queue();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ignored) {
					}

					try {
						Mantaro.instance().getSelf().removeEventListener(new Listener());
						System.exit(1);
					} catch (Exception e) {
						Mantaro.instance().getSelf().addEventListener(new Listener());
						Log.instance().print(":heavy_multiplication_x: " + "Couldn't shut down." + e.toString(), this.getClass(), Type.CRITICAL, e);
					}
				} else {
					event.getChannel().sendMessage(":heavy_multiplication_x:" + "You cannot do that, silly.").queue();
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
