package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.extensions.CollectionUtils;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.action.ImageActionCmd;
import net.kodehawa.mantarobot.commands.action.TextActionCmd;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Event;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

@Module
public class ActionCmds {
	public static final DataManager<List<String>> BLEACH = new SimpleFileDataManager("assets/mantaro/texts/bleach.txt");
	public static final DataManager<List<String>> GREETINGS = new SimpleFileDataManager("assets/mantaro/texts/greetings.txt");
	public static final DataManager<List<String>> HUGS = new SimpleFileDataManager("assets/mantaro/texts/hugs.txt");
	public static final DataManager<List<String>> KISSES = new SimpleFileDataManager("assets/mantaro/texts/kisses.txt");
	public static final DataManager<List<String>> PATS = new SimpleFileDataManager("assets/mantaro/texts/pats.txt");
	public static final DataManager<List<String>> TSUNDERE = new SimpleFileDataManager("assets/mantaro/texts/tsundere.txt");

	@Event
	public static void action(CommandRegistry registry) {
		registry.register("action", new SimpleCommand(Category.ACTION) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				String noArgs = content.split(" ")[0];
				TextChannel channel = event.getChannel();
				switch (noArgs) {
					case "facedesk":
						channel.sendMessage("http://puu.sh/rK6E7/0b745e5544.gif").queue();
						break;
					case "nom":
						channel.sendMessage("http://puu.sh/rK7t2/330182c282.gif").queue();
						break;
					case "bleach":
						channel.sendMessage(CollectionUtils.random(BLEACH.get())).queue();
						break;
					default:
						onError(event);
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Action commands")
					.addField("Description:", "~>action bleach: Random bleach picture.\n" +
						"~>action facedesk: When you really mess up.\n" +
						"~>action nom: nom nom.", false)
					.setColor(Color.PINK)
					.build();
			}
		});
	}

	@Event
	public static void bloodsuck(CommandRegistry registry) {
		registry.register("bloodsuck", new SimpleCommand(Category.ACTION) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (event.getMessage().getMentionedUsers().isEmpty()) {
					event.getChannel().sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/ZR8Plmd.png"), "suck.png", null).queue();
				} else {
					String bString = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors
						.joining(" "));
					String bs = String.format(EmoteReference.TALKING + "%s has sucked the blood of %s", event.getAuthor().getAsMention(),
						bString);
					event.getChannel().sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/ZR8Plmd.png"), "suck.png",
						new MessageBuilder().append(bs).build()).queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Bloodsuck")
					.setDescription("Suck the blood of the mentioned members")
					.build();
			}
		});
	}

	@Event
	public static void lewd(CommandRegistry registry) {
		registry.register("lewd", new SimpleCommand(Category.ACTION) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				String lood = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
				event.getChannel().sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/LJfZYau.png"), "lewd.png"
					, new MessageBuilder().append(lood).append(" Y-You lewdie!").build()).queue();
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Lewd")
					.setDescription("lewd").build();
			}
		});
	}

	@Event
	public static void meow(CommandRegistry registry) {
		registry.register("meow", new SimpleCommand(Category.ACTION) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				Message receivedMessage = event.getMessage();
				if (!receivedMessage.getMentionedUsers().isEmpty()) {
					String mew = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
					event.getChannel().sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/yFGHvVR.gif"), "mew.gif",
						new MessageBuilder().append(EmoteReference.TALKING).append(String.format("*is meowing at %s.*", mew)).build()).queue();
				} else {
					event.getChannel().sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/yFGHvVR.gif"), "mew.gif",
						new MessageBuilder().append(":speech_balloon: Meow.").build()).queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Meow command")
					.setDescription("Meow either to a person or the sky.")
					.setColor(Color.cyan)
					.build();
			}
		});
		registry.registerAlias("mew", "meow");
	}

	@Event
	public static void register(CommandRegistry cr) {

		//pat();
		cr.register("pat", new ImageActionCmd(
			"Pat", "Pats the specified user.", Color.PINK,
			"pat.gif", EmoteReference.TALKING + "%s you have been patted by %s", PATS.get()));

		//hug();
		cr.register("hug", new ImageActionCmd(
			"Hug", "Hugs the specified user.", Color.PINK,
			"hug.gif", EmoteReference.TALKING + "%s you have been hugged by %s", HUGS.get()
		));

		//kiss();
		cr.register("kiss", new ImageActionCmd(
			"Kiss", "Kisses the specified user.", Color.PINK,
			"kiss.gif", EmoteReference.TALKING + "%s you have been kissed by %s", KISSES.get()
		));

		//greet();
		cr.register("greet", new TextActionCmd(
			"Greet", "Sends a random greeting", Color.DARK_GRAY,
			EmoteReference.TALKING + "%s", GREETINGS.get()
		));

		//tsundere();
		cr.register("tsundere", new TextActionCmd(
			"Tsundere Command", "Y-You baka!", Color.PINK,
			EmoteReference.MEGA + "%s", TSUNDERE.get()
		));

	}
}
