package net.kodehawa.mantarobot.commands.interaction.polls;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.kodehawa.dataporter.oldentities.OldGuild;
import net.kodehawa.mantarobot.commands.interaction.Lobby;
import net.kodehawa.mantarobot.core.listeners.operations.old.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.old.OperationListener;
import net.kodehawa.mantarobot.core.listeners.operations.old.ReactionOperationListener;
import net.kodehawa.mantarobot.core.listeners.operations.old.ReactionOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.helpers.ExtraGuildData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Poll extends Lobby {

    private static Map<String, Poll> runningPolls = new HashMap<>();

    private String[] options;
    private GuildMessageReceivedEvent event;
    private long timeout;
    private boolean isCompilant = true;
    private String name = "";
    private Future<Void> runningPoll;

    public Poll(GuildMessageReceivedEvent event, String name, long timeout, String... options) {
        super(event.getChannel());
        this.event = event;
        this.options = options;
        this.timeout = timeout;
        this.name = name;

        if(options.length > 9 || options.length < 2 || timeout > 2820000 || timeout < 30000) {
            isCompilant = false;
        }
    }

    public static Map<String, Poll> getRunningPolls() {
        return runningPolls;
    }

    public static PollBuilder builder() {
        return new PollBuilder();
    }

    public void startPoll() {
        try {
            if(!isCompilant) {
                getChannel().sendMessage(EmoteReference.WARNING +
                        "This poll cannot build. " +
                        "**Remember that the maximum amount of options are 9, the minimum is 2 and that the maximum timeout is 45m and the minimum timeout is 30s.**\n" +
                        "OptionHandler are separated with a comma, for example `1,2,3`. For spaced stuff use commas at the start and end of the sentence.").queue();
                getRunningPolls().remove(getChannel().getId());
                return;
            }

            if(isPollAlreadyRunning(getChannel())) {
                getChannel().sendMessage(EmoteReference.WARNING + "There seems to be another poll running here...").queue();
                return;
            }

            if(!event.getGuild().getSelfMember().hasPermission(getChannel(), Permission.MESSAGE_ADD_REACTION)) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "Seems like I cannot add reactions here...").queue();
                getRunningPolls().remove(getChannel());
                return;
            }

            OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            ExtraGuildData data = dbGuild.getData();
            AtomicInteger at = new AtomicInteger();

            data.setRanPolls(data.getRanPolls() + 1L);
            dbGuild.save();

            String toShow = Stream.of(options).map(opt -> String.format("#%01d.- %s", at.incrementAndGet(), opt)).collect(Collectors.joining("\n"));

            if(toShow.length() > 1014) {
                toShow = "This was too long to show, so I pasted it: " + Utils.paste(toShow);
            }

            EmbedBuilder builder = new EmbedBuilder().setAuthor(String.format("Poll #%1d created by %s",
                    data.getRanPolls(), event.getAuthor().getName()), null, event.getAuthor().getAvatarUrl())
                    .setDescription("**Poll started. React to the number to vote.**\n*" + name + "*")
                    .addField("OptionHandler", "```md\n" + toShow + "```", false)
                    .setColor(event.getMember().getColor())
                    .setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
                    .setFooter("You have " + Utils.getDurationMinutes(timeout) + " minutes to vote.", event.getAuthor().getAvatarUrl());


            getChannel().sendMessage(builder.build()).queue(this::createPoll);

            InteractiveOperations.create(getChannel(), timeout, e -> {
                if(e.getAuthor().getId().equals(event.getAuthor().getId())) {
                    if(e.getMessage().getRawContent().equalsIgnoreCase("&cancelpoll")) {
                        runningPoll.cancel(true);
                        getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled poll").queue();
                        getRunningPolls().remove(getChannel().getId());
                        return OperationListener.COMPLETED;
                    }
                    return OperationListener.RESET_TIMEOUT;
                }
                return OperationListener.IGNORED;
            });

            runningPolls.put(getChannel().getId(), this);
        } catch(Exception e) {
            getChannel().sendMessage(EmoteReference.ERROR + "An unknown error has occurred while setting up a poll. Maybe try again?").queue();
        }
    }

    public boolean isPollAlreadyRunning(TextChannel channel) {
        return runningPolls.containsKey(channel.getId());
    }

    private String[] reactions(int options) {
        if(options < 2) throw new IllegalArgumentException("How?");
        if(options > 9) throw new IllegalArgumentException("How?? ^ 2");
        String[] r = new String[options];
        for(int i = 0; i < options; i++) {
            r[i] = (char) ('\u0031' + i) + "\u20e3";
        }
        return r;
    }

    private Future<Void> createPoll(Message message) {
        runningPoll = ReactionOperations.create(message, TimeUnit.MILLISECONDS.toSeconds(timeout), new ReactionOperationListener() {
            @Override
            public int add(MessageReactionAddEvent e) {
                int i = e.getReactionEmote().getName().charAt(0) - '\u0030';
                if(i < 1 || i > options.length) return OperationListener.IGNORED;
                return OperationListener.IGNORED; //always return false anyway lul
            }

            @Override
            public void onExpire() {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle("Poll results")
                        .setDescription("**Showing results for the poll started by " + event.getAuthor().getName() + "** with name: *" + name + "*")
                        .setFooter("Thanks for your vote", null);

                AtomicInteger react = new AtomicInteger(0);
                AtomicInteger counter = new AtomicInteger(0);
                String votes = new ArrayList<>(getChannel().getMessageById(message.getIdLong()).complete().getReactions()).stream()
                        .filter(r -> react.getAndIncrement() <= options.length)
                        .map(r -> "+Registered " + (r.getCount() - 1) + " votes for option " + options[counter.getAndIncrement()])
                        .collect(Collectors.joining("\n"));

                embedBuilder.addField("Results", "```diff\n" + votes + "```", false);
                event.getChannel().sendMessage(embedBuilder.build()).queue();
                getRunningPolls().remove(getChannel().getId());
            }
        }, reactions(options.length));

        return runningPoll;
    }
}
