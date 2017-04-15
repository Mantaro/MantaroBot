package net.kodehawa.mantarobot.commands;

import com.mashape.unirest.http.Unirest;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static br.com.brjdevs.java.utils.extensions.CollectionUtils.random;

@RegisterCommand.Class
@Slf4j
public class MiscCmds {
	public static final DataManager<List<String>> facts = new SimpleFileDataManager("assets/mantaro/texts/facts.txt");
	public static final DataManager<List<String>> noble = new SimpleFileDataManager("assets/mantaro/texts/noble.txt");
	private static final String[] HEX_LETTERS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

	@RegisterCommand
	public static void eightBall(CommandRegistry cr) {
		cr.register("8ball", SimpleCommand.builder(Category.MISC)
				.permission(CommandPermission.USER)
				.code((thiz, event, content, args) -> {
					if (content.isEmpty()) {
						thiz.onHelp(event);
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
					} catch (Exception exception) {
						event.getChannel().sendMessage(EmoteReference.ERROR + "I ran into an error while fetching 8ball results. My owners " +
								"have been notified and will resolve this soon.")
								.queue();
						log.warn("Error while processing answer", exception);
						return;
					}

					event.getChannel().sendMessage("\uD83D\uDCAC " + answer + ".").queue();
				})
				.help((thiz, event) -> thiz.helpEmbed(event, "8ball")
						.setDescription("Retrieves an answer from the magic 8Ball.\n"
								+ "~>8ball <question>. Retrieves an answer from 8ball based on the question or sentence provided.")
						.build())
				.build());
	}

