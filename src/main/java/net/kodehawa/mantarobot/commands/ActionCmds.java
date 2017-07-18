package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.collections.CollectionUtils;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.action.ImageActionCmd;
import net.kodehawa.mantarobot.commands.action.TextActionCmd;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

@Module
public class ActionCmds {
    //TODO API-fy this images
    static final DataManager<List<String>> BITES = new SimpleFileDataManager("assets/mantaro/texts/bites.txt");
    static final DataManager<List<String>> GREETINGS = new SimpleFileDataManager("assets/mantaro/texts/greetings.txt");
    static final DataManager<List<String>> HUGS = new SimpleFileDataManager("assets/mantaro/texts/hugs.txt");
    static final DataManager<List<String>> PATS = new SimpleFileDataManager("assets/mantaro/texts/pats.txt");
    static final DataManager<List<String>> POKES = new SimpleFileDataManager("assets/mantaro/texts/pokes.txt");
    static final DataManager<List<String>> SLAPS = new SimpleFileDataManager("assets/mantaro/texts/slaps.txt");
    private static final DataManager<List<String>> BLEACH = new SimpleFileDataManager("assets/mantaro/texts/bleach.txt");
    private static final DataManager<List<String>> HIGHFIVES = new SimpleFileDataManager("assets/mantaro/texts/highfives.txt");
    private static final DataManager<List<String>> KISSES = new SimpleFileDataManager("assets/mantaro/texts/kisses.txt");
    private static final DataManager<List<String>> POUTS = new SimpleFileDataManager("assets/mantaro/texts/pouts.txt");
    private static final DataManager<List<String>> TICKLES = new SimpleFileDataManager("assets/mantaro/texts/tickles.txt");
    private static final DataManager<List<String>> TSUNDERE = new SimpleFileDataManager("assets/mantaro/texts/tsundere.txt");
    private static final DataManager<List<String>> LEWD = new SimpleFileDataManager("assets/mantaro/texts/lewd.txt");
    private static final DataManager<List<String>> FACEDESK = new SimpleFileDataManager("assets/mantaro/texts/facedesk.txt");

    @Subscribe
    public static void action(CommandRegistry registry) {
        registry.register("action", new SimpleCommand(Category.ACTION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String noArgs = content.split(" ")[0];
                TextChannel channel = event.getChannel();
                switch(noArgs) {
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
                        .addField("Usage", "`~>action bleach` - **Random bleach picture**.\n" +
                                "`~>action facedesk` - **When you really mess up.**\n" +
                                "`~>action nom` - **nom nom**.", false)
                        .setColor(Color.PINK)
                        .build();
            }
        });
    }

    @Subscribe
    public static void bloodsuck(CommandRegistry registry) {
        registry.register("bloodsuck", new SimpleCommand(Category.ACTION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(event.getMessage().getMentionedUsers().isEmpty()) {
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
                        .setDescription("**Sucks the blood of the mentioned members**")
                        .build();
            }
        });
    }

    @Subscribe
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
                        .setDescription("**L-lewd**").build();
            }
        });
    }

    @Subscribe
    public static void meow(CommandRegistry registry) {
        registry.register("meow", new SimpleCommand(Category.ACTION) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Message receivedMessage = event.getMessage();
                if(!receivedMessage.getMentionedUsers().isEmpty()) {
                    String mew = event.getMessage().getMentionedUsers().stream().map(IMentionable::getAsMention).collect(Collectors.joining(" "));
                    event.getChannel().sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/yFGHvVR.gif"), "mew.gif",
                            new MessageBuilder().append(EmoteReference.TALKING).append(String.format("%s *is meowing at %s.*", event.getAuthor().getAsMention(), mew)).build()).queue();
                } else {
                    event.getChannel().sendFile(ImageActionCmd.CACHE.getInput("http://imgur.com/yFGHvVR.gif"), "mew.gif",
                            new MessageBuilder().append(":speech_balloon: Meow.").build()).queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Meow command")
                        .setDescription("**Meow either to a person or the sky**.")
                        .setColor(Color.cyan)
                        .build();
            }
        });
        registry.registerAlias("meow", "mew");
    }

    @Subscribe
    public static void register(CommandRegistry cr) {

        //pat();
        cr.register("pat", new ImageActionCmd(
                "Pat", "Pats the specified user.", Color.PINK,
                "pat.gif", EmoteReference.TALKING + "%s you have been patted by %s", PATS.get(), "Aww, I see you are lonely, take a pat <3"
        ));

        //hug();
        cr.register("hug", new ImageActionCmd(
                "Hug", "Hugs the specified user.", Color.PINK,
                "hug.gif", EmoteReference.TALKING + "%s you have been hugged by %s", HUGS.get(), "Aww, I see you are lonely, take a hug <3"
        ));

        //kiss();
        cr.register("kiss", new ImageActionCmd(
                "Kiss", "Kisses the specified user.", Color.PINK,
                "kiss.gif", EmoteReference.TALKING + "%s you have been kissed by %s", KISSES.get(), "Aww, I see you are lonely, *kisses*"
        ));

        //poke();
        cr.register("poke", new ImageActionCmd(
                "Poke", "Pokes the specified user.", Color.PINK,
                "poke.gif", EmoteReference.TALKING + "%s you have been poked by %s :eyes:", POKES.get(), "Aww, I see you are lonely, *pokes you*"
        ));

        //slap();
        cr.register("slap", new ImageActionCmd(
                "Slap", "Slaps the specified user ;).", Color.PINK,
                "slap.gif", EmoteReference.TALKING + "%s you have been slapped by %s!", SLAPS.get(), "Hmm, why do you want this? Uh, I guess... *slaps you*"
        ));

        //bite();
        cr.register("bite", new ImageActionCmd(
                "Bite", "Bites the specified user.", Color.PINK,
                "bite.gif", EmoteReference.TALKING + "%s you have been bitten by %s :eyes:", BITES.get(), "*bites you*"
        ));

        //tickle();
        cr.register("tickle", new ImageActionCmd(
                "Tickle", "Tickles the specified user.", Color.PINK,
                "tickle.gif", EmoteReference.JOY + "%s you have been tickled by %s", TICKLES.get(), "*tickles you*"
        ));

        //highfive();
        cr.register("highfive", new ImageActionCmd(
                "Highfive", "Highfives with the specified user.", Color.PINK,
                "highfive.gif", EmoteReference.TALKING + "%s highfives %s :heart:", HIGHFIVES.get(), "*highfives*", true
        ));

        //pout();
        cr.register("pout", new ImageActionCmd(
                "Pout", "Pouts at the specified user.", Color.PINK,
                "pout.gif", EmoteReference.TALKING + "%s pouts at %s *hmph*", POUTS.get(), "*pouts*, **hmph**", true
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
