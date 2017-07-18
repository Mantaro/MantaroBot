package net.kodehawa.mantarobot.core;

import com.rethinkdb.gen.exc.ReqlError;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.dataporter.oldentities.OldGuild;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.helpers.ExtraGuildData;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.commands.AliasCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.modules.commands.base.Command;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Snow64;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static net.kodehawa.mantarobot.utils.StringUtils.splitArgs;

@Slf4j
public class CommandProcessorAndRegistry implements CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("CommandProcessor");

    @Getter
    public static int commandTotal = 0;

    private final Map<String, Command> commands = new HashMap<>();

    public Map<String, Command> commands() {
        return commands;
    }

    public Command register(String s, Command c) {
        commands.putIfAbsent(s, c);
        return c;
    }

    public void registerAlias(String c, String o) {
        if(!commands.containsKey(c)) {
            System.out.println(c + " isn't in the command map...");
        }

        register(o, new AliasCommand(o, commands.get(c)));
    }

    public void run(GuildMessageReceivedEvent event) {
        //Global Blacklist
        if(MantaroData.db().getMantaroData().getBlackListedUsers().contains(event.getAuthor().getId())) return;

        //Stats, Apparently
        long start = System.currentTimeMillis();

        //Command Processing
        Config conf = MantaroData.config().get();
        String rawCmd = event.getMessage().getRawContent();
        String[] prefix = conf.prefix;
        String customPrefix = MantaroData.db().getGuild(event.getGuild()).getData().getGuildCustomPrefix();

        String usedPrefix = null;
        for(String s : prefix) {
            if(rawCmd.startsWith(s)) usedPrefix = s;
        }

        if(usedPrefix != null && rawCmd.startsWith(usedPrefix)) rawCmd = rawCmd.substring(usedPrefix.length());
        else if(customPrefix != null && rawCmd.startsWith(customPrefix))
            rawCmd = rawCmd.substring(customPrefix.length());
        else if(usedPrefix == null) return;

        String[] parts = splitArgs(rawCmd, 2);
        String cmdName = parts[0], content = parts[1];

        //Grab some stuff
        Command cmd = commands.get(cmdName);

        OldGuild dbg = MantaroData.db().getGuild(event.getGuild());
        ExtraGuildData data = dbg.getData();

        if(cmd == null) return;

        //CHECKS. A LOT OF THEM.
        if(!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_EMBED_LINKS)) {
            event.getChannel().sendMessage(EmoteReference.STOP + "I require the permission ``Embed Links``. " +
                    "All Commands will be refused until you give me that permission.\n" +
                    "http://i.imgur.com/Ydykxcy.gifv Refer to this on instructions on how to give the bot the permissions. " +
                    "Also check all the other roles the bot has have that permissions and remember to check channel-specific permissions. Thanks you.").queue();
            return;
        }

        if(data.getDisabledCommands().contains(cmdName)) {
            return;
        }

        if(data.getChannelSpecificDisabledCommands().get(event.getChannel().getId()) != null &&
                data.getChannelSpecificDisabledCommands().get(event.getChannel().getId()).contains(cmdName)) {
            return;
        }

        if(data.getDisabledUsers().contains(event.getAuthor().getId())) {
            return;
        }

        if(MantaroData.db().getGuild(event.getGuild()).getData().getDisabledChannels().contains(event.getChannel().getId()) && cmd
                .category() != Category.MODERATION) {
            return;
        }

        if(conf.isPremiumBot() && cmd.category() == Category.CURRENCY) {
            return;
        }

        if(data.getDisabledCategories().contains(cmd.category())) {
            return;
        }

        if(data.getChannelSpecificDisabledCategories().computeIfAbsent(event.getChannel().getId(), wew -> new ArrayList<>()).contains(cmd.category())) {
            return;
        }

        //If we are in the patreon bot, deny all requests from unknown guilds.
        if(conf.isPremiumBot() && !conf.isOwner(event.getAuthor()) && !dbg.isPremium()) {
            event.getChannel().sendMessage(
                    EmoteReference.ERROR + "Seems like you're trying to use the Patreon bot when this guild is **not** marked as premium. " +
                            "**If you think this is an error please contact Kodehawa#3457 or poke me on #donators in the support guild**").queue();
            return;
        }

        if(!cmd.permission().test(event.getMember())) {
            event.getChannel().sendMessage(EmoteReference.STOP + "You have no permissions to trigger this command").queue();
            return;
        }

        //EXECUTION!
        boolean success = false;
        try {
            cmd.run(event, cmdName, content);
            success = true;
        } catch(IndexOutOfBoundsException e) {
            event.getChannel().sendMessage(
                    EmoteReference.ERROR + "Your query returned no results or incorrect type arguments. Check the command help.")
                    .queue();
        } catch(PermissionException e) {
            event.getChannel().sendMessage(
                    EmoteReference.ERROR + "I don't have permission to do this :(! I need the permission: " + e
                            .getPermission() + (e.getMessage() != null ? " | Message: " + e.getMessage() : "")).queue();
        } catch(IllegalArgumentException e) { //NumberFormatException == IllegalArgumentException
            event.getChannel().sendMessage(
                    EmoteReference.ERROR + "Incorrect type arguments or message exceeds 2048 characters. Check command help.")
                    .queue();
            log.warn("Exception caught and alternate message sent. We should look into this, anyway.", e);
        } catch(ReqlError e) {
            event.getChannel().sendMessage(
                    EmoteReference.ERROR + "Sorry! I'm having some problems with my database... ").queue();
            SentryHelper.captureExceptionContext(
                    "Something seems to have broken in the db! Check this out!", e, this.getClass(), "Database");
        } catch(Exception e) {
            String id = Snow64.toSnow64(event.getMessage().getIdLong());

            event.getChannel().sendMessage(
                    EmoteReference.ERROR + "I ran into an unexpected error. (Error ID: ``" + id + "``)\n" +
                            "If you want, **contact ``Kodehawa#3457`` on DiscordBots** (popular bot guild), or join our **support guild** (Link on ``~>about``). Don't forget the Error ID!"
            ).queue();

            SentryHelper.captureException("Unexpected Exception on Command: " + event.getMessage()
                    .getRawContent() + " | (Error ID: ``" + id + "``)", e, this.getClass());
        }

        //LOGGING!
        commandTotal++;
        MantaroBot.getInstance().getStatsClient().increment("commands");
        LOGGER.trace(
                "Command invoked: {}, by {}#{} with timestamp {}", cmdName, event.getAuthor().getName(), event.getAuthor().getDiscriminator(),
                new Date(System.currentTimeMillis())
        );

        long end = System.currentTimeMillis();
        MantaroBot.getInstance().getStatsClient().histogram("command_query_time", end - start);
    }
}