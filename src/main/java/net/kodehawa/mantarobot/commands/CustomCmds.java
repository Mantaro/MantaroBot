/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.custom.CustomCommandHandler;
import net.kodehawa.mantarobot.commands.info.stats.manager.CommandStatsManager;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.CustomCommand;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;
import static net.kodehawa.mantarobot.data.MantaroData.db;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

@Slf4j
@Module
@SuppressWarnings("unused")
public class CustomCmds {
    private static final Map<String, CustomCommand> customCommands = new ConcurrentHashMap<>();
    private final static Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+"),
            INVALID_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9_]"),
            NAME_WILDCARD_PATTERN = Pattern.compile("[a-zA-Z0-9_*]+");

    public static boolean handle(String cmdName, GuildMessageReceivedEvent event, I18nContext lang, String args) {
        CustomCommand customCommand = getCustomCommand(event.getGuild().getId(), cmdName);
        if (customCommand == null)
            return false;

        List<String> values = customCommand.getValues();
        if(customCommand.getData().isNsfw() && !event.getChannel().isNSFW()) {
            event.getChannel().sendMessageFormat(lang.get("commands.custom.nsfw_not_nsfw"), EmoteReference.ERROR).queue();
            return true;
        }

        CommandStatsManager.log("custom command");

        String response = random(values);
        try {
            new CustomCommandHandler(event, lang, response, args).handle();
        } catch (Exception e) {
            event.getChannel().sendMessageFormat(lang.get("commands.custom.error_running"), EmoteReference.ERROR).queue();
        }

        return true;
    }

    @Subscribe
    public void custom(CommandRegistry cr) {
        //People spamming crap... we cant have nice things owo
        final RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 5);

        String any = "[\\d\\D]*?";

        cr.register("custom", new SimpleCommand(Category.UTILS) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!Utils.handleDefaultRatelimit(rateLimiter, event.getAuthor(), event))
                    return;

                if(args.length < 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.no_args"), EmoteReference.ERROR).queue();
                    return;
                }

                String action = args[0];

                if(action.equals("list") || action.equals("ls")) {
                    if(!MantaroCore.hasLoadedCompletely()) {
                        event.getChannel().sendMessage(languageContext.get("commands.custom.not_loaded")).queue();
                        return;
                    }

                    String filter = event.getGuild().getId() + ":";
                    //TODO: this is a real issue lol @natan pls find a way to load list without doing like 50k queries in a second?
                    List<String> commands = db().getCustomCommands(event.getGuild())
                            .stream()
                            .map(CustomCommand::getName)
                            .collect(Collectors.toList());

                    EmbedBuilder builder = new EmbedBuilder()
                            .setAuthor(languageContext.get("commands.custom.ls.header"), null, event.getGuild().getIconUrl())
                            .setColor(event.getMember().getColor())
                            .setDescription(commands.isEmpty() ? languageContext.get("general.dust") : checkString(forType(commands)));

                    event.getChannel().sendMessage(builder.build()).queue();
                    return;
                }

                if(action.equals("view")) {
                    if(args.length < 2) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.view.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String cmd = args[1];
                    CustomCommand command = db().getCustomCommand(event.getGuild(), cmd);

                    if(command == null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.view.not_found"), EmoteReference.ERROR).queue();
                        return;
                    }

                    int number;

                    try {
                        number = Integer.parseInt(args[2]) - 1;
                    } catch(NumberFormatException e) {
                        event.getChannel().sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(command.getValues().size() < number) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.view.less_than_specified"), EmoteReference.ERROR).queue();
                        return;
                    }

                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.view.success"), (number + 1), command.getName(), command.getValues().get(number)).queue();
                    return;
                }

                if(action.equals("raw")) {
                    if(args.length < 2) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.raw.no_command"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String cmd = args[1];
                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                        return;
                    }

                    CustomCommand custom = db().getCustomCommand(event.getGuild(), cmd);
                    if(custom == null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.not_found"), EmoteReference.ERROR2, cmd).queue();
                        return;
                    }

                    Pair<String, Integer> pair = DiscordUtils.embedList(custom.getValues(), Object::toString);

                    String pasted = null;

                    if(pair.getRight() < custom.getValues().size()) {
                        AtomicInteger i = new AtomicInteger();
                        pasted = Utils.paste(custom.getValues().stream().map(s -> i.incrementAndGet() + s).collect(Collectors.joining("\n")));
                    }

                    EmbedBuilder embed = baseEmbed(event, String.format(languageContext.get("commands.custom.raw.header"), cmd))
                            .setDescription(pair.getLeft())
                            .setFooter(String.format(languageContext.get("commands.custom.raw.amount"), pair.getRight(), custom.getValues().size()), null);

                    if(pasted != null && pasted.contains("hastebin.com")) {
                        embed.addField(languageContext.get("commands.custom.raw.pasted"), pasted, false);
                    }

                    event.getChannel().sendMessage(embed.build()).queue();
                    return;
                }

                if(db().getGuild(event.getGuild()).getData().isCustomAdminLock() && !CommandPermission.ADMIN.test(event.getMember())) {
                    event.getChannel().sendMessage(languageContext.get("commands.custom.admin_only")).queue();
                    return;
                }

                if(action.equals("clear")) {
                    if(!CommandPermission.ADMIN.test(event.getMember())) {
                        event.getChannel().sendMessageFormat(languageContext.get("general.invalid_action"), EmoteReference.ERROR).queue();
                        return;
                    }

                    List<CustomCommand> customCommands = db().getCustomCommands(event.getGuild());

                    if(customCommands.isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.no_cc"), EmoteReference.ERROR).queue();
                    }
                    int size = customCommands.size();
                    customCommands.forEach(CustomCommand::deleteAsync);
                    customCommands.forEach(c -> CustomCmds.customCommands.remove(c.getId()));
                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.clear.success"), EmoteReference.PENCIL, size).queue();
                    return;
                }

                if(args.length < 2) {
                    onHelp(event);
                    return;
                }

                String cmd = args[1];

                if(action.equals("make")) {
                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                        return;
                    }

                    List<String> responses = new ArrayList<>();
                    boolean created = InteractiveOperations.create(
                            event.getChannel(), event.getAuthor().getIdLong(),60, e -> {
                                if(!e.getAuthor().equals(event.getAuthor()))
                                    return Operation.IGNORED;

                                String c = e.getMessage().getContentRaw();
                                if(!c.startsWith("&"))
                                    return Operation.IGNORED;
                                c = c.substring(1);

                                boolean nsfw = false;

                                if(c.startsWith("~>cancel") || c.startsWith("~>stop")) {
                                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.make.cancel"), EmoteReference.CORRECT).queue();
                                    return Operation.COMPLETED;
                                }

                                if(c.startsWith("~>nsfw")) {
                                    nsfw = true;
                                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.nsfw_enabled"), EmoteReference.CORRECT).queue();
                                    return Operation.RESET_TIMEOUT;
                                }

                                if(c.startsWith("~>sfw")) {
                                    nsfw = false;
                                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.nsfw_disabled"), EmoteReference.CORRECT).queue();
                                    return Operation.RESET_TIMEOUT;
                                }

                                if(c.startsWith("~>save")) {
                                    String arg = c.substring(6).trim();
                                    String saveTo = !arg.isEmpty() ? arg : cmd;

                                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                                        event.getChannel().sendMessageFormat(languageContext.get("general.invalid_character"), EmoteReference.ERROR).queue();
                                        return Operation.RESET_TIMEOUT;
                                    }

                                    if(cmd.length() >= 100) {
                                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.name_too_long"), EmoteReference.ERROR).queue();
                                        return Operation.RESET_TIMEOUT;
                                    }

                                    if(DefaultCommandProcessor.REGISTRY.commands().containsKey(saveTo)) {
                                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.already_exists"), EmoteReference.ERROR).queue();
                                        return Operation.RESET_TIMEOUT;
                                    }

                                    if(responses.isEmpty()) {
                                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.make.no_responses_added"), EmoteReference.ERROR).queue();
                                    } else {
                                        CustomCommand custom = CustomCommand.of(event.getGuild().getId(), cmd, responses);
                                        custom.getData().setOwner(event.getAuthor().getId());
                                        custom.getData().setNsfw(nsfw);
                                        //save at DB
                                        custom.saveAsync();

                                        //reflect at local
                                        customCommands.put(custom.getId(), custom);

                                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.make.success"), EmoteReference.CORRECT, cmd).queue();

                                        //easter egg :D
                                        TextChannelGround.of(event).dropItemWithChance(8, 2);
                                    }
                                    return Operation.COMPLETED;
                                }

                                responses.add(c.replace("@everyone", "[nice meme]").replace("@here", "[you tried]"));
                                e.getMessage().addReaction(EmoteReference.CORRECT.getUnicode()).queue();
                                return Operation.RESET_TIMEOUT;
                            }) != null;

                    if(created) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.make.start_text"), EmoteReference.PENCIL, cmd).queue();
                    } else {
                        event.getChannel().sendMessageFormat(languageContext.get("general.interactive_running"), EmoteReference.ERROR).queue();
                    }

                    return;
                }

                if(action.equals("eval")) {
                    try {
                        new CustomCommandHandler(event, languageContext, content.replace(action + " ", "")).handle(true);
                    } catch (Exception e) {
                        event.getChannel().sendMessage(String.format(languageContext.get("commands.custom.eval.error"), EmoteReference.ERROR, e.getMessage() == null ? "" : " (E: " + e.getMessage() + ")")).queue();
                    }

                    return;
                }

                if(action.equals("remove") || action.equals("rm")) {
                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                        return;
                    }

                    CustomCommand custom = db().getCustomCommand(event.getGuild(), cmd);
                    if(custom == null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.not_found"), EmoteReference.ERROR2, cmd).queue();
                        return;
                    }

                    //delete at DB
                    custom.deleteAsync();

                    //reflect at local
                    customCommands.remove(custom.getId());

                    //clear commands if none
                    if(customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + cmd)))
                        DefaultCommandProcessor.REGISTRY.commands().remove(cmd);

                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.remove.success"), EmoteReference.PENCIL, cmd).queue();

                    return;
                }

                if(action.equals("import")) {
                    if(!NAME_WILDCARD_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                        return;
                    }

                    Map<String, Guild> mapped = MantaroBot.getInstance().getMutualGuilds(event.getAuthor()).stream()
                            .collect(Collectors.toMap(ISnowflake::getId, g -> g));

                    List<Pair<Guild, CustomCommand>> filtered = MantaroData.db()
                            .getCustomCommandsByName(("*" + cmd + "*").replace("*", any)).stream()
                            .map(customCommand -> {
                                Guild guild = mapped.get(customCommand.getGuildId());
                                return guild == null ? null : Pair.of(guild, customCommand);
                            })
                            .filter(Objects::nonNull)

                            .collect(Collectors.toList());

                    if(filtered.size() == 0) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.import.not_found"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DiscordUtils.selectList(
                            event, filtered,
                            pair -> String.format(languageContext.get("commands.custom.import.header"), pair.getValue().getName(), pair.getKey()),
                            s -> baseEmbed(event, languageContext.get("commands.custom.import.selection")).setDescription(s)
                                    .setFooter(
                                            languageContext.get("commands.custom.import.note"),
                                            null
                                    ).build(),
                            pair -> {
                                CustomCommand custom = CustomCommand.transfer(event.getGuild().getId(), pair.getValue());
                                //save at DB
                                custom.saveAsync();

                                //reflect at local
                                customCommands.put(custom.getId(), custom);

                                event.getChannel().sendMessageFormat(languageContext.get("commands.custom.import.success"),
                                        custom.getName(), pair.getKey().getName(), custom.getValues().size()
                                ).queue();

                                //easter egg :D
                                TextChannelGround.of(event).dropItemWithChance(8, 2);
                            }
                    );

                    return;
                }

                if(action.equals("info")) {
                    CustomCommand command = db().getCustomCommand(event.getGuild(), cmd);
                    String owner = command.getData().getOwner();
                    User user = owner.isEmpty() ? null : MantaroBot.getInstance().getUserCache().getElementById(owner);
                    event.getChannel().sendMessage(new EmbedBuilder()
                            .setAuthor("Custom Command Information for " + cmd, null, event.getAuthor().getEffectiveAvatarUrl())
                            .setDescription(
                                    EmoteReference.BLUE_SMALL_MARKER + "**Owner:** " + (user == null ? "Nobody" : user.getName() + "#" + user.getDiscriminator()) + "\n" +
                                    EmoteReference.BLUE_SMALL_MARKER + "**Owner ID:** " + (user == null ? "None" : user.getId()) + "\n" +
                                    EmoteReference.BLUE_SMALL_MARKER + "**NSFW:** " + command.getData().isNsfw() + "\n" +
                                    EmoteReference.BLUE_SMALL_MARKER + "**Responses:** " + command.getValues().size() + "\n"
                            )
                            .setThumbnail("https://i.imgur.com/jPL5Lof.png")
                            .build()
                    ).queue();
                    return;
                }

                if(args.length < 3) {
                    onHelp(event);
                    return;
                }

                String value = args[2];

                if(action.equals("edit")) {
                    CustomCommand custom = db().getCustomCommand(event.getGuild(), cmd);
                    if(custom == null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.not_found"), EmoteReference.ERROR2, cmd).queue();
                        return;
                    }

                    String[] vals = StringUtils.splitArgs(value, 2);
                    int where;

                    try {
                        where = Math.abs(Integer.parseInt(vals[0]));
                    } catch(NumberFormatException e) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.edit.invalid_number"), EmoteReference.ERROR).queue();
                        return;
                    }

                    List<String> values = custom.getValues();
                    if(where - 1 > values.size()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.edit.no_index"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(vals[1].isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.edit.empty_response"), EmoteReference.ERROR).queue();
                        return;
                    }

                    custom.getValues().set(where - 1, vals[1]);

                    custom.saveAsync();
                    customCommands.put(custom.getId(), custom);

                    event.getChannel().sendMessage(String.format(languageContext.get("commands.custom.edit.success"), EmoteReference.CORRECT, where, custom.getName())).queue();
                    return;
                }

                if(action.equals("rename")) {
                    if(!NAME_PATTERN.matcher(cmd).matches() || !NAME_PATTERN.matcher(value).matches()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(DefaultCommandProcessor.REGISTRY.commands().containsKey(value)) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.already_exists"), EmoteReference.ERROR).queue();
                        return;
                    }

                    CustomCommand oldCustom = db().getCustomCommand(event.getGuild(), cmd);

                    if(oldCustom == null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.not_found"), EmoteReference.ERROR2, cmd).queue();
                        return;
                    }

                    CustomCommand newCustom = CustomCommand.of(event.getGuild().getId(), value, oldCustom.getValues());

                    //change at DB
                    oldCustom.deleteAsync();
                    newCustom.saveAsync();

                    //reflect at local
                    customCommands.remove(oldCustom.getId());
                    customCommands.put(newCustom.getId(), newCustom);

                    //clear commands if none
                    if(customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + cmd)))
                        DefaultCommandProcessor.REGISTRY.commands().remove(cmd);

                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.rename.success"), EmoteReference.CORRECT, cmd, value).queue();

                    //easter egg :D
                    TextChannelGround.of(event).dropItemWithChance(8, 2);
                    return;
                }

                if(action.equals("add") || action.equals("new")) {
                    Map<String, Optional<String>> opts = br.com.brjdevs.java.utils.texts.StringUtils.parse(content.split(" "));
                    String value1 = Utils.replaceArguments(opts, value, "nsfw");

                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(cmd.length() >= 100) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.name_too_long"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(DefaultCommandProcessor.REGISTRY.commands().containsKey(cmd)) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.custom.already_exists"), EmoteReference.ERROR, cmd).queue();
                        return;
                    }

                    CustomCommand custom = CustomCommand.of(
                            event.getGuild().getId(), cmd,
                            Collections.singletonList(value1.replace("@everyone", "[nice meme]").replace("@here", "[you tried]")));

                    if(action.equals("add")) {
                        CustomCommand c = db().getCustomCommand(event, cmd);

                        if(c != null) custom.getValues().addAll(c.getValues());
                    }

                    custom.getData().setOwner(event.getAuthor().getId());
                    if(opts.containsKey("nsfw")) {
                        custom.getData().setNsfw(true);
                    }

                    //save at DB
                    custom.saveAsync();

                    //reflect at local
                    customCommands.put(custom.getId(), custom);

                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.add.success"), EmoteReference.CORRECT, cmd).queue();

                    //easter egg :D
                    TextChannelGround.of(event).dropItemWithChance(8, 2);
                    return;
                }

                event.getChannel().sendMessageFormat(languageContext.get("commands.custom.invalid"), EmoteReference.ERROR).queue();
            }

            @Override
            public String[] splitArgs(String content) {
                return SPLIT_PATTERN.split(content, 3);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "CustomCommand Manager")
                        .setDescription("**Manages the Custom Commands of the Guild.**")
                        .addField("Guide", "https://github.com/Mantaro/MantaroBot/wiki/Custom-Commands", false)
                        .addField(
                                "Usage:",
                                "`~>custom` - Shows this help\n" +
                                        "`~>custom <list|ls>` - **List all commands. If detailed is supplied, it prints the responses of each command.**\n" +
                                        "`~>custom clear` - **Remove all Custom Commands from this Guild. (ADMIN-ONLY)**\n" +
                                        "`~>custom add <name> <response>` - **Creates or adds the response provided to a custom command.**\n" +
                                        "`~>custom make <name>` - **Starts a Interactive Operation to create a command with the specified name.**\n" +
                                        "`~>custom <remove|rm> <name>` - **Removes a command with an specific name.**\n" +
                                        "`~>custom import <search>` - **Imports a command from another guild you're in.**\n" +
                                        "`~>custom eval <response>` - **Tests how a custom command response will look**\n" +
                                        "`~>custom edit <name> <response number> <new content>` - **Edits one response of the specified command**\n" +
                                        "`~>custom view <name> <response number>` - **Views the content of one response**\n" +
                                        "`~>custom rename <previous name> <new name>` - **Renames a custom command**",
                                false
                        )
                        .addField("Considerations", "If you wish to dissallow normal people from making custom commands, run `~>opts admincustom true`", false).build();
            }
        });
    }

    //Lazy-load custom commands into cache.
    private static CustomCommand getCustomCommand(String id, String name) {
        if(customCommands.containsKey(id + ":" + name)) {
            return customCommands.get(id + ":" + name);
        }

        CustomCommand custom = db().getCustomCommand(id, name);
        //yes
        if(custom == null)
            return null;

        if(!NAME_PATTERN.matcher(name).matches()) {
            String newName = INVALID_CHARACTERS_PATTERN.matcher(custom.getName()).replaceAll("_");
            log.info("Custom Command with Invalid Characters '%s' found. Replacing with '_'", custom.getName());

            custom.deleteAsync();
            custom = CustomCommand.of(custom.getGuildId(), newName, custom.getValues());
            custom.saveAsync();
        }

        if(DefaultCommandProcessor.REGISTRY.commands().containsKey(custom.getName())) {
            custom.deleteAsync();
            custom = CustomCommand.of(custom.getGuildId(), "_" + custom.getName(), custom.getValues());
            custom.saveAsync();
        }

        //add to registry
        customCommands.put(custom.getId(), custom);

        return custom;
    }
}
