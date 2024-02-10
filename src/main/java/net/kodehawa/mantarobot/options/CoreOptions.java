package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static net.kodehawa.mantarobot.utils.Utils.mapConfigObjects;

@Option
public class CoreOptions extends OptionHandler {
    public CoreOptions() {
        setType(OptionType.GENERAL);
    }

    @Override
    public String description() {
        return "Core options.";
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent event) {
        registerOption("check:data", "Data check.",
                "Checks the data values you have set on this server. **THIS IS NOT USER-FRIENDLY**. " +
                        "If you wanna send this to the support server, use -print at the end.", "Checks the data values you have set on this server.",
                (ctx, args) -> {
                    var dbGuild = ctx.getDBGuild();
                    var lang = ctx.getLanguageContext();

                    // Map as follows: name, value
                    // This filters out unused configs.
                    var fieldMap = mapConfigObjects(dbGuild);
                    if (fieldMap == null) {
                        ctx.sendLocalized("options.check_data.retrieve_failure", EmoteReference.ERROR);
                        return;
                    }

                    final var guild = ctx.getGuild();
                    var opts = StringUtils.parseArguments(args);
                    if (opts.containsKey("print") || opts.containsKey("paste")) {
                        var builder = new StringBuilder();
                        for (var entry : fieldMap.entrySet()) {
                            builder.append("* ").append(entry.getKey()).append(": ").append(entry.getValue().right()).append("\n");
                        }

                        ctx.sendFormat("Send this: %s", Utils.paste(builder.toString()));
                        return;
                    }

                    var embedBuilder = new EmbedBuilder();
                    embedBuilder.setAuthor("Option Debug", null, ctx.getAuthor().getEffectiveAvatarUrl())
                            .setDescription(
                                    String.format(lang.get("options.check_data.header") + lang.get("options.check_data.terminology"),
                                            guild.getName())
                            )
                            .setThumbnail(guild.getIconUrl())
                            .setFooter(lang.get("options.check_data.footer"), null);

                    List<MessageEmbed.Field> fields = new LinkedList<>();
                    for (var e : fieldMap.entrySet()) {
                        fields.add(new MessageEmbed.Field(EmoteReference.BLUE_SMALL_MARKER + e.getKey() + ":\n" + e.getValue().left(),
                                e.getValue().right() == null ? lang.get("options.check_data.null_set") : String.valueOf(e.getValue().right()),
                                false)
                        );
                    }

                    var splitFields = DiscordUtils.divideFields(6, fields);
                    DiscordUtils.listButtons(ctx.getUtilsContext(), 200, embedBuilder, splitFields);                }
        );

        registerOption("reset:all", "Options reset.", "Resets all options on this server.", ctx -> {
            //Temporary stuff.
            var dbGuild = ctx.getDBGuild();
            // New object?
            var temp = ctx.getDBGuild();

            //The persistent data we wish to maintain.
            var premiumKey = temp.getPremiumKey();
            var gameTimeoutExpectedAt = temp.getGameTimeoutExpectedAt();
            var cases = temp.getCases();
            var ranPolls = temp.getRanPolls();
            var allowedBirthdays = temp.getAllowedBirthdays();
            var notified = temp.isNotifiedFromBirthdayChange();
            var greetReceived = temp.hasReceivedGreet();

            //Assign everything all over again
            var newDbGuild = MongoGuild.of(dbGuild.getId());
            newDbGuild.premiumUntil(dbGuild.getPremiumUntil());
            newDbGuild.gameTimeoutExpectedAt(gameTimeoutExpectedAt);
            newDbGuild.ranPolls(ranPolls);
            newDbGuild.cases(cases);
            newDbGuild.premiumKey(premiumKey);
            newDbGuild.getAllowedBirthdays().addAll(allowedBirthdays);
            newDbGuild.notifiedFromBirthdayChange(notified);
            newDbGuild.receivedGreet(greetReceived);

            newDbGuild.insertOrReplace();

            ctx.sendLocalized("options.reset_all.success", EmoteReference.CORRECT);
        });
    }
}
