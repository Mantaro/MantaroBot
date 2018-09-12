package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.MiscCmds;
import net.kodehawa.mantarobot.commands.custom.legacy.ConditionalCustoms;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CustomCommandHandler {
    private interface Func {
        void handle(GuildMessageReceivedEvent event, I18nContext lang, String value, String args);
    }

    private static final Map<String, Func> specialHandlers = new LinkedHashMap<>();
    //there's no way in hell this would work but ok
    //actually p sure this is just to make me feel safer and serves no purpose whatsoever.
    private static final Pattern filtered1 = Pattern.compile("([a-zA-Z0-9]{24}\\.[a-zA-Z0-9]{6}\\.[a-zA-Z0-9_\\-])\\w+");

    static {
        specialHandlers.put("text", (event, lang, value, args) -> event.getChannel().sendMessage(value).queue());

        specialHandlers.put("play", (event, lang, value, args) -> {
            GuildData data = MantaroData.db().getGuild(event.getGuild()).getData();
            if (data.getDisabledCommands().contains("play")) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "The play command is disabled on this server. Cannot run this custom command.").queue();
                return;
            }

            List<String> channelDisabledCommands = data.getChannelSpecificDisabledCommands().get(event.getChannel().getId());
            if (channelDisabledCommands != null && channelDisabledCommands.contains("play")) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "The play command is disabled on this channel. Cannot run this custom command.").queue();
                return;
            }

            try {
                new URL(value);
            } catch (Exception ignored) {
                value = "ytsearch: " + value;
            }

            MantaroBot.getInstance().getAudioManager().loadAndPlay(event, value, false, false, lang);
        });

        specialHandlers.put("embed", (event, lang, value, args) -> {
            try {
                EmbedJSON embed = GsonDataManager.gson(false)
                    .fromJson('{' + value + '}', EmbedJSON.class);
                event.getChannel().sendMessage(embed.gen(event.getMember())).queue();
            } catch (IllegalArgumentException invalid) {
                if (invalid.getMessage().contains("URL must be a valid http or https url")) {
                    event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_image"), EmoteReference.ERROR2).queue();
                } else {
                    event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_string"), EmoteReference.ERROR2, value).queue();
                }
            } catch (Exception ignored) {
                event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_json"), EmoteReference.ERROR2, value).queue();
            }
        });

        specialHandlers.put("img", (event, lang, value, args) -> {
            try {
                if (!EmbedBuilder.URL_PATTERN.asPredicate().test(value)) {
                    event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_link"), EmoteReference.ERROR2, value).queue();
                    return;
                }

                event.getChannel().sendMessage(new EmbedBuilder().setImage(value).setColor(event.getMember().getColor()).build()).queue();

            } catch (IllegalArgumentException invalid) {
                event.getChannel().sendMessageFormat(lang.get("commands.custom.invalid_image"), EmoteReference.ERROR2).queue();
            }
        });

        specialHandlers.put("image", specialHandlers.get("img"));
        specialHandlers.put("imgembed", specialHandlers.get("img"));
        specialHandlers.put("iam", (event, lang, value, args) -> MiscCmds.iamFunction(value, event, lang));
        specialHandlers.put("iamnot", (event, lang, value, args) -> MiscCmds.iamnotFunction(value, event, lang));
    }

    private final String args;
    private final GuildMessageReceivedEvent event;
    private final I18nContext langContext;
    private String response;

    public CustomCommandHandler(GuildMessageReceivedEvent event, I18nContext lang, String response) {
        this(event, lang, response, "");
    }

    public CustomCommandHandler(GuildMessageReceivedEvent event, I18nContext lang, String response, String args) {
        this.event = event;
        this.response = response;
        this.langContext = lang;
        this.args = args;
    }

    public void handle(boolean preview) {
        if (!processResponse())
            return;
        if (specialHandling())
            return;

        MessageBuilder builder = new MessageBuilder().setContent(filtered1.matcher(response).replaceAll("-filtered regex-"));
        if(preview) {
            builder.append("\n\n")
                    .append(EmoteReference.WARNING)
                    .append("**This is a preview of how a CC with this content would look like, ALL MENTIONS ARE DISABLED ON THIS MODE.**\n")
                    .append("`Command Preview Requested By: ")
                    .append(event.getAuthor().getName())
                    .append("#")
                    .append(event.getAuthor().getDiscriminator())
                    .append("`")
                    .stripMentions(event.getJDA(), Message.MentionType.ROLE, Message.MentionType.USER);
        }

        builder.stripMentions(event.getJDA(), Message.MentionType.HERE, Message.MentionType.EVERYONE).sendTo(event.getChannel()).queue();
    }

    public void handle() {
        this.handle(false);
    }

    private boolean processResponse() {
        if (response.startsWith("text:")) {
            return true;
        }

        response = processText(response);

        return true;
    }

    private String processText(String text) {
        if (text.contains("$(")) {
            text = new DynamicModifiers()
                .mapEvent("event", event)
                .resolve(text);
        }

        return ConditionalCustoms.resolve(text, 0);
    }

    private boolean specialHandling() {
        int c = response.indexOf(':');
        if (c == -1) return false;

        String prefix = response.substring(0, c);
        String value = response.substring(c + 1);

        Func func = specialHandlers.get(prefix);
        if (func == null)
            return false;

        func.handle(event, langContext, value, args);

        return true;
    }
}
