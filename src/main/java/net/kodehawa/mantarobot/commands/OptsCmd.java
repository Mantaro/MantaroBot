package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.Option;
import net.kodehawa.mantarobot.options.OptionType;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Map.Entry;
import static net.kodehawa.mantarobot.utils.Utils.centerString;
import static net.kodehawa.mantarobot.utils.Utils.mapObjects;

@Module
public class OptsCmd {
	public static net.kodehawa.mantarobot.core.modules.commands.base.Command optsCmd;

	@Subscribe
	public void register(CommandRegistry registry) {
		registry.register("opts", optsCmd = new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (args.length == 1 && args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")) {
					Queue<Message> toSend = new MessageBuilder()
							.append("```prologn```") //Trick to make it count the end and start of the formatting
							.append(String.format("%-34s | %s \n", centerString("Name", 34), centerString("Description", 60)))
							.append(centerString("** ------------------- **", 75))
							.append("\n")
							.append(Option.getAvaliableOptions().stream().collect(Collectors.joining("\n")))
							.buildAll(MessageBuilder.SplitPolicy.NEWLINE);

					toSend.forEach(message -> event.getChannel().sendMessage("```prolog\n" +
							message.getContent().replace("```prologn```", "")
							+ "```").queue());

					return;
				}

				if (args.length < 2) {
					event.getChannel().sendMessage(help(event)).queue();
					return;
				}

                StringBuilder name = new StringBuilder();

				if (args[0].equalsIgnoreCase("help")) {
					for (int i = 1; i < args.length; i++) {
						String s = args[i];
						if (name.length() > 0) name.append(":");
						name.append(s);
						Option option = Option.getOptionMap().get(name.toString());

						if (option != null) {
							try{
								EmbedBuilder builder = new EmbedBuilder()
										.setAuthor(option.getOptionName(), null, event.getAuthor().getEffectiveAvatarUrl())
										.setDescription(option.getDescription())
										.setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
										.addField("Type", option.getType().toString(), false);
								event.getChannel().sendMessage(builder.build()).queue();
							} catch (IndexOutOfBoundsException ignored) {}
							return;
						}
					}
					event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid option help name.").queue(
							message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
					);

					return;
				}

                for (int i = 0; i < args.length; i++) {
					String s = args[i];
					if (name.length() > 0) name.append(":");
					name.append(s);
					Option option = Option.getOptionMap().get(name.toString());

					if (option != null) {
						BiConsumer<GuildMessageReceivedEvent, String[]> callable = Option.getOptionMap().get(name.toString()).getEventConsumer();
						try{
							String[] a;
							if (++i < args.length) a = Arrays.copyOfRange(args  , i, args.length);
							else a = new String[0];
							callable.accept(event, a);
						} catch (IndexOutOfBoundsException ignored) {}
						return;
					}
				}

				event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid option or arguments.").queue(
						message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
				);
				event.getChannel().sendMessage(help(event)).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Options and Configurations Command")
					.setDescription("**This command allows you to change Mantaro settings for this server.**\n" +
						"All values set are local rather than global, meaning that they will only effect this server.")
					.addField("Usage", "The command is so big that we moved the description to the wiki. [Click here](https://github.com/Mantaro/MantaroBot/wiki/Configuration) to go to the Wiki Article.", false)
					.build();
			}
		}).addOption("check:data", new Option("Data check.",
				"Checks the data values you have set on this server. **THIS IS NOT USER-FRIENDLY**", OptionType.GENERAL)
		.setAction(event -> {
			DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
			GuildData guildData = dbGuild.getData();
			//Map as follows: name, value
			Map<String, Object> fieldMap = mapObjects(guildData);

			if(fieldMap == null) {
				event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot retrieve values. Weird thing...").queue();
				return;
			}

			StringBuilder show = new StringBuilder();
			show.append("Options set for server **")
					.append(event.getGuild().getName())
					.append("**\n\n");

			AtomicInteger ai = new AtomicInteger();

			for(Entry e : fieldMap.entrySet()) {
				show.append(ai.incrementAndGet())
						.append(".- `")
						.append(e.getKey())
						.append("`");

				if(e.getValue() == null) {
					show.append(" **is not set to anything.")
							.append("**\n");
				} else {
					show.append(" is set to: **")
							.append(e.getValue())
							.append("**\n");
				}
			}

			Queue<Message> toSend = new MessageBuilder().append(show.toString()).buildAll(MessageBuilder.SplitPolicy.NEWLINE);
			toSend.forEach(message -> event.getChannel().sendMessage(message).queue());
		}).setShortDescription("Checks the data values you have set on this server."));
	}

	public static void onHelp(GuildMessageReceivedEvent event) {
		event.getChannel().sendMessage(optsCmd.help(event)).queue();
	}

	public static SimpleCommand getOpts() {
		return (SimpleCommand) optsCmd;
	}
}