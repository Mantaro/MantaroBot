package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.texts.StringUtils;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.commands.interaction.polls.PollBuilder;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

@Module
@Slf4j
public class MiscCmds {
    public static final DataManager<List<String>> facts = new SimpleFileDataManager("assets/mantaro/texts/facts.txt");
    public static final DataManager<List<String>> noble = new SimpleFileDataManager("assets/mantaro/texts/noble.txt");
    private static final String[] HEX_LETTERS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

    @Subscribe
    public static void eightBall(CommandRegistry cr) {
        cr.register("8ball", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(content.isEmpty()) {
                    onError(event);
                    return;
                }

                String textEncoded;
                String answer;
                try {
                    textEncoded = URLEncoder.encode(content, "UTF-8");
                    String json = Utils.wget(String.format("https://8ball.delegator.com/magic/JSON/%1s", textEncoded), event);
                    answer = new JSONObject(json).getJSONObject("magic").getString("answer");
                } catch(Exception exception) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I ran into an error while fetching 8ball results.")
                            .queue();
                    return;
                }

                event.getChannel().sendMessage("\uD83D\uDCAC " + answer + ".").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "8ball")
                        .setDescription("**Retrieves an answer from the almighty 8ball.**")
                        .addField("Usage",
                                "`~>8ball <question>` - **Retrieves an answer from 8ball based on the question or sentence provided.**",
                                false)
                        .build();
            }
        });

        cr.registerAlias("8ball", "8b");
    }

    @Subscribe
    public static void iam(CommandRegistry cr) {
        cr.register("iam", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Map<String, String> autoroles = MantaroData.db().getGuild(event.getGuild()).getData().getAutoroles();
                if(args.length == 0 || content.length() == 0) {
                    onError(event);
                    return;
                }

                StringBuilder stringBuilder = new StringBuilder();

                if(content.equals("list") || content.equals("ls")) {
                    EmbedBuilder embed = baseEmbed(event, "Autorole list");
                    if(autoroles.size() > 0) {
                        autoroles.forEach((name, roleId) -> {
                            Role role = event.getGuild().getRoleById(roleId);
                            if(role != null)
                                stringBuilder.append("\nAutorole name: ").append(name).append(" | Gives role **").append(role.getName()).append("**");
                        });

                        embed.setDescription(checkString(stringBuilder.toString()));
                    } else embed.setDescription("There aren't any autoroles setup in this server!");
                    event.getChannel().sendMessage(embed.build()).queue();
                    return;
                }
                iamFunction(args[0], event);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Iam (autoroles)")
                        .setDescription("**Get an autorole that your server administrators have set up!**")
                        .addField("Usage", "`~>iam <name>` - **Get the role with the specified name**.\n"
                                + "`~>iam list` - **List all the available autoroles in this server**", false)
                        .build();
            }
        });
    }

    public static void iamFunction(String autoroleName, GuildMessageReceivedEvent event) {
        Map<String, String> autoroles = MantaroData.db().getGuild(event.getGuild()).getData().getAutoroles();

        if(autoroles.containsKey(autoroleName)) {
            Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
            if(role == null) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "The role that this autorole corresponded " +
                        "to has been deleted").queue();
            } else {
                if(event.getMember().getRoles().stream().filter(r1 -> r1.getId().equals(role.getId())).collect(Collectors.toList()).size() > 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You already have this role, silly!").queue();
                    return;
                }
                try {
                    event.getGuild().getController().addRolesToMember(event.getMember(), role).queue(aVoid -> {
                        event.getChannel().sendMessage(EmoteReference.OK + event.getAuthor().getAsMention() + ", you've been " +
                                "given the **" + role.getName() + "** role").queue();
                    });
                } catch(PermissionException pex) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't take from you **" + role.getName() + ". Make " +
                            "sure that I have permission to add roles and that my role is above **" + role.getName() + "**")
                            .queue();
                }
            }
        } else
            event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't an autorole with the name ``" + autoroleName + "``!").queue();
    }

    @Subscribe
    public static void iamnot(CommandRegistry cr) {
        cr.register("iamnot", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                HashMap<String, String> autoroles = MantaroData.db().getGuild(event.getGuild()).getData().getAutoroles();
                if(args.length == 0 || content.length() == 0) {
                    onHelp(event);
                    return;
                }

                if(content.equals("list")) {
                    EmbedBuilder embed = baseEmbed(event, "Autorole list");
                    if(autoroles.size() > 0) {
                        autoroles.forEach((name, roleId) -> {
                            Role role = event.getGuild().getRoleById(roleId);
                            if(role != null)
                                embed.appendDescription("\nAutorole name: " + name + " | Gives role **" + role.getName() + "**");
                        });
                    } else embed.setDescription("There aren't any autoroles setup in this server!");
                    event.getChannel().sendMessage(embed.build()).queue();
                    return;
                }

                iamnotFunction(args[0], event);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Iamnot (autoroles)")
                        .setDescription("**Remove an autorole that your server administrators have set up!**")
                        .addField("Usage", "~>iamnot <name>. Remove the role with the specified name.\n"
                                + "~>iamnot list. List all the available autoroles in this server", false)
                        .build();
            }
        });
    }

    public static void iamnotFunction(String autoroleName, GuildMessageReceivedEvent event) {
        Map<String, String> autoroles = MantaroData.db().getGuild(event.getGuild()).getData().getAutoroles();

        if(autoroles.containsKey(autoroleName)) {
            Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
            if(role == null) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "The role that this autorole corresponded " +
                        "to has been deleted").queue();
            } else {
                if(!(event.getMember().getRoles().stream().filter(r1 -> r1.getId().equals(role.getId())).collect(Collectors.toList()).size() > 0)) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have this role, silly!").queue();
                    return;
                }
                try {
                    event.getGuild().getController().removeRolesFromMember(event.getMember(), role).queue(aVoid -> {
                        event.getChannel().sendMessage(EmoteReference.OK + event.getAuthor().getAsMention() + ", you've " +
                                "lost the **" + role.getName() + "** role").queue();
                    });
                } catch(PermissionException pex) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't give you **" + role.getName() + ". Make " +
                            "sure that I have permission to add roles and that my role is above **" + role.getName() + "**")
                            .queue();
                }
            }
        } else
            event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't an autorole with the name ``" + autoroleName + "``!").queue();
    }

    @Subscribe
    public static void misc(CommandRegistry cr) {
        cr.register("misc", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                TextChannel channel = event.getChannel();
                String noArgs = content.split(" ")[0];
                switch(noArgs) {
                    case "reverse":
                        String stringToReverse = content.replace("reverse ", "");
                        String reversed = new StringBuilder(stringToReverse).reverse().toString();
                        channel.sendMessage(reversed.replace("@everyone", "").replace("@here", "")).queue();
                        break;
                    case "rndcolor":
                        String s = String.format(EmoteReference.TALKING + "Your random color is %s", randomColor());
                        channel.sendMessage(s).queue();
                        break;
                    case "noble":
                        channel.sendMessage(EmoteReference.TALKING + noble.get().get(new Random().nextInt(noble.get().size() - 1)) + " " +
                                "-Noble").queue();
                        break;
                    default:
                        onError(event);
                        break;
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Misc Commands")
                        .setDescription("**Miscellaneous funny/useful commands.**")
                        .addField("Usage",
                                "`~>misc reverse <sentence>` - **Reverses any given sentence.**\n"
                                        + "`~>misc noble` - **Random Lost Pause quote.**\n"
                                        + "`~>misc rndcolor` - **Gives you a random hex color.**\n"
                                , false)
                        .addField("Parameter Explanation",
                                "`sentence` - **A sentence to reverse.**\n"
                                        + "`@user` - **A user to mention.**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public static void randomFact(CommandRegistry cr) {
        cr.register("randomfact", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                event.getChannel().sendMessage(
                        EmoteReference.TALKING + facts.get().get(new Random().nextInt(facts.get().size() - 1))).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Random Fact")
                        .setDescription("**Sends a random fact.**")
                        .build();
            }
        });

        cr.registerAlias("randomfact", "rf");
    }

    @Subscribe
    public static void createPoll(CommandRegistry registry) {
        registry.register("createpoll", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Map<String, Optional<String>> opts = StringUtils.parse(args);
                PollBuilder builder = Poll.builder();
                if(!opts.containsKey("time") || !opts.get("time").isPresent()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You didn't include either the `-time` argument or it was empty!").queue();
                    return;
                }

                if(!opts.containsKey("options") || !opts.get("options").isPresent()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You didn't include either the `-options` argument or it was empty!").queue();
                    return;
                }

                if(!opts.containsKey("name") || !opts.get("name").isPresent()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You didn't include either the `-name` argument or it was empty!").queue();
                    return;
                }

                if(opts.containsKey("name") || opts.get("name").isPresent()) {
                    builder.setName(opts.get("name").get().replaceAll(String.valueOf('"'), ""));
                }


                String[] options = opts.get("options").get().replaceAll(String.valueOf('"'), "").split(",");
                long timeout = Utils.parseTime(opts.get("time").get());

                builder.setEvent(event)
                        .setTimeout(timeout)
                        .setOptions(options)
                        .build()
                        .startPoll();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Poll Command")
                        .setDescription("**Creates a poll**")
                        .addField("Usage", "`~>poll [-options <options>] [-time <time>] [-name <name>]`", false)
                        .addField("Parameters", "`-options` The options to add. Minimum is 2 and maximum is 9.\n" +
                                "`-time` The time the operation is gonna take. The format is as follows `1ms29s` for 1 minute and 21 seconds. Maximum poll runtime is 45 minutes.\n" +
                                "`-name` The name of the poll for reference.", false)
                        .addField("Considerations", "To cancel the running poll type &cancelpoll. Only the person who started it can cancel it.", false)
                        .build();
            }
        });

        registry.registerAlias("createpoll", "poll");
    }

    /**
     * @return a random hex color.
     */
    private static String randomColor() {
        return IntStream.range(0, 6).mapToObj(i -> random(HEX_LETTERS)).collect(Collectors.joining());
    }
}
