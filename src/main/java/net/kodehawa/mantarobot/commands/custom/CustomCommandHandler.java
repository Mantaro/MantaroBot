package net.kodehawa.mantarobot.commands.custom;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.MiscCmds;
import net.kodehawa.mantarobot.commands.custom.kaiperscript.parser.InterpreterEvaluator;
import net.kodehawa.mantarobot.commands.custom.kaiperscript.parser.KaiperScriptExecutor;
import net.kodehawa.mantarobot.commands.custom.kaiperscript.parser.internal.LimitReachedException;
import net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper.SafeEmbed;
import net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper.SafeGuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.custom.legacy.ConditionalCustoms;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import xyz.avarel.kaiper.interpreter.GlobalVisitorSettings;
import xyz.avarel.kaiper.runtime.Obj;
import xyz.avarel.kaiper.runtime.Str;
import xyz.avarel.kaiper.runtime.functions.NativeFunc;
import xyz.avarel.kaiper.runtime.java.JavaObject;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomCommandHandler {
    private interface Func {
        void handle(GuildMessageReceivedEvent event, I18nContext lang, String value, String args);
    }

    private static final Map<String, Func> specialHandlers = new LinkedHashMap<>();

    static {
        //FIXME on NEXT KAIPER UPDATE:
        //GlobalVisitorSettings will be replaced by VisitorSettings instance

        GlobalVisitorSettings.ITERATION_LIMIT = 200;
        GlobalVisitorSettings.SIZE_LIMIT = 100;
        GlobalVisitorSettings.MILLISECONDS_LIMIT = 200;
        GlobalVisitorSettings.RECURSION_DEPTH_LIMIT = 100;

        // Special handlers
        specialHandlers.put("k", (event, lang, value, args) -> {
            //FIXME on NEXT KAIPER UPDATE:
            //LimitReachedException will be replaced by VisitorException

            try {
                String code = value.trim();
                if (code.isEmpty()) return;

                if (!code.startsWith("<$k")) code = "<$k " + code;

                SafeEmbed[] embed = new SafeEmbed[1];
                String result = new KaiperScriptExecutor(code)
                    .execute(
                        new InterpreterEvaluator()
                            .declare("event", new JavaObject(new SafeGuildMessageReceivedEvent(event)))
                            .declare("embed", new NativeFunc("embed") {
                                JavaObject wrap;

                                @Override
                                protected synchronized Obj eval(List<Obj> arguments) {
                                    if (wrap == null) {
                                        embed[0] = new SafeEmbed();
                                        wrap = new JavaObject(embed[0]);
                                    }
                                        return wrap;
                                }
                            })
                            .declare("args", Str.of(args))
                    )
                    .trim();

                MessageBuilder message = new MessageBuilder().append(result.replace("@everyone", "\u200Deveryone").replace("@here", "\u200Dhere"));

                if (embed[0] != null) {
                    EmbedBuilder builder = SafeEmbed.builder(embed[0]);

                    if (!message.isEmpty()) {
                        message.setEmbed(builder.build());
                    }
                }

                if (!message.isEmpty()) {
                    event.getChannel().sendMessage(message.build()).queue();
                }
            } catch (LimitReachedException e) {
                event.getChannel().sendMessage("**Error**: " + e.getMessage()).queue();
            }
        });

        specialHandlers.put("text", (event, lang, value, args) -> event.getChannel().sendMessage(value).queue());

        specialHandlers.put("play", (event, lang, value, args) -> {
            try {
                new URL(value);
            } catch (Exception ignored) {
                value = "ytsearch: " + value;
            }

            MantaroBot.getInstance()
                .getAudioManager()
                .loadAndPlay(event, value, false, false, lang);
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

    public void handle() {
        if (!processResponse()) return;
        if (specialHandling()) return;
        event.getChannel().sendMessage(response).queue();
    }

    private boolean processResponse() {
        if (response.startsWith("k:") || response.startsWith("text:")) {
            return true;
        }

        if (response.contains("<$")) {
            //FIXME on NEXT KAIPER UPDATE:
            //LimitReachedException will be replaced by VisitorException

            try {
                response = new KaiperScriptExecutor(response)
                    .mapTextTokens(this::processText)
                    .execute(
                        new InterpreterEvaluator()
                            .declare("event", new JavaObject(new SafeGuildMessageReceivedEvent(event)))
                            .declare("args", Str.of(args))
                    ).replace("@everyone", "\u200Deveryone").replace("@here", "\u200Dhere");
            } catch (LimitReachedException e) {
                event.getChannel().sendMessage("**Error**: " + e.getMessage()).queue();
                return false;
            }
        } else {
            response = processText(response);
        }

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
        if (func == null) return false;

        func.handle(event, langContext, value, args);

        return true;
    }
}
