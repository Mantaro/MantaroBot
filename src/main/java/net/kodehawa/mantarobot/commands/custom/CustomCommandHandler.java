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
        void handle(GuildMessageReceivedEvent event, String value, String args);
    }

    private static final Map<String, Func> specialHandlers = new LinkedHashMap<>();

    static {
        //FIXME on NEXT KAIPER UPDATE:
        //GlobalVisitorSettings will be replaced by VisitorSettings instance

        GlobalVisitorSettings.ITERATION_LIMIT = 200;
        GlobalVisitorSettings.SIZE_LIMIT = 100;
        GlobalVisitorSettings.MILLISECONDS_LIMIT = 300;
        GlobalVisitorSettings.RECURSION_DEPTH_LIMIT = 100;

        // Special handlers
        specialHandlers.put("k", (event, value, args) -> {
            //FIXME on NEXT KAIPER UPDATE:
            //LimitReachedException will be replaced by VisitorException

            try {
                SafeEmbed[] embed = new SafeEmbed[1];
                String result = new KaiperScriptExecutor("<$" + value + "$>")
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

                MessageBuilder message = new MessageBuilder().append(result);

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

        specialHandlers.put("play", (event, value, args) -> {
            try {
                new URL(value);
            } catch (Exception ignored) {
                value = "ytsearch: " + value;
            }

            MantaroBot.getInstance()
                .getAudioManager()
                .loadAndPlay(event, value, false, false);
        });

        specialHandlers.put("embed", (event, value, args) -> {
            try {
                EmbedJSON embed = GsonDataManager.gson(false)
                    .fromJson('{' + value + '}', EmbedJSON.class);
                event.getChannel().sendMessage(embed.gen(event.getMember())).queue();
            } catch (IllegalArgumentException invalid) {
                if (invalid.getMessage().contains("URL must be a valid http or https url")) {
                    event.getChannel().sendMessage(EmoteReference.ERROR2 + "This command contains an invalid image, please fix...").queue();
                } else {
                    event.getChannel().sendMessage(
                        EmoteReference.ERROR2 + "The string ``{" + value + "}`` isn't valid, or the output is longer than 2000 characters."
                    ).queue();
                }
            } catch (Exception ignored) {
                event.getChannel().sendMessage(EmoteReference.ERROR2 + "The string ``{" + value + "}`` isn't a valid JSON.").queue();
            }
        });

        specialHandlers.put("img", (event, value, args) -> {
            try {
                if (!EmbedBuilder.URL_PATTERN.asPredicate().test(value)) {
                    event.getChannel().sendMessage(
                        EmoteReference.ERROR2 + "The string ``" + value + "`` isn't a valid link."
                    ).queue();
                    return;
                }

                event.getChannel().sendMessage(new EmbedBuilder().setImage(value).setColor(event.getMember().getColor()).build()).queue();

            } catch (IllegalArgumentException invalid) {
                event.getChannel().sendMessage(EmoteReference.ERROR2 + "This command contains an invalid image, please fix...").queue();
            }
        });

        specialHandlers.put("image", specialHandlers.get("img"));
        specialHandlers.put("imgembed", specialHandlers.get("img"));

        specialHandlers.put("iam", (event, value, args) -> {
            MiscCmds.iamFunction(value, event);
        });

        specialHandlers.put("iamnot", (event, value, args) -> {
            MiscCmds.iamnotFunction(value, event);
        });
    }

    private final String args;
    private final GuildMessageReceivedEvent event;
    private String response;

    public CustomCommandHandler(GuildMessageReceivedEvent event, String response) {
        this(event, response, "");
    }

    public CustomCommandHandler(GuildMessageReceivedEvent event, String response, String args) {
        this.event = event;
        this.response = response;
        this.args = args;
    }

    public void handle() {
        if (!processResponse()) return;
        if (specialHandling()) return;
        event.getChannel().sendMessage(response).queue();
    }

    private boolean processResponse() {
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
                    );
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

        func.handle(event, value, args);

        return true;
    }
}
