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

import br.com.brjdevs.java.utils.async.Async;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.custom.CustomCommandHandler;
import net.kodehawa.mantarobot.commands.info.stats.manager.CommandStatsManager;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
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
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;
import static net.kodehawa.mantarobot.data.MantaroData.db;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

@Slf4j
@Module
public class CustomCmds {
    private static final Map<String, List<String>> customCommands = new ConcurrentHashMap<>();
    private final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+"),
            INVALID_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9_]"),
            NAME_WILDCARD_PATTERN = Pattern.compile("[a-zA-Z0-9_*]+");

    public static boolean handle(String cmdName, GuildMessageReceivedEvent event, I18nContext lang, String args) {
        List<String> values = customCommands.get(event.getGuild().getId() + ":" + cmdName);
        if(values == null) return false;

        CommandStatsManager.log("custom command");

        String response = random(values).replace("@everyone", "\u200Deveryone").replace("@here", "\u200Dhere");
        try {
            new CustomCommandHandler(event, lang, response, args).handle();
        } catch (Exception e) {
            event.getChannel().sendMessage(String.format("%sError while running custom command... please check the response content and length (cannot be more than 2000 chars).", EmoteReference.ERROR)).queue();
        }

        return true;
    }

    @Subscribe
    public void custom(CommandRegistry cr) {
        String any = "[\\d\\D]*?";

        cr.register("custom", new SimpleCommand(Category.UTILS) {
            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(args.length < 1) {
                    onHelp(event);
                    return;
                }

                String action = args[0];

                if(action.equals("list") || action.equals("ls")) {

                    if(!MantaroCore.hasLoadedCompletely()) {
                        event.getChannel().sendMessage("The bot hasn't been fully booted up yet... custom commands will be available shortly!").queue();
                        return;
                    }

                    String filter = event.getGuild().getId() + ":";
                    List<String> commands = customCommands.keySet().stream()
                            .filter(s -> s.startsWith(filter))
                            .map(s -> s.substring(filter.length()))
                            .collect(Collectors.toList());

                    EmbedBuilder builder = new EmbedBuilder()
                            .setAuthor("Commands for this guild", null, event.getGuild().getIconUrl())
                            .setColor(event.getMember().getColor());
                    builder.setDescription(
                            commands.isEmpty() ? "There is nothing here, just dust." : checkString(forType(commands)));

                    event.getChannel().sendMessage(builder.build()).queue();
                    return;
                }

                if(action.equals("view")) {
                    if(args.length < 2) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the command and the response number!").queue();
                        return;
                    }

                    String cmd = args[1];
                    CustomCommand command = db().getCustomCommand(event.getGuild(), cmd);

                    if(command == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't a custom command with that name here!").queue();
                        return;
                    }

                    int number;

                    try {
                        number = Integer.parseInt(args[2]) - 1;
                    } catch(NumberFormatException e) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a number...").queue();
                        return;
                    }

                    if(command.getValues().size() < number) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "This commands has less responses than the number you specified...").queue();
                        return;
                    }

                    event.getChannel().sendMessage(String.format("**Response `%d` for custom command `%s`:** \n```\n%s```", (number + 1),
                            command.getName(), command.getValues().get(number))).queue();

                    return;
                }

                if(action.equals("raw")) {
                    if(args.length < 2) {
                        onHelp(event);
                        return;
                    }

                    String cmd = args[1];
                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not allowed character.").queue();
                        return;
                    }

                    CustomCommand custom = db().getCustomCommand(event.getGuild(), cmd);
                    if(custom == null) {
                        event.getChannel().sendMessage(
                                String.format("%sThere's no Custom Command ``%s`` in this Guild.", EmoteReference.ERROR2, cmd)).queue();
                        return;
                    }

                    Pair<String, Integer> pair = DiscordUtils.embedList(custom.getValues(), Object::toString);

                    String pasted = null;

                    if(pair.getRight() < custom.getValues().size()) {
                        AtomicInteger i = new AtomicInteger();
                        pasted = Utils.paste(custom.getValues().stream().map(s -> i.incrementAndGet() + s).collect(Collectors.joining("\n")));
                    }

                    EmbedBuilder embed = baseEmbed(event, String.format("Command \"%s\":", cmd))
                            .setDescription(pair.getLeft())
                            .setFooter(
                                    String.format("(Showing %d responses of %d)", pair.getRight(), custom.getValues().size()), null);

                    if(pasted != null && pasted.contains("hastebin.com")) {
                        embed.addField("Pasted content", pasted, false);
                    }

                    event.getChannel().sendMessage(embed.build()).queue();
                    return;
                }

                if(db().getGuild(event.getGuild()).getData().isCustomAdminLock() && !CommandPermission.ADMIN.test(event.getMember())) {
                    event.getChannel().sendMessage("This guild only accepts custom command creation, edits, imports and eval from administrators.").queue();
                    return;
                }

                if(action.equals("clear")) {
                    if(!CommandPermission.ADMIN.test(event.getMember())) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot do that, silly.").queue();
                        return;
                    }

                    List<CustomCommand> customCommands = db().getCustomCommands(event.getGuild());

                    if(customCommands.isEmpty()) {
                        event.getChannel().sendMessage(
                                EmoteReference.ERROR + "There's no Custom Commands registered in this Guild, just dust.").queue();
                    }
                    int size = customCommands.size();
                    customCommands.forEach(CustomCommand::deleteAsync);
                    customCommands.forEach(c -> CustomCmds.customCommands.remove(c.getId()));
                    event.getChannel().sendMessage(String.format("%sCleared **%d Custom Commands**!", EmoteReference.PENCIL, size))
                            .queue();
                    return;
                }


                if(args.length < 2) {
                    onHelp(event);
                    return;
                }

                String cmd = args[1];

                if(action.equals("make")) {
                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not allowed character.").queue();
                        return;
                    }

                    List<String> responses = new ArrayList<>();
                    boolean created = InteractiveOperations.create(
                            event.getChannel(), event.getAuthor().getIdLong(),60, e -> {
                                if(!e.getAuthor().equals(event.getAuthor())) return Operation.IGNORED;

                                String c = e.getMessage().getContentRaw();
                                if(!c.startsWith("&")) return Operation.IGNORED;
                                c = c.substring(1);

                                if(c.startsWith("~>cancel") || c.startsWith("~>stop")) {
                                    event.getChannel().sendMessage(String.format("%sCommand Creation canceled.", EmoteReference.CORRECT))
                                            .queue();
                                    return Operation.COMPLETED;
                                }

                                if(c.startsWith("~>save")) {
                                    String arg = c.substring(6).trim();
                                    String saveTo = !arg.isEmpty() ? arg : cmd;

                                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                                        event.getChannel().sendMessage(String.format("%sNot allowed character.", EmoteReference.ERROR))
                                                .queue();
                                        return Operation.RESET_TIMEOUT;
                                    }

                                    if(cmd.length() >= 100) {
                                        event.getChannel().sendMessage(String.format("%sName is too long.", EmoteReference.ERROR))
                                                .queue();
                                        return Operation.RESET_TIMEOUT;
                                    }

                                    if(DefaultCommandProcessor.REGISTRY.commands().containsKey(saveTo)) {
                                        event.getChannel().sendMessage(String.format("%sA command already exists with this name!", EmoteReference.ERROR)).queue();
                                        return Operation.RESET_TIMEOUT;
                                    }

                                    if(responses.isEmpty()) {
                                        event.getChannel().sendMessage(
                                                String.format("%sNo responses were added. Stopping creation without saving...", EmoteReference.ERROR))
                                                .queue();
                                    } else {
                                        CustomCommand custom = CustomCommand.of(event.getGuild().getId(), cmd, responses);

                                        //save at DB
                                        custom.saveAsync();

                                        //reflect at local
                                        customCommands.put(custom.getId(), custom.getValues());

                                        event.getChannel().sendMessage(
                                                String.format("%sSaved to command ``%s``!", EmoteReference.CORRECT, cmd)).queue();

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
                        event.getChannel().sendMessage(
                                String.format("%sStarted **\"Creation of Custom Command ``%s``\"**!\nSend ``&~>stop`` to stop creation **without saving**.\nSend ``&~>save`` to stop creation an **save the new Command**. Send any text beginning with ``&`` to be added to the Command Responses.\nThis Interactive Operation ends without saving after 60 seconds of inactivity.", EmoteReference.PENCIL, cmd))
                                .queue();
                    } else {
                        //impossible as per 5.0?
                        event.getChannel().sendMessage(
                                String.format("%sThere's already an Interactive Operation happening on this channel.", EmoteReference.ERROR))
                                .queue();
                    }

                    return;
                }

                if(action.equals("eval")) {
                    try {
                        new CustomCommandHandler(event, languageContext, cmd).handle();
                    } catch (Exception e) {
                        event.getChannel().sendMessage(String.format("%sThere was an error while evaluating your command!%s", EmoteReference.ERROR, e.getMessage() == null ? "" : " (E: " + e.getMessage() + ")")).queue();
                    }

                    return;
                }

                if(action.equals("remove") || action.equals("rm")) {
                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not allowed character.").queue();
                        return;
                    }

                    CustomCommand custom = db().getCustomCommand(event.getGuild(), cmd);
                    if(custom == null) {
                        event.getChannel().sendMessage(
                                EmoteReference.ERROR2 + "There's no Custom Command ``" + cmd + "`` in this Guild.").queue();
                        return;
                    }

                    //delete at DB
                    custom.deleteAsync();

                    //reflect at local
                    customCommands.remove(custom.getId());

                    //clear commands if none
                    if(customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + cmd)))
                        DefaultCommandProcessor.REGISTRY.commands().remove(cmd);

                    event.getChannel().sendMessage(String.format("%sRemoved Custom Command ``%s``!", EmoteReference.PENCIL, cmd))
                            .queue();

                    return;
                }

                if(action.equals("import")) {
                    if(!NAME_WILDCARD_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not allowed character.").queue();
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
                        event.getChannel().sendMessage(String.format("%sThere are no custom commands matching your search query.", EmoteReference.ERROR)).queue();
                        return;
                    }

                    DiscordUtils.selectList(
                            event, filtered,
                            pair -> String.format("`%s` - Guild: `%s`", pair.getValue().getName(), pair.getKey()),
                            s -> baseEmbed(event, "Select the Command:").setDescription(s)
                                    .setFooter(
                                            "(You can only select custom commands from guilds that you are a member of)",
                                            null
                                    ).build(),
                            pair -> {
                                String cmdName = pair.getValue().getName();
                                List<String> responses = pair.getValue().getValues();
                                CustomCommand custom = CustomCommand.of(event.getGuild().getId(), cmdName, responses);

                                //save at DB
                                custom.saveAsync();

                                //reflect at local
                                customCommands.put(custom.getId(), custom.getValues());

                                event.getChannel().sendMessage(String
                                        .format("Imported custom command `%s` from guild `%s` with responses `%s`", cmdName,
                                                pair.getKey().getName(), String.join("``, ``", responses)
                                        )).queue();

                                //easter egg :D
                                TextChannelGround.of(event).dropItemWithChance(8, 2);
                            }
                    );

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
                        event.getChannel().sendMessage(
                                EmoteReference.ERROR2 + "There's no Custom Command ``" + cmd + "`` in this Guild.").queue();
                        return;
                    }

                    String[] vals = StringUtils.splitArgs(value, 2);
                    int where;

                    try {
                        where = Math.abs(Integer.parseInt(vals[0]));
                    } catch(NumberFormatException e) {
                        event.getChannel().sendMessage(String.format("%sYou need to specify a correct number to change!", EmoteReference.ERROR)).queue();
                        return;
                    }

                    List<String> values = custom.getValues();
                    if(where - 1 > values.size()) {
                        event.getChannel().sendMessage(String.format("%sYou cannot edit a non-existent index!", EmoteReference.ERROR)).queue();
                        return;
                    }

                    if(vals[1].isEmpty()) {
                        event.getChannel().sendMessage(String.format("%sCannot edit to an empty response!", EmoteReference.ERROR)).queue();
                        return;
                    }

                    custom.getValues().set(where - 1, vals[1]);

                    custom.saveAsync();
                    customCommands.put(custom.getId(), custom.getValues());

                    event.getChannel().sendMessage(String.format("%sEdited response **#%d** of the command `%s` correctly!", EmoteReference.CORRECT, where, custom.getName())).queue();
                    return;
                }

                if(action.equals("rename")) {
                    if(!NAME_PATTERN.matcher(cmd).matches() || !NAME_PATTERN.matcher(value).matches()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not allowed character.").queue();
                        return;
                    }

                    if(DefaultCommandProcessor.REGISTRY.commands().containsKey(value)) {
                        event.getChannel().sendMessage(String.format("%sA command already exists with this name!", EmoteReference.ERROR)).queue();
                        return;
                    }

                    CustomCommand oldCustom = db().getCustomCommand(event.getGuild(), cmd);

                    if(oldCustom == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR2 + "There's no Custom Command ``" + cmd + "`` in this Guild.").queue();
                        return;
                    }

                    CustomCommand newCustom = CustomCommand.of(event.getGuild().getId(), value, oldCustom.getValues());

                    //change at DB
                    oldCustom.deleteAsync();
                    newCustom.saveAsync();

                    //reflect at local
                    customCommands.remove(oldCustom.getId());
                    customCommands.put(newCustom.getId(), newCustom.getValues());

                    //clear commands if none
                    if(customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + cmd)))
                        DefaultCommandProcessor.REGISTRY.commands().remove(cmd);

                    event.getChannel().sendMessage(String.format("%sRenamed command ``%s`` to ``%s``!", EmoteReference.CORRECT, cmd, value)).queue();

                    //easter egg :D
                    TextChannelGround.of(event).dropItemWithChance(8, 2);
                    return;
                }

                if(action.equals("add") || action.equals("new")) {
                    if(!NAME_PATTERN.matcher(cmd).matches()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not allowed character.").queue();
                        return;
                    }

                    if(cmd.length() >= 100) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Name is too long.")
                                .queue();
                        return;
                    }

                    if(DefaultCommandProcessor.REGISTRY.commands().containsKey(cmd)) {
                        event.getChannel().sendMessage(
                                EmoteReference.ERROR + "A command already exists with this name!").queue();
                        return;
                    }

                    CustomCommand custom = CustomCommand.of(
                            event.getGuild().getId(), cmd,
                            Collections.singletonList(value.replace("@everyone", "[nice meme]").replace("@here", "[you tried]")));

                    if(action.equals("add")) {
                        CustomCommand c = db().getCustomCommand(event, cmd);

                        if(c != null) custom.getValues().addAll(c.getValues());
                    }

                    //save at DB
                    custom.saveAsync();

                    //reflect at local
                    customCommands.put(custom.getId(), custom.getValues());

                    event.getChannel().sendMessage(String.format("%sSaved to command ``%s``!", EmoteReference.CORRECT, cmd))
                            .queue();

                    //easter egg :D
                    TextChannelGround.of(event).dropItemWithChance(8, 2);
                    return;
                }

                onHelp(event);
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

    @Subscribe
    public void onPostLoad(PostLoadEvent e) {
        Async.thread(() -> db().getCustomCommands().forEach(custom -> {
            if(!NAME_PATTERN.matcher(custom.getName()).matches()) {
                String newName = INVALID_CHARACTERS_PATTERN.matcher(custom.getName()).replaceAll("_");
                log.info("Custom Command with Invalid Characters '%s' found. Replacing with '%'", custom.getName());

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
            customCommands.put(custom.getId(), custom.getValues());
        }));
    }
}
