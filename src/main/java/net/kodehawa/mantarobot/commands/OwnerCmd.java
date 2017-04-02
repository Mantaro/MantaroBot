package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.extensions.Async;
import bsh.Interpreter;
import com.rethinkdb.gen.exc.ReqlError;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.lib.mantarolang.CompiledFunction;
import net.kodehawa.lib.mantarolang.MantaroLang;
import net.kodehawa.lib.mantarolang.objects.LangObject;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroShard;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.DBGuild;
import net.kodehawa.mantarobot.data.entities.DBUser;
import net.kodehawa.mantarobot.data.entities.MantaroObj;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

import static br.com.brjdevs.java.utils.extensions.CollectionUtils.random;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

public class OwnerCmd extends Module {
	private interface Evaluator {
		Object eval(GuildMessageReceivedEvent event, String code);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("Owner");
	private final String[] sleepQuotes = {"*goes to sleep*", "Mama, It's not night yet. *hmph*. okay. bye.", "*grabs pillow*", "*~~goes to sleep~~ goes to dreaming dimension*", "*grabs plushie*", "Momma, where's my Milk cup? *drinks and goes to sleep*"};

	public OwnerCmd() {
		super(Category.OWNER);
		blacklist();
		owner();
	}

	private void blacklist() {
		super.register("blacklist", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				MantaroObj obj = MantaroData.db().getMantaroData();
				if (args[0].equals("guild")) {
					if (args[1].equals("add")) {
						if (MantaroBot.getInstance().getGuildById(args[2]) == null) return;
						obj.getBlackListedGuilds().add(args[2]);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Blacklisted Guild: " + event.getJDA().getGuildById(args[2])).queue();
						obj.save();
					} else if (args[1].equals("remove")) {
						if (!obj.getBlackListedGuilds().contains(args[2])) return;
						obj.getBlackListedGuilds().remove(args[2]);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Unblacklisted Guild: " + args[2]).queue();
						obj.save();
					}
					return;
				}

				if (args[0].equals("user")) {
					if (args[1].equals("add")) {
						if (MantaroBot.getInstance().getUserById(args[2]) == null) return;
						obj.getBlackListedUsers().add(args[2]);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Blacklisted User: " + event.getJDA().getUserById(args[2])).queue();
						obj.save();
					} else if (args[1].equals("remove")) {
						if (!obj.getBlackListedUsers().contains(args[2])) return;
						obj.getBlackListedUsers().remove(args[2]);
						event.getChannel().sendMessage(EmoteReference.CORRECT + "Unblacklisted User: " + event.getJDA().getUserById(args[2])).queue();
						obj.save();
					}
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Blacklist command")
					.setDescription("Blacklists a user (user argument) or a guild (guild argument) by id.")
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}
		});
	}

	public CompletableFuture<Void> notifyMusic(String content) {
		return CompletableFuture.allOf(MantaroBot.getInstance().getAudioManager().getMusicManagers().values()
			.stream()
			.filter(musicManager -> musicManager.getTrackScheduler().getCurrentTrack() != null)
			.filter(musicManager -> musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel() != null)
			.filter(musicManager -> musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel().canTalk())
			.map(musicManager -> musicManager.getTrackScheduler().getCurrentTrack().getRequestedChannel().sendMessage(content).submit())
			.map(future -> (CompletableFuture<Message>) future)
			.toArray(CompletableFuture[]::new));
	}

