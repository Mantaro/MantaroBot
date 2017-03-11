package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.RateLimiter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.utils.data.BugData;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BugreportCmds extends Module
{
    private static final String RATELIMIT_MESSAGE = "You are being ratelimited";

    private final RateLimiter limiter = new RateLimiter(1, (int)TimeUnit.MINUTES.toMillis(10)); //ratelimit for one report/10 minutes

    public BugreportCmds() {
        super(Category.UTILS);
        report();
    }

    private void report()
    {
        super.register("bugreport", new SimpleCommand() {
            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Bug commands")
                        .addField("Description",
                                "~>bugreport bug",
                        false)
                        .build();
            }

            @Override
            public void call(String[] args, String content, GuildMessageReceivedEvent event)
            {
                if(!limiter.process(event.getAuthor().getId())) {
                    event.getChannel().sendMessage(RATELIMIT_MESSAGE).queue();
                    return;
                }
                BugData dt = MantaroData.getBugs().get();
                BugData.Bug bug = new BugData.Bug();
                bug.bug = String.join(" ", args);
                bug.reporterId = Long.parseLong(event.getAuthor().getId());
                bug.time = event.getMessage().getCreationTime().toEpochSecond()*1000;
                long id = dt.nextId.getAndIncrement();
                dt.bugs.put(id, bug);
                event.getChannel().sendMessage("Reported successfully").queue();
                bug.messageId = Long.parseLong(
                        MantaroBot.getInstance().getTextChannelById(MantaroData.getConfig().get().bugreportChannel).sendMessage(new EmbedBuilder()
                                .setTitle("Bug #" + id, null)
                                .setAuthor(event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator(), null, event.getAuthor().getEffectiveAvatarUrl())
                                .setDescription(bug.bug)
                                .build()
                        ).complete().getId()
                );
                MantaroData.getBugs().save();
            }

            @Override
            public boolean isHiddenFromHelp() {
                return false;
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.USER;
            }
        });
    }

    private void accept()
    {
        super.register("bug", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                if(args.length < 2) {
                    event.getChannel().sendMessage("Usage: bug accept/close number").queue();
                    return;
                }
                Map<Long, BugData.Bug> bugs = MantaroData.getBugs().get().bugs;
                BugData.Bug bug;
                try {
                    bug = bugs.get(Long.parseLong(args[1]));
                } catch (NumberFormatException nfe) {
                    event.getChannel().sendMessage("No bug with id " + args[1]).queue();
                    return;
                }
                switch(args[0]) {
                    case "accept":
                        bugs.remove(Long.parseLong(args[1]));
                        MantaroBot.getInstance().getTextChannelById(MantaroData.getConfig().get().bugreportChannel).pinMessageById("" + bug.messageId).queue();
                        break;
                    case "close":
                        bugs.remove(Long.parseLong(args[1]));
                        MantaroBot.getInstance().getTextChannelById(MantaroData.getConfig().get().bugreportChannel).unpinMessageById("" + bug.messageId).queue((v)->
                            MantaroBot.getInstance().getTextChannelById(MantaroData.getConfig().get().bugreportChannel).deleteMessageById("" + bug.messageId).queue((vv)->
                                event.getChannel().sendMessage("Closed bug #" + args[1]).queue()
                            )
                        );
                        break;
                    default:
                        event.getChannel().sendMessage("Usage: bug accept/close number").queue();
                        break;
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Bug commands")
                        .addField("Description:", "~>bug accept: accepts a bug\n" +
                                                  "~>bug decline: declines a bug\n",
                                false).build();
            }
        });
    }
}
