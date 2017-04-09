package net.kodehawa.mantarobot.commands;

import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.CommandPermission;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MiscCmds extends Module {
    public static final DataManager<List<String>> facts = new SimpleFileDataManager("assets/mantaro/texts/facts.txt");
    public static final DataManager<List<String>> noble = new SimpleFileDataManager("assets/mantaro/texts/noble.txt");
    private static final Logger LOGGER = LoggerFactory.getLogger("Audio");

    public MiscCmds() {
        super(Category.MISC);
        misc();
        eightBall();
        randomFact();
        iam();
    }

    private void iam() {
        super.register("iam", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                HashMap<String, String> autoroles = MantaroData.db().getGuild(event.getGuild()).getData().getAutoroles();
                if (args.length == 0) {
                    event.getChannel().sendMessage(helpEmbed(event, "Iam (autoroles)")
                            .setDescription("Get an autorole that your server administrators have set up!\n"
                                    + "~>iam <name>. Get the role with the specified name.\n"
                                    + "~>iam list. List all the available autoroles in this server")
                            .build()).queue();
                    return;
                }
                if (content.equals("list")) {
                    EmbedBuilder embed = baseEmbed(event, "Autorole list");
                    if (autoroles.size() > 0) {
                        autoroles.forEach((name, roleId) -> {
                            Role role = event.getGuild().getRoleById(roleId);
                            if (role != null) embed.appendDescription("\n" + name + ": " + role.getName());
                        });
                    }
                    else embed.setDescription("There aren't any autoroles setup in this server!");
                    event.getChannel().sendMessage(embed.build()).queue();
                    return;
                }
                String autoroleName = args[0];
                if (autoroles.containsKey(autoroleName)) {
                    Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
                    if (role == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "The role that this autorole corresponded " +
                                "to has been deleted").queue();
                    }
                    else {
                        try {
                            event.getGuild().getController().addRolesToMember(event.getMember(), role).queue(aVoid -> {
                                event.getChannel().sendMessage(EmoteReference.OK + event.getAuthor().getAsMention() + ", you've been " +
                                        "given the **" + role.getName() + "** role").queue();
                            });
                        }
                        catch (PermissionException pex) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't give you **" + role.getName() + ". Make " +
                                    "sure that I have permission to add roles and that my role is above **" + role.getName() + "**")
                                    .queue();
                        }
                    }
                }
                else event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't an autorole with this name!").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Iam (autoroles)")
                        .setDescription("Get an autorole that your server administrators have set up!\n"
                                + "~>iam <name>. Get the role with the specified name.\n"
                                + "~>iam list. List all the available autoroles in this server")
                        .build();
            }
        });
    }

    private void eightBall() {
        super.register("8ball", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                if (content.isEmpty()) {
                    onHelp(event);
                    return;
                }

                String textEncoded;
                String answer;
                try {
                    textEncoded = URLEncoder.encode(content, "UTF-8");
                    answer = Unirest.get(String.format("https://8ball.delegator.com/magic/JSON/%1s", textEncoded))
                            .asJson()
                            .getBody()
                            .getObject()
                            .getJSONObject("magic")
                            .getString("answer");
                }
                catch (Exception exception) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I ran into an error while fetching 8ball results. My owners " +
                            "have been notified and will resolve this soon.")
                            .queue();
                    LOGGER.warn("Error while processing answer", exception);
                    return;
                }

                event.getChannel().sendMessage("\uD83D\uDCAC " + answer + ".").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "8ball")
                        .setDescription("Retrieves an answer from the magic 8Ball.\n"
                                + "~>8ball <question>. Retrieves an answer from 8ball based on the question or sentence provided.")
                        .build();
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.USER;
            }

        });
    }

    private void misc() {
        super.register("misc", new SimpleCommand() {
            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Misc Commands")
                        .setDescription("Miscellaneous funny/useful commands. Ranges from funny commands and random colors to bot " +
                                "hardware information\n"
                                + "Usage:\n"
                                + "~>misc reverse <sentence>: Reverses any given sentence.\n"
                                + "~>misc noble: Random Lost Pause quote.\n"
                                + "~>misc rndcolor: Gives you a random hex color.\n"
                                + "Parameter explanation:\n"
                                + "sentence: A sentence to reverse."
                                + "@user: A user to mention.")
                        .build();
            }

            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                TextChannel channel = event.getChannel();
                String noArgs = content.split(" ")[0];
                switch (noArgs) {
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
                        onHelp(event);
                        break;
                }
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.USER;
            }

        });
    }

    /**
     * @return a random hex color.
     */
    private String randomColor() {
        String[] letters = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        StringBuilder color = new StringBuilder("#");
        for (int i = 0; i < 6; i++) {
            color.append(letters[(int) Math.floor(Math.random() * 16)]);
        }
        return color.toString();
    }

    private void randomFact() {
        super.register("randomfact", new SimpleCommand() {
            @Override
            protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
                event.getChannel().sendMessage(EmoteReference.TALKING + facts.get().get(new Random().nextInt(facts.get().size() - 1)))
                        .queue();
            }

            @Override
            public CommandPermission permissionRequired() {
                return CommandPermission.USER;
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Random Fact")
                        .setDescription("Sends a random fact.")
                        .build();
            }
        });
    }
}
