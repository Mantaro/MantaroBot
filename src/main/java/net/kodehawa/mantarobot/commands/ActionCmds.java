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
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public class ActionCmds extends Module {
    public static final DataManager<List<String>> BLEACH = new SimpleFileDataManager("assets/mantaro/texts/bleach.txt");
    public static final DataManager<List<String>> GREETINGS = new SimpleFileDataManager("assets/mantaro/texts/greetings.txt");
    public static final DataManager<List<String>> HUGS = new SimpleFileDataManager("assets/mantaro/texts/hugs.txt");
    public static final DataManager<List<String>> KISSES = new SimpleFileDataManager("assets/mantaro/texts/kisses.txt");
    public static final DataManager<List<String>> PATS = new SimpleFileDataManager("assets/mantaro/texts/pats.txt");
    public static final DataManager<List<String>> TSUNDERE = new SimpleFileDataManager("assets/mantaro/texts/tsundere.txt");

    public ActionCmds() {
        super(Category.ACTION);

        //pat();
        super.register("pat", new ImageActionCmd(
                "Pat", "Pats the specified user.", Color.PINK,
                "pat.gif", EmoteReference.TALKING + "%s you have been patted by %s", PATS.get()));

        //hug();
        super.register("hug", new ImageActionCmd(
                "Hug", "Hugs the specified user.", Color.PINK,
                "hug.gif", EmoteReference.TALKING + "%s you have been hugged by %s", HUGS.get()
        ));

        //kiss();
        super.register("kiss", new ImageActionCmd(
                "Kiss", "Kisses the specified user.", Color.PINK,
                "kiss.gif", EmoteReference.TALKING + "%s you have been kissed by %s", KISSES.get()
        ));

        //greet();
        super.register("greet", new TextActionCmd(
                "Greet", "Sends a random greeting", Color.DARK_GRAY,
                EmoteReference.TALKING + "%s", GREETINGS.get()
        ));

        //tsundere();
        super.register("tsundere", new TextActionCmd(
                "Tsundere Command", "Y-You baka!", Color.PINK,
                EmoteReference.MEGA + "%s", TSUNDERE.get()
        ));

        action();
        meow();
        bloodsuck();
        lewd();
    }

    private void action() {
        super.register("action", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
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
                        onHelp(event);
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Action commands")
                        .addField("Description:", "~>action bleach: Random image of someone drinking bleach.\n" +
                                "~>action facedesk: Facedesks.\n" +
                                "~>action nom: nom nom.", false)
                        .setColor(Color.PINK)
                        .build();
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.USER;
            }

        });
    }

    private void bloodsuck() {
        super.register("bloodsuck", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                TextChannel channel = event.getChannel();
                if (event.getMessage().getMentionedUsers().isEmpty()) {
                    channel.sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/ZR8Plmd.png"), "suck.png", null).queue();
                }
                else {
                    String bString = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors
							.joining(" "));
                    String bs = String.format(EmoteReference.TALKING + "%s sucks the blood of %s", event.getAuthor().getAsMention(),
							bString);
                    channel.sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/ZR8Plmd.png"), "suck.png",
                            new MessageBuilder().append(bs).build()).queue();
                }
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.USER;
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return baseEmbed(event, "Bloodsuck")
                        .setDescription("Sucks the blood of the mentioned user(s)")
                        .build();
            }
        });
    }

    private void lewd() {
        super.register("lewd", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                String lood = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining
						(" "));
                event.getChannel().sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/LJfZYau.png"), "lewd.png"
                        , new MessageBuilder().append(lood).append(" Y-You lewdie!").build()).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Lewd")
                        .setDescription("Y-You lewdie")
                        .build();
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.USER;
            }

        });
    }

    private void meow() {
        super.register("mew", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                TextChannel channel = event.getChannel();
                Message receivedMessage = event.getMessage();
                if (!receivedMessage.getMentionedUsers().isEmpty()) {
                    String mew = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
                    channel.sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/yFGHvVR.gif"), "mew.gif",
                            new MessageBuilder().append(EmoteReference.TALKING).append(String.format("*meows at %s.*", mew)).build()).queue();
                }
                else {
                    channel.sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/yFGHvVR.gif"), "mew.gif",
                            new MessageBuilder().append(":speech_balloon: Meeeeow.").build()).queue();
                }
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.USER;
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Meow command")
                        .setDescription("Meows at a user or just meows.")
                        .setColor(Color.cyan)
                        .build();
            }
        });
    }
}
