/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.custom.CustomCommandHandler;
import net.kodehawa.mantarobot.commands.custom.v3.Parser;
import net.kodehawa.mantarobot.commands.custom.v3.SyntaxException;
import net.kodehawa.mantarobot.commands.info.stats.manager.CommandStatsManager;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.CustomCommand;
import net.kodehawa.mantarobot.db.entities.helpers.CustomCommandData;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.data.MantaroData.db;

@Module
@SuppressWarnings("unused")
public class CustomCmds {
    public final static Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+"),
            INVALID_CHARACTERS_PATTERN = Pattern.compile("[^a-zA-Z0-9_]"),
            NAME_WILDCARD_PATTERN = Pattern.compile("[a-zA-Z0-9_*]+");
    private static final Map<String, CustomCommand> customCommands = new ConcurrentHashMap<>();
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CustomCmds.class);
    private static SecureRandom random = new SecureRandom();
    
    
    public static boolean handle(String prefix, String cmdName, GuildMessageReceivedEvent event, I18nContext lang, String args) {
        CustomCommand customCommand = getCustomCommand(event.getGuild().getId(), cmdName);
        GuildData guildData = db().getGuild(event.getGuild()).getData();
        
        if(customCommand == null)
            return false;
        
        //CCS disable check start.
        if(guildData.getDisabledCommands().contains(cmdName)) {
            return false;
        }
        
        List<String> channelDisabledCommands = guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId());
        if(channelDisabledCommands != null && channelDisabledCommands.contains(cmdName)) {
            return false;
        }
        
        HashMap<String, List<String>> roleSpecificDisabledCommands = guildData.getRoleSpecificDisabledCommands();
        if(event.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCommands.computeIfAbsent(r.getId(), s -> new ArrayList<>()).contains(cmdName)) && !CommandPermission.ADMIN.test(event.getMember())) {
            return false;
        }
        //CCS disable check end.
        
        List<String> values = customCommand.getValues();
        if(customCommand.getData().isNsfw() && !event.getChannel().isNSFW()) {
            event.getChannel().sendMessageFormat(lang.get("commands.custom.nsfw_not_nsfw"), EmoteReference.ERROR).queue();
            return true;
        }
        
        CommandStatsManager.log("custom command");
        
        String response = values.get(random.nextInt(values.size()));
        try {
            new CustomCommandHandler(prefix, event, lang, response, args).handle();
        } catch(SyntaxException e) {
            new MessageBuilder().append(String.format(lang.get("commands.custom.error_running_new"), EmoteReference.ERROR, e.getMessage()))
                    .sendTo(event.getChannel())
                    .queue();
        } catch(Exception e) {
            event.getChannel().sendMessageFormat(lang.get("commands.custom.error_running"), EmoteReference.ERROR).queue();
            e.printStackTrace();
        }
        
        return true;
    }
    
    //Lazy-load custom commands into cache.
    public static CustomCommand getCustomCommand(String id, String name) {
        //lol
        if(DefaultCommandProcessor.REGISTRY.commands().containsKey(name)) {
            return null;
        }
        
        if(customCommands.containsKey(id + ":" + name)) {
            return customCommands.get(id + ":" + name);
        }
        
        CustomCommand custom = db().getCustomCommand(id, name);
        //yes
        if(custom == null)
            return null;
        
        if(!NAME_PATTERN.matcher(name).matches()) {
            String newName = INVALID_CHARACTERS_PATTERN.matcher(custom.getName()).replaceAll("_");
            log.info("Custom Command with Invalid Characters {} found. Replacing with '_'", custom.getName());
            
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
    
    @Subscribe
    public void custom(CommandRegistry cr) {
        String any = "[\\d\\D]*?";
        final ManagedDatabase db = db();
        
        //People spamming crap... we cant have nice things owo
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                                                          .spamTolerance(2)
                                                          .limit(1)
                                                          .cooldown(4, TimeUnit.SECONDS)
                                                          .cooldownPenaltyIncrease(4, TimeUnit.SECONDS)
                                                          .maxCooldown(2, TimeUnit.MINUTES)
                                                          .pool(MantaroData.getDefaultJedisPool())
                                                          .prefix("custom")
                                                          .build();
        
        SimpleTreeCommand customCommand = (SimpleTreeCommand) cr.register("custom", new SimpleTreeCommand(Category.UTILS) {
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Manages the Custom Commands of the Guild. If you wish to allow normal people to make custom commands, run `~>opts admincustom false` (it's locked to admins by default)")
                               .setUsage("`~>custom <sub command>`")
                               .build();
            }
        });
        
        customCommand.setPredicate(e -> Utils.handleDefaultIncreasingRatelimit(rateLimiter, e.getAuthor(), e, null));
        
        //Just so this is in english.
        I18nContext i18nTemp = new I18nContext(null, null);
        Predicate<GuildMessageReceivedEvent> adminPredicate = (event) -> {
            if(db().getGuild(event.getGuild()).getData().isCustomAdminLockNew() && !CommandPermission.ADMIN.test(event.getMember())) {
                event.getChannel().sendMessage(i18nTemp.get("commands.custom.admin_only")).queue();
                return false;
            }
            
            return true;
        };
        
        customCommand.addSubCommand("list", new SubCommand() {
            @Override
            public String description() {
                return "Lists all the current commands on this server.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String filter = event.getGuild().getId() + ":";
                List<String> commands = db().getCustomCommands(event.getGuild())
                                                .stream()
                                                .map(CustomCommand::getName)
                                                .collect(Collectors.toList());
                
                EmbedBuilder builder = new EmbedBuilder()
                                               .setAuthor(languageContext.get("commands.custom.ls.header"), null, event.getGuild().getIconUrl())
                                               .setColor(event.getMember().getColor())
                                               .setThumbnail("https://images.emojiterra.com/twitter/v11/512px/1f6e0.png")
                                               .setDescription(languageContext.get("commands.custom.ls.description") + "\n" +
                                                                       (commands.isEmpty() ? languageContext.get("general.dust") :
                                                                                checkString(commands.stream().map(cc -> "*`" + cc + "`*").collect(Collectors.joining(", "))
                                                                                ))
                                               ).setFooter(String.format(languageContext.get("commands.custom.ls.footer"), commands.size()), event.getAuthor().getEffectiveAvatarUrl());
                
                event.getChannel().sendMessage(builder.build()).queue();
            }
        }).createSubCommandAlias("list", "ls");
        
        customCommand.addSubCommand("view", new SubCommand() {
            @Override
            public String description() {
                return "Views the response of an specific command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                String[] args = StringUtils.splitArgs(content, 2);
                if(args.length < 2) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.view.not_found"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String cmd = args[0];
                CustomCommand command = db().getCustomCommand(event.getGuild(), cmd);
                
                if(command == null) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.view.not_found"), EmoteReference.ERROR).queue();
                    return;
                }
                
                int number;
                try {
                    number = Integer.parseInt(args[1]) - 1;
                } catch(NumberFormatException e) {
                    channel.sendMessageFormat(languageContext.get("general.invalid_number"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(command.getValues().size() < number) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.view.less_than_specified"), EmoteReference.ERROR).queue();
                    return;
                }
                
                channel.sendMessageFormat(languageContext.get("commands.custom.view.success"), (number + 1), command.getName(), command.getValues().get(number)).queue();
                
            }
        }).createSubCommandAlias("view", "vw");
        
        customCommand.addSubCommand("raw", new SubCommand() {
            @Override
            public String description() {
                return "Show all the raw responses of the specified command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                String command = content.trim();
                if(command.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.raw.no_command"), EmoteReference.ERROR).queue();
                    return;
                }
                
                CustomCommand custom = db().getCustomCommand(event.getGuild(), command);
                if(custom == null) {
                    new MessageBuilder()
                            .setContent(String.format(languageContext.get("commands.custom.not_found"), EmoteReference.ERROR2, command))
                            .stripMentions(event.getJDA())
                            .sendTo(channel)
                            .queue();
                    
                    return;
                }
                
                List<MessageEmbed.Field> fields = new ArrayList<>();
                AtomicInteger count = new AtomicInteger();
                for(String value : custom.getValues()) {
                    String val = value;
                    if(value.length() > 900)
                        val = Utils.paste(value);
                    
                    fields.add(new MessageEmbed.Field("Response N° " + count.incrementAndGet(), val, true));
                }
                
                EmbedBuilder embed = baseEmbed(event, String.format(languageContext.get("commands.custom.raw.header"), command))
                                             .setDescription(languageContext.get("commands.custom.raw.description"))
                                             .setFooter(String.format(languageContext.get("commands.custom.raw.amount"), 6, custom.getValues().size()), null);
                
                List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(6, fields);
                
                boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ADD_REACTION);
                if(hasReactionPerms) {
                    embed.appendDescription("\n" + String.format(languageContext.get("general.buy_sell_paged_react"), splitFields.size(), ""));
                    DiscordUtils.list(event, 100, false, embed, splitFields);
                } else {
                    embed.appendDescription("\n" + String.format(languageContext.get("general.buy_sell_paged_text"), splitFields.size(), ""));
                    DiscordUtils.listText(event, 100, false, embed, splitFields);
                }
            }
        }).createSubCommandAlias("raw", "rw");
        
        customCommand.addSubCommand("clear", new SubCommand() {
            @Override
            public String description() {
                return "Clear all custom commands.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!adminPredicate.test(event)) {
                    return;
                }
                
                TextChannel channel = event.getChannel();
                List<CustomCommand> customCommands = db().getCustomCommands(event.getGuild());
                
                if(customCommands.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.no_cc"), EmoteReference.ERROR).queue();
                }
                int size = customCommands.size();
                customCommands.forEach(CustomCommand::deleteAsync);
                customCommands.forEach(c -> CustomCmds.customCommands.remove(c.getId()));
                channel.sendMessageFormat(languageContext.get("commands.custom.clear.success"), EmoteReference.PENCIL, size).queue();
            }
        }).createSubCommandAlias("clear", "clr");
        
        customCommand.addSubCommand("eval", new SubCommand() {
            @Override
            public String description() {
                return "Evaluates the result of a custom command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!adminPredicate.test(event)) {
                    return;
                }
                
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.eval.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }
                
                try {
                    String ctn = content;
                    ctn = Utils.DISCORD_INVITE.matcher(ctn).replaceAll("-invite link-");
                    ctn = Utils.DISCORD_INVITE_2.matcher(ctn).replaceAll("-invite link-");
                    
                    //Sadly no way to get the prefix used, so eval will have the old bug still.
                    new CustomCommandHandler("", event, languageContext, ctn).handle(true);
                } catch(SyntaxException e) {
                    new MessageBuilder().append(String.format(languageContext.get("commands.custom.eval.new_error"), EmoteReference.ERROR, e.getMessage()))
                            .sendTo(event.getChannel())
                            .queue();
                } catch(Exception e) {
                    new MessageBuilder().append(String.format(languageContext.get("commands.custom.eval.error"), EmoteReference.ERROR, e.getMessage() == null ? "" : " (E: " + e.getMessage() + ")"))
                            .sendTo(event.getChannel())
                            .queue();
                }
            }
        }).createSubCommandAlias("eval", "evl");
        
        customCommand.addSubCommand("remove", new SubCommand() {
            @Override
            public String description() {
                return "Removes a custom command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!adminPredicate.test(event)) {
                    return;
                }
                
                TextChannel channel = event.getChannel();
                
                if(content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.remove.no_command"), EmoteReference.ERROR).queue();
                    return;
                }
    
                if(!NAME_PATTERN.matcher(content).matches()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                    return;
                }
                
                //hint: always check for this
                if(DefaultCommandProcessor.REGISTRY.commands().containsKey(content)) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.already_exists"), EmoteReference.ERROR, content).queue();
                    return;
                }
                
                CustomCommand custom = getCustomCommand(event.getGuild().getId(), content);
                if(custom == null) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.not_found"), EmoteReference.ERROR2, content).queue();
                    return;
                }
                
                //delete at DB
                custom.deleteAsync();
                
                //reflect at local
                customCommands.remove(custom.getId());
                
                //clear commands if none
                if(customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + content)))
                    customCommands.remove(content);
                
                channel.sendMessageFormat(languageContext.get("commands.custom.remove.success"), EmoteReference.PENCIL, content).queue();
            }
        }).createSubCommandAlias("remove", "rm");
        
        customCommand.addSubCommand("import", new SubCommand() {
            @Override
            public String description() {
                return "Imports a custom command from another server you're in.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!adminPredicate.test(event)) {
                    return;
                }
                
                TextChannel channel = event.getChannel();
                
                if(content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.import.no_command"), EmoteReference.ERROR).queue();
                    return;
                }
    
                if(!NAME_WILDCARD_PATTERN.matcher(content).matches()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Map<String, Guild> mapped = MantaroBot.getInstance()
                                                    .getShardManager()
                                                    .getMutualGuilds(event.getAuthor()).stream()
                                                    .collect(Collectors.toMap(ISnowflake::getId, g -> g));
                
                List<Pair<Guild, CustomCommand>> filtered = db
                                                                    .getCustomCommandsByName(("*" + content + "*").replace("*", any)).stream()
                                                                    .map(customCommand -> {
                                                                        Guild guild = mapped.get(customCommand.getGuildId());
                                                                        return guild == null ? null : Pair.of(guild, customCommand);
                                                                    })
                                                                    .filter(Objects::nonNull)
                        
                                                                    .collect(Collectors.toList());
                
                if(filtered.size() == 0) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.import.not_found"), EmoteReference.ERROR).queue();
                    return;
                }
                
                DiscordUtils.selectList(
                        event, filtered,
                        pair -> String.format(languageContext.get("commands.custom.import.header"), pair.getValue().getName(), pair.getRight().getValues().size(), pair.getKey()),
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
                            
                            channel.sendMessageFormat(languageContext.get("commands.custom.import.success"),
                                    custom.getName(), pair.getKey().getName(), custom.getValues().size()
                            ).queue();
                            
                            //easter egg :D
                            TextChannelGround.of(event).dropItemWithChance(8, 2);
                        }
                );
            }
        }).createSubCommandAlias("import", "ipt");
        
        customCommand.addSubCommand("info", new SubCommand() {
            @Override
            public String description() {
                return "Shows the information about an specific command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.raw.no_command"), EmoteReference.ERROR).queue();
                    return;
                }
    
                CustomCommand command = db().getCustomCommand(event.getGuild(), content);
                String owner = command.getData().getOwner();
                User user = owner.isEmpty() ? null : MantaroBot.getInstance().getShardManager().getUserCache().getElementById(owner);
                event.getChannel().sendMessage(new EmbedBuilder()
                                                       .setAuthor("Custom Command Information for " + content, null, event.getAuthor().getEffectiveAvatarUrl())
                                                       .setDescription(
                                                               EmoteReference.BLUE_SMALL_MARKER + "**Owner:** " + (user == null ? "Nobody" : user.getName() + "#" + user.getDiscriminator()) + "\n" +
                                                                       EmoteReference.BLUE_SMALL_MARKER + "**Owner ID:** " + (user == null ? "None" : user.getId()) + "\n" +
                                                                       EmoteReference.BLUE_SMALL_MARKER + "**NSFW:** " + command.getData().isNsfw() + "\n" +
                                                                       EmoteReference.BLUE_SMALL_MARKER + "**Responses:** " + command.getValues().size() + "\n"
                                                       )
                                                       .setThumbnail("https://i.imgur.com/jPL5Lof.png")
                                                       .build()
                ).queue();
                
            }
        });
        
        customCommand.addSubCommand("edit", new SubCommand() {
            @Override
            public String description() {
                return "Edits the response of a command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!adminPredicate.test(event)) {
                    return;
                }
                
                TextChannel channel = event.getChannel();
                
                if(content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.edit.no_command"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Map<String, String> opts = new HashMap<>();
                try {
                    opts = getArguments(content);
                } catch(StringIndexOutOfBoundsException ignore) {
                }
                String ctn = Utils.replaceArguments(opts, content, "nsfw");
                
                String[] args = StringUtils.splitArgs(ctn, -1);
                if(args.length < 2) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.edit.not_enough_args"), EmoteReference.ERROR).queue();
                    return;
                }
                var cmd = args[0];
                CustomCommand custom = db().getCustomCommand(event.getGuild(), cmd);
                if(custom == null) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.not_found"), EmoteReference.ERROR2, args[0]).queue();
                    return;
                }
                
                int where;
                String index = args[1];
                //replace first occurrence and second argument: custom command and index.
                String commandContent = ctn.replaceFirst(cmd, "").replaceFirst(index, "").trim();
                try {
                    where = Math.abs(Integer.parseInt(index));
                } catch(NumberFormatException e) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.edit.invalid_number"), EmoteReference.ERROR).queue();
                    return;
                }
                
                List<String> values = custom.getValues();
                if(where - 1 > values.size()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.edit.no_index"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(commandContent.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.edit.empty_response"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(opts.containsKey("nsfw")) {
                    custom.getData().setNsfw(true);
                }
                
                custom.getValues().set(where - 1, commandContent);
                
                custom.saveAsync();
                customCommands.put(custom.getId(), custom);
                
                channel.sendMessage(String.format(languageContext.get("commands.custom.edit.success"), EmoteReference.CORRECT, where, custom.getName())).queue();
            }
        });
        
        customCommand.addSubCommand("deleteresponse", new SubCommand() {
            @Override
            public String description() {
                return "Deletes a response of a command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!adminPredicate.test(event)) {
                    return;
                }
                
                TextChannel channel = event.getChannel();
                
                if(content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.deleteresponse.no_command"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String[] args = StringUtils.splitArgs(content, -1);
                if(args.length < 1) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.deleteresponse.not_enough_args"), EmoteReference.ERROR).queue();
                    return;
                }
                
                CustomCommand custom = db().getCustomCommand(event.getGuild(), args[0]);
                if(custom == null) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.not_found"), EmoteReference.ERROR2, args[0]).queue();
                    return;
                }
                
                int where;
                String index = args[1];
                try {
                    where = Math.abs(Integer.parseInt(index));
                } catch(NumberFormatException e) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.deleteresponse.invalid_number"), EmoteReference.ERROR).queue();
                    return;
                }
                
                List<String> values = custom.getValues();
                if(where - 1 > values.size()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.deleteresponse.no_index"), EmoteReference.ERROR).queue();
                    return;
                }
                
                custom.getValues().remove(where - 1);
                
                custom.saveAsync();
                customCommands.put(custom.getId(), custom);
                
                channel.sendMessage(String.format(languageContext.get("commands.custom.deleteresponse.success"), EmoteReference.CORRECT, where, custom.getName())).queue();
            }
        }).createSubCommandAlias("deleteresponse", "dlr");
        
        customCommand.addSubCommand("rename", new SubCommand() {
            @Override
            public String description() {
                return "Renames a custom command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!adminPredicate.test(event)) {
                    return;
                }
                
                TextChannel channel = event.getChannel();
                
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.rename.no_command"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String[] args = StringUtils.splitArgs(content, -1);
                
                if(args.length < 2) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.custom.rename.not_enough_args"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String cmd = args[0];
                String value = args[1];
                
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
                
                final CustomCommandData oldCustomData = oldCustom.getData();
                newCustom.getData().setNsfw(oldCustomData.isNsfw());
                newCustom.getData().setOwner(oldCustomData.getOwner());
                
                //change at DB
                oldCustom.deleteAsync();
                newCustom.saveAsync();
                
                //reflect at local
                customCommands.remove(oldCustom.getId());
                customCommands.put(newCustom.getId(), newCustom);
                
                //clear commands if none
                if(customCommands.keySet().stream().noneMatch(s -> s.endsWith(":" + cmd)))
                    customCommands.remove(cmd);
                
                event.getChannel().sendMessageFormat(languageContext.get("commands.custom.rename.success"), EmoteReference.CORRECT, cmd, value).queue();
                
                //easter egg :D
                TextChannelGround.of(event).dropItemWithChance(8, 2);
                
            }
        }).createSubCommandAlias("rename", "rn");
        
        customCommand.addSubCommand("add", new SubCommand() {
            @Override
            public String description() {
                return "Adds a new custom commands or adds a response to an existing command.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!adminPredicate.test(event)) {
                    return;
                }
                
                TextChannel channel = event.getChannel();
                
                if(content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.add.no_command"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String[] args = StringUtils.splitArgs(content, -1);
                
                if(args.length < 2) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.add.not_enough_args"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String cmd = args[0];
                String value = content.replaceFirst(args[0], "").trim();
                
                Map<String, String> opts = new HashMap<>();
                try {
                    opts = getArguments(content);
                } catch(StringIndexOutOfBoundsException ignore) {
                }
                String cmdSource = Utils.replaceArguments(opts, value, "nsfw");
                
                if(cmdSource.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.add.empty_content"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(!NAME_PATTERN.matcher(cmd).matches()) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.character_not_allowed"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(cmd.length() >= 50) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.name_too_long"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(DefaultCommandProcessor.REGISTRY.commands().containsKey(cmd)) {
                    channel.sendMessageFormat(languageContext.get("commands.custom.already_exists"), EmoteReference.ERROR, cmd).queue();
                    return;
                }
                
                Guild guild = event.getGuild();
                cmdSource = cmdSource.replace("@everyone", "[nice meme]").replace("@here", "[you tried]");
                
                if(cmdSource.contains("v3:")) {
                    try {
                        new Parser(cmdSource).parse();
                    } catch(SyntaxException e) {
                        channel.sendMessageFormat(languageContext.get("commands.custom.new_error"), EmoteReference.ERROR, e.getMessage()).queue();
                        return;
                    }
                }
                
                CustomCommand custom = CustomCommand.of(guild.getId(), cmd, Collections.singletonList(cmdSource));
                CustomCommand c = db().getCustomCommand(event, cmd);
                
                if(c != null) {
                    custom.getValues().addAll(c.getValues());
                } else {
                    //Are the first two checks redundant?
                    if(!getConfig().isPremiumBot && !db.getGuild(guild).isPremium() && db.getCustomCommands(guild).size() > 100) {
                        channel.sendMessageFormat(languageContext.get("commands.custom.add.too_many_commands"), EmoteReference.ERROR).queue();
                        return;
                    }
                }
                
                
                custom.getData().setOwner(event.getAuthor().getId());
                if(opts.containsKey("nsfw")) {
                    custom.getData().setNsfw(true);
                }
                
                //save at DB
                custom.saveAsync();
                //reflect at local
                customCommands.put(custom.getId(), custom);
                
                channel.sendMessageFormat(languageContext.get("commands.custom.add.success"), EmoteReference.CORRECT, cmd).queue();
                
                //easter egg :D
                TextChannelGround.of(event).dropItemWithChance(8, 2);
            }
        }).createSubCommandAlias("add", "new");
    }
}