	@RegisterCommand
	public static void iam(CommandRegistry cr) {
		cr.register("iam", SimpleCommand.builder(Category.MISC)
				.permission(CommandPermission.USER)
				.code((thiz, event, content, args) -> {
					HashMap<String, String> autoroles = MantaroData.db().getGuild(event.getGuild()).getData().getAutoroles();
					if (args.length == 0 || content.length() == 0) {
						thiz.onHelp(event);
						return;
					}
					if (content.equals("list")) {
						EmbedBuilder embed = thiz.baseEmbed(event, "Autorole list");
						if (autoroles.size() > 0) {
							autoroles.forEach((name, roleId) -> {
								Role role = event.getGuild().getRoleById(roleId);
								if (role != null)
									embed.appendDescription("\nAutorole name: " + name + " | Gives role **" + role.getName() + "**");
							});
						} else embed.setDescription("There aren't any autoroles setup in this server!");
						event.getChannel().sendMessage(embed.build()).queue();
						return;
					}

					String autoroleName = args[0];
					if (autoroles.containsKey(autoroleName)) {
						Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
						if (role == null) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "The role that this autorole corresponded " +
									"to has been deleted").queue();
						} else {
							if (event.getMember().getRoles().stream().filter(r1 -> r1.getId().equals(role.getId())).collect(Collectors.toList()).size() > 0) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "You already have this role, silly!").queue();
								return;
							}
							try {
								event.getGuild().getController().addRolesToMember(event.getMember(), role).queue(aVoid -> {
									event.getChannel().sendMessage(EmoteReference.OK + event.getAuthor().getAsMention() + ", you've been " +
											"given the **" + role.getName() + "** role").queue();
								});
							} catch (PermissionException pex) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't take from you **" + role.getName() + ". Make " +
										"sure that I have permission to add roles and that my role is above **" + role.getName() + "**")
										.queue();
							}
						}
					} else
						event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't an autorole with this name!").queue();
				})
				.help((thiz, event) -> thiz.helpEmbed(event, "Iam (autoroles)")
						.setDescription("Get an autorole that your server administrators have set up!\n"
								+ "~>iam <name>. Get the role with the specified name.\n"
								+ "~>iam list. List all the available autoroles in this server")
						.build())
				.build());
	}

	@RegisterCommand
	public static void iamnot(CommandRegistry cr) {
		cr.register("iamnot", SimpleCommand.builder(Category.MISC)
				.permission(CommandPermission.USER)
				.code((thiz, event, content, args) -> {
					HashMap<String, String> autoroles = MantaroData.db().getGuild(event.getGuild()).getData().getAutoroles();
					if (args.length == 0 || content.length() == 0) {
						thiz.onHelp(event);
						return;
					}
					if (content.equals("list")) {
						EmbedBuilder embed = thiz.baseEmbed(event, "Autorole list");
						if (autoroles.size() > 0) {
							autoroles.forEach((name, roleId) -> {
								Role role = event.getGuild().getRoleById(roleId);
								if (role != null)
									embed.appendDescription("\nAutorole name: " + name + " | Gives role **" + role.getName() + "**");
							});
						} else embed.setDescription("There aren't any autoroles setup in this server!");
						event.getChannel().sendMessage(embed.build()).queue();
						return;
					}
					String autoroleName = args[0];
					if (autoroles.containsKey(autoroleName)) {
						Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
						if (role == null) {
							event.getChannel().sendMessage(EmoteReference.ERROR + "The role that this autorole corresponded " +
									"to has been deleted").queue();
						} else {
							if (!(event.getMember().getRoles().stream().filter(r1 -> r1.getId().equals(role.getId())).collect(Collectors.toList()).size() > 0)) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have this role, silly!").queue();
								return;
							}
							try {
								event.getGuild().getController().removeRolesFromMember(event.getMember(), role).queue(aVoid -> {
									event.getChannel().sendMessage(EmoteReference.OK + event.getAuthor().getAsMention() + ", you've " +
											"lost the **" + role.getName() + "** role").queue();
								});
							} catch (PermissionException pex) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't give you **" + role.getName() + ". Make " +
										"sure that I have permission to add roles and that my role is above **" + role.getName() + "**")
										.queue();
							}
						}
					} else
						event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't an autorole with this name!").queue();
				})
				.help((thiz, event) -> thiz.helpEmbed(event, "Iamnot (autoroles)")
						.setDescription("Remove an autorole that your server administrators have set up!\n"
								+ "~>iamnot <name>. Remove the role with the specified name.\n"
								+ "~>iamnot list. List all the available autoroles in this server")
						.build())
				.build());
	}

	@RegisterCommand
	public static void misc(CommandRegistry cr) {
		cr.register("misc", SimpleCommand.builder(Category.MISC)
				.permission(CommandPermission.USER)
				.code((thiz, event, content, args) -> {
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
							thiz.onHelp(event);
							break;
					}
				})
				.help((thiz, event) -> thiz.helpEmbed(event, "Misc Commands")
						.setDescription("Miscellaneous funny/useful commands.\n"
								+ "Usage:\n"
								+ "~>misc reverse <sentence>: Reverses any given sentence.\n"
								+ "~>misc noble: Random Lost Pause quote.\n"
								+ "~>misc rndcolor: Gives you a random hex color.\n"
								+ "Parameter explanation:\n"
								+ "sentence: A sentence to reverse."
								+ "@user: A user to mention.")
						.build())
				.build());
	}

	@RegisterCommand
	public static void randomFact(CommandRegistry cr) {
		cr.register("randomfact", SimpleCommand.builder(Category.MISC)
				.permission(CommandPermission.USER)
				.code((thiz, event, content, args) -> {
					event.getChannel().sendMessage(EmoteReference.TALKING + facts.get().get(new Random().nextInt(facts.get().size() - 1))).queue();
				})
				.help((thiz, event) -> thiz.helpEmbed(event, "Random Fact")
						.setDescription("Sends a random fact.")
						.build())
				.build());
	}
	/**
	 * @return a random hex color.
	 */
	private static String randomColor() {
		return IntStream.range(0, 6).mapToObj(i -> random(HEX_LETTERS)).collect(Collectors.joining());
	}
}