	private void owner() {
		Map<String, Evaluator> evals = new HashMap<>();
		evals.put("js", (event, code) -> {
			ScriptEngine script = new ScriptEngineManager().getEngineByName("nashorn");
			script.put("jda", event.getJDA());
			script.put("event", event);
			script.put("guild", event.getGuild());
			script.put("channel", event.getChannel());

			try {
				return script.eval(String.join("\n",
					"load(\"nashorn:mozilla_compat.js\");",
					"imports = new JavaImporter(java.util, java.io, java.net);",
					"(function() {",
					"with(imports) {",
					code,
					"}",
					"})()"
				));
			} catch (Exception e) {
				return e;
			}
		});

		evals.put("bsh", (event, code) -> {
			Interpreter interpreter = new Interpreter();
			try {
				interpreter.set("jda", event.getJDA());
				interpreter.set("event", event);
				interpreter.set("guild", event.getGuild());
				interpreter.set("channel", event.getChannel());

				return interpreter.eval(String.join("\n",
					"import *;",
					code
				));
			} catch (Exception e) {
				return e;
			}
		});

		evals.put("groovy", (event, code) -> {
			Binding b = new Binding();
			b.setVariable("jda", event.getJDA());
			b.setVariable("event", event);
			b.setVariable("guild", event.getGuild());
			b.setVariable("channel", event.getChannel());
			GroovyShell sh = new GroovyShell(b);
			try {
				return sh.evaluate(code);
			} catch (Exception e) {
				return e;
			}
		});

		evals.put("m", (event, code) -> {
			OptionalLong compileTime = OptionalLong.empty();
			OptionalLong executeTime = OptionalLong.empty();
			Object r;
			try {
				CompiledFunction<Pair<Long, List<LangObject>>> compiledFunction = new MantaroLang().compile(code);
				compileTime = OptionalLong.of(compiledFunction.timeTook());

				Pair<Long, List<LangObject>> run = compiledFunction.run();
				executeTime = OptionalLong.of(run.getKey());

				List<LangObject> returnList = run.getRight();

				r = returnList.isEmpty() ? null : returnList.size() == 1 ? returnList.get(0) : returnList;
			} catch (Exception e) {
				r = e;
			}

			OptionalLong runningTime = executeTime;
			compileTime.ifPresent(l -> event.getChannel().sendMessage("**MantaroLang Debug**\n**Compile Time**: " + l + " ms" + (runningTime.isPresent() ? "\n**Executing Time**: " + runningTime.orElse(0) + " ms" : "")).queue());

			return r;
		});

		evals.put("cw", (event, code) -> {
            Object[] returns;
            boolean errored = false;
            try {
                returns = MantaroData.connectionWatcher().eval(code);
            } catch(RuntimeException e) {
                errored = true;
                returns = new Object[]{e.getMessage()};
            }
            String result = returns.length == 1 ? returns[0] == null ? null : String.valueOf(returns[0]) : Arrays.asList(returns).toString();
            if(errored) return new Error(result == null ? "Internal error" : result) {
                @Override
                public String toString() {
                    return getMessage();
                }
            };
            return result;
        });

		super.register("owner", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (args.length < 1) {
					onHelp(event);
					return;
				}

				String option = args[0];

				if (option.equals("cw")){
				   if(args.length < 2) {
				       onHelp(event);
				       return;
                   }

                   String sub = args[1].substring(0, args[1].indexOf(' '));
				   if(sub.equals("info")) {
				       event.getChannel().sendMessage(new EmbedBuilder()
                            .setAuthor("Connection Watcher info", null, null)
                            .setDescription(MantaroData.connectionWatcher().get().toString())
                            .setColor(event.getGuild().getSelfMember().getColor())
                            .setFooter("Asked by: " + event.getAuthor().getName(), null)
                       .build()).queue();
                   } else if(sub.equals("eval")) {
				       String[] parts = event.getMessage().getRawContent().split(" ");
				       if(parts.length < 4) {
				           onHelp(event);
				           return;
                       }
				       Object[] returns;
				       boolean errored = false;
				       try {
                           returns = MantaroData.connectionWatcher().eval(String.join(" ", Arrays.copyOfRange(parts, 3, parts.length)));
                       } catch(RuntimeException e) {
				           errored = true;
				           returns = new Object[]{e.getMessage()};
                       }
                       String result = returns.length == 1 ? returns[0] == null ? null : String.valueOf(returns[0]) : Arrays.asList(returns).toString();
                       event.getChannel().sendMessage(new EmbedBuilder()
                               .setAuthor("Evaluated " + (errored ? "and errored" : "with success"), null, event.getAuthor().getAvatarUrl())
                               .setColor(errored ? Color.RED : Color.GREEN)
                               .setDescription(result == null ? "Executed successfully with no objects returned" : ("Executed " + (errored ? "and errored: " : "successfully and returned: ") + result))
                               .setFooter("Asked by: " + event.getAuthor().getName(), null)
                               .build()
                       ).queue();
                   } else {
                       onHelp(event);
                   }

                   return;
                }

                if(option.equals("premium")){
					String sub = args[1].substring(0, args[1].indexOf(' '));
					if(sub.equals("add")){
						try{
							String[] values = SPLIT_PATTERN.split(args[1], 3);
							DBUser db = MantaroData.db().getUser(values[1]);
							db.incrementPremium(TimeUnit.DAYS.toMillis(Long.parseLong(values[2])));
							db.saveAsync();
							event.getChannel().sendMessage(EmoteReference.CORRECT +
									"The premium feature for user " + db.getId() + " now is until " +
									new Date(db.getPremiumUntil())).queue();
							return;
						} catch (IndexOutOfBoundsException e){
							event.getChannel().sendMessage(
									EmoteReference.ERROR + "You need to specify id and number of days").queue();
							e.printStackTrace();
							return;
						}
					}

					if(sub.equals("guild")){
						try{
							String[] values = SPLIT_PATTERN.split(args[1], 3);
							DBGuild db = MantaroData.db().getGuild(values[1]);
							db.incrementPremium(TimeUnit.DAYS.toMillis(Long.parseLong(values[2])));
							db.saveAsync();
							event.getChannel().sendMessage(EmoteReference.CORRECT +
									"The premium feature for guild " + db.getId() + " now is until " +
									new Date(db.getPremiumUntil())).queue();
							return;
						} catch (IndexOutOfBoundsException e){
							event.getChannel().sendMessage(
									EmoteReference.ERROR + "You need to specify id and number of days").queue();
							e.printStackTrace();
							return;
						}
					}
				}

				if (option.equals("shutdown") || option.equals("restart")) {
					if (args.length == 2) {
						try {
							notifyMusic(args[1]).get();
						} catch (InterruptedException | ExecutionException ignored) {
						}
					}

					try {
						prepareShutdown(event);
					} catch (Exception e) {
						LOGGER.warn(EmoteReference.ERROR + "Couldn't prepare shutdown." + e.toString(), e);
						return;
					}

					//If we manage to get here, there's nothing else except us.

					//Here in Darkness, everything is okay.
					//Listen to the waves, and let them fade away.

					System.exit(option.equals("restart") ? 15 : 0);
					return;
				}

				if (option.equals("forceshutdown") || option.equals("forcerestart")) {
					if (args.length == 2) {
						try {
							notifyMusic(args[1]).get();
						} catch (InterruptedException | ExecutionException ignored) {
						}
					}

					try {
						prepareShutdown(event);
					} catch (Exception e) {
						LOGGER.warn(EmoteReference.ERROR + "Couldn't prepare shutdown. I don't care, I'm gonna restart anyway." + e.toString(), e);
					}

					//If we manage to get here, there's nothing else except us.

					//Here in Darkness, everything is okay.
					//Listen to the waves, and let them fade away.

					System.exit(option.equals("forcerestart") ? 15 : 0);
					return;
				}

				if (args.length < 2) {
					onHelp(event);
					return;
				}

				String value = args[1];

				if (option.equals("notifymusic")) {
					notifyMusic(value);
					event.getChannel().sendMessage(EmoteReference.MEGA + "Guilds playing music were notified!").queue();
					return;
				}

				if (option.equals("error")) {
					switch (value) {
						case "bounds":
							throw new IndexOutOfBoundsException("PLEASE IGNORE ME");
						case "rethonk":
							throw new ReqlError("PLEASE IGNORE ME");
						case "args":
							throw new IllegalArgumentException("PLEASE IGNORE ME");
						case "perms":
							throw new PermissionException("PLEASE IGNORE ME");
						case "generic":
							throw new RuntimeException("PLEASE IGNORE ME");
					}
				}

				String[] values = SPLIT_PATTERN.split(value, 2);
				if (values.length < 2) {
					onHelp(event);
					return;
				}

				String k = values[0], v = values[1];

				if (option.equals("scheduleshutdown") || option.equals("schedulerestart")) {
					boolean restart = option.equals("schedulerestart");
					if (k.equals("time")) {
						double s = Double.parseDouble(v);
						int millis = (int) (s * 1000);
						Async.thread(millis, () -> {
							try {
								prepareShutdown(event);
							} catch (Exception e) {
								LOGGER.warn(EmoteReference.ERROR + "Couldn't prepare shutdown. I don't care, I'm gonna restart anyway." + e.toString(), e);
							}
							System.exit(restart ? 15 : 0);
						});

						event.getChannel().sendMessage(EmoteReference.STOPWATCH + " Sleeping in " + s + " seconds...").queue();
						return;
					}

					if (k.equals("connections")) {
						int connections = Integer.parseInt(v);

						IntSupplier currentConnections = () -> (int) event.getJDA().getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(
							voiceChannel.getGuild().getSelfMember())).count();

						Async.task("Watching Thread.", s -> {
							if (currentConnections.getAsInt() > connections) return;

							try {
								prepareShutdown(event);
							} catch (Exception e) {
								LOGGER.warn("Couldn't prepare shutdown. I don't care, I'm gonna do it anyway." + e.toString(), e);
							}

							System.exit(restart ? 15 : 0);
							s.shutdown();
						}, 2);
						return;
					}

					onHelp(event);
					return;
				}

				if (option.equals("varadd")) {
					try {
						String v1 = values[1];
						switch (values[0]) {
							case "pat":
								ActionCmds.PATS.get().add(v1);
								ActionCmds.PATS.save();
								event.getChannel().sendMessage(EmoteReference.CORRECT + "Added to pat list: " + v).queue();
								break;
							case "hug":
								ActionCmds.HUGS.get().add(v1);
								ActionCmds.HUGS.save();
								event.getChannel().sendMessage(EmoteReference.CORRECT + "Added to hug list: " + v).queue();
								break;
							case "greeting":
								ActionCmds.GREETINGS.get().add(content.replace("varadd greeting ", ""));
								ActionCmds.GREETINGS.save();
								event.getChannel().sendMessage(EmoteReference.CORRECT + "Added to greet list: " + content.replace("greeting ", "")).queue();
								break;
							case "splash":
								MantaroShard.SPLASHES.get().add(content.replace("varadd splash ", ""));
								MantaroShard.SPLASHES.save();
								event.getChannel().sendMessage(EmoteReference.CORRECT + "Added to splash list: " + content.replace("splash ", "")).queue();
								break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					return;
				}

				if (option.equals("eval")) {
					Evaluator evaluator = evals.get(k);
					if (evaluator == null) {
						onHelp(event);
						return;
					}

					Object result = evaluator.eval(event, v);
					boolean errored = result instanceof Throwable;

					event.getChannel().sendMessage(new EmbedBuilder()
						.setAuthor("Evaluated " + (errored ? "and errored" : "with success"), null, event.getAuthor().getAvatarUrl())
						.setColor(errored ? Color.RED : Color.GREEN)
						.setDescription(result == null ? "Executed successfully with no objects returned" : ("Executed " + (errored ? "and errored: " : "successfully and returned: ") + result.toString()))
						.setFooter("Asked by: " + event.getAuthor().getName(), null)
						.build()
					).queue();

					return;
				}

				onHelp(event);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Owner command")
					.setDescription("~>owner shutdown/forceshutdown: Shutdowns the bot\n" +
						"~>owner restart/forcerestart: Restarts the bot.\n" +
						"~>owner scheduleshutdown time <time>: Schedules a fixed amount of seconds the bot will wait to be shutted down.\n" +
						"~>owner varadd <pat/hug/greeting/splash>: Adds a link or phrase to the specified list.\n" +
						"~>owner eval <bsh/js/groovy/m/cw> <line of code>: Evals a specified code snippet.\n" +
                        "~>owner cw <info/eval>: Shows info or evals specified code in the Connection Watcher.\n" +
						"~>owner premium add <id> <days>: Adds premium to the specified user for x days.")
					.addField("Shush.", "If you aren't Adrian or Kode you shouldn't be looking at this, huh " + EmoteReference.EYES, false)
					.build();
			}

			@Override
			public CommandPermission permissionRequired() {
				return CommandPermission.BOT_OWNER;
			}

			@Override
			protected String[] splitArgs(String content) {
				return SPLIT_PATTERN.split(content, 2);
			}
		});
	}

	private void prepareShutdown(GuildMessageReceivedEvent event) {
		MantaroBot.getInstance().getAudioManager().getMusicManagers().forEach((s, musicManager) -> {
			if (musicManager.getTrackScheduler() != null) musicManager.getTrackScheduler().stop();
		});

		try {
		    MantaroData.connectionWatcher().close();
        } catch(Exception e) {}

		Arrays.stream(MantaroBot.getInstance().getShards()).forEach(MantaroShard::prepareShutdown);

		event.getChannel().sendMessage(random(sleepQuotes)).complete();

		Arrays.stream(MantaroBot.getInstance().getShards()).forEach(mantaroShard -> mantaroShard.getJDA().shutdown(true));
	}
}
