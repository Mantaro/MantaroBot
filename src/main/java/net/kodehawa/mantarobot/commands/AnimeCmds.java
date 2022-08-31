/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.kodehawa.mantarobot.commands.anime.AnimeData;
import net.kodehawa.mantarobot.commands.anime.CharacterData;
import net.kodehawa.mantarobot.commands.anime.KitsuRetriever;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.text.StringEscapeUtils;

import java.awt.*;
import java.net.SocketTimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Module
public class AnimeCmds {
    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Anime.class);
        cr.registerSlash(Character.class);
    }

    @Name("anime")
    @Defer
    @Description("Get anime information from Kitsu.")
    @Category(CommandCategory.FUN)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "name", description = "The name of the anime/manga to look for.", required = true)
    })
    @Help(description = "Get anime information from Kitsu.", usage = "`/anime name:[name]` - Look up the information for the specified anime", parameters = {
            @Help.Parameter(name = "name", description = "The name of the anime/manga to look for.")
    })
    public static class Anime extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            try {
                var name = ctx.getOptionAsString("name");
                if (name.isEmpty()) {
                    ctx.reply("commands.anime.no_args", EmoteReference.ERROR);
                    return;
                }

                var found = KitsuRetriever.searchAnime(name);

                if (found.isEmpty()) {
                    ctx.reply("commands.anime.no_results", EmoteReference.ERROR);
                    return;
                }

                var languageContext = ctx.getLanguageContext();
                if (found.size() == 1) {
                    animeData(ctx, languageContext, found.get(0));
                    return;
                }

                Function<AnimeData, String> format = anime -> {
                    if (anime.getAttributes().getTitles().getJa_jp() != null) {
                        return "%s **[%s](%s)** (%s)".formatted(
                                EmoteReference.BLUE_SMALL_MARKER,
                                anime.getAttributes().getCanonicalTitle(), anime.getURL(),
                                anime.getAttributes().getTitles().getJa_jp());
                    } else {
                        return "%s **[%s](%s)**".formatted(
                                EmoteReference.BLUE_SMALL_MARKER,
                                anime.getAttributes().getCanonicalTitle(), anime.getURL());
                    }
                };

                DiscordUtils.selectListButtonSlash(ctx, found.stream().limit(5).collect(Collectors.toList()), format,
                        s -> baseEmbed(ctx, languageContext.get("commands.anime.selection_start"))
                                .setDescription(s)
                                .setColor(Color.PINK)
                                .setThumbnail("https://i.imgur.com/VwlGqdk.png")
                                .setFooter(languageContext.get("commands.anime.information_footer"), ctx.getAuthor().getAvatarUrl())
                                .build(),
                        (anime, hook) -> animeData(ctx, languageContext, anime));
            } catch (JsonProcessingException jex) {
                jex.printStackTrace();
                ctx.reply("commands.anime.no_results", EmoteReference.ERROR);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                ctx.reply("commands.anime.malformed_result", EmoteReference.ERROR);
            } catch (SocketTimeoutException timeout) {
                ctx.reply("commands.anime.timeout", EmoteReference.ERROR);
            } catch (Exception ex) {
                ex.printStackTrace();
                ctx.reply("commands.anime.error", EmoteReference.ERROR, ex.getClass().getSimpleName());
            }
        }
    }

    @Name("character")
    @Defer
    @Description("Get character information from Kitsu.")
    @Category(CommandCategory.FUN)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "name", description = "The name of the character to look for.", required = true)
    })
    @Help(description = "Get character information from Kitsu.", usage = "`/character name:[name]` - Look up the information for the specified character", parameters = {
            @Help.Parameter(name = "name", description = "The name of the character to look for.")
    })
    public static class Character extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            try {
                var name = ctx.getOptionAsString("name");
                if (name.isEmpty()) {
                    ctx.reply("commands.character.no_args", EmoteReference.ERROR);
                    return;
                }

                var characters = KitsuRetriever.searchCharacters(name);
                if (characters.isEmpty()) {
                    ctx.reply("commands.anime.no_results", EmoteReference.ERROR);
                    return;
                }

                var languageContext = ctx.getLanguageContext();
                if (characters.size() == 1) {
                    characterData(ctx, languageContext, characters.get(0));
                    return;
                }

                Function<CharacterData, String> format = character -> {
                    if (character.getAttributes().getNames().getJa_jp() == null) {
                        return "%s **[%s](%s)**".formatted(
                                EmoteReference.BLUE_SMALL_MARKER,
                                character.getAttributes().getName(), character.getURL());
                    } else {
                        return "%s **[%s](%s)** (%s)".formatted(
                                EmoteReference.BLUE_SMALL_MARKER,
                                character.getAttributes().getName(), character.getURL(),
                                character.getAttributes().getNames().getJa_jp());
                    }
                };

                DiscordUtils.selectListButtonSlash(ctx, characters.stream().limit(5).collect(Collectors.toList()), format,
                        s -> baseEmbed(ctx, languageContext.get("commands.anime.information_footer"))
                                .setDescription(s)
                                .setColor(Color.PINK)
                                .setThumbnail("https://i.imgur.com/VwlGqdk.png")
                                .setFooter(languageContext.get("commands.anime.information_footer"), ctx.getAuthor().getAvatarUrl())
                                .build(),
                        (character, hook)  -> characterData(ctx, languageContext, character));
            } catch (JsonProcessingException jex) {
                jex.printStackTrace();
                ctx.reply("commands.anime.no_results", EmoteReference.ERROR);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                ctx.reply("commands.anime.malformed_result", EmoteReference.ERROR);
            } catch (SocketTimeoutException timeout) {
                ctx.reply("commands.anime.timeout", EmoteReference.ERROR);
            }catch (Exception ex) {
                ctx.reply("commands.anime.error", EmoteReference.ERROR, ex.getClass().getSimpleName());
            }
        }
    }

    private static void animeData(SlashContext ctx, I18nContext lang, AnimeData animeData) {
        try {
            final var attributes = animeData.getAttributes();
            final var title = attributes.getCanonicalTitle();
            final var releaseDate = attributes.getStartDate();
            final var endDate = attributes.getEndDate();
            final var animeDescription = attributes.getSynopsis() == null ? "" :
                    StringEscapeUtils.unescapeHtml4(attributes.getSynopsis().replace("<br>", " "));

            final var favoriteCount = String.valueOf(attributes.getFavoritesCount());
            final var imageUrl = attributes.getPosterImage().getMedium();
            final var typeName = attributes.getShowType();
            final var animeType = typeName.length() > 3 ? Utils.capitalize(typeName.toLowerCase()) : typeName;
            final var episodes = String.valueOf(attributes.getEpisodeCount());
            final var episodeDuration = String.valueOf(attributes.getEpisodeLength());

            if (attributes.isNsfw() && !ctx.isChannelNSFW()) {
                ctx.reply(lang.get("commands.anime.hentai"), EmoteReference.ERROR);
                return;
            }

            final var player = MantaroData.db().getPlayer(ctx.getAuthor());
            final var badge = APIUtils.getHushBadge(title, Utils.HushType.ANIME);
            if (badge != null && player.getData().addBadgeIfAbsent(badge)) {
                player.saveUpdating();
            }

            //Start building the embedded message.
            var embed = new EmbedBuilder();
            embed.setColor(Color.PINK)
                    .setDescription(StringUtils.limit(animeDescription, 1400))
                    .setAuthor(lang.get("commands.anime.information_header").formatted(title), animeData.getURL(), imageUrl)
                    .setFooter(lang.get("commands.anime.information_notice"), ctx.getAuthor().getEffectiveAvatarUrl())
                    .setThumbnail(imageUrl)
                    .addField(EmoteReference.CALENDAR.toHeaderString() + lang.get("commands.anime.release_date"),
                            releaseDate, true
                    )
                    .addField(EmoteReference.CALENDAR2.toHeaderString() + lang.get("commands.anime.end_date"),
                            (endDate == null || endDate.equals("null") ? lang.get("commands.anime.airing") : endDate),
                            true
                    )
                    .addField(EmoteReference.STAR.toHeaderString() + lang.get("commands.anime.favorite_count"), favoriteCount, true)
                    .addField(EmoteReference.DEV.toHeaderString() + lang.get("commands.anime.type"), animeType, true)
                    .addField(EmoteReference.SATELLITE.toHeaderString() + lang.get("commands.anime.episodes"), episodes, true)
                    .addField(EmoteReference.CLOCK.toHeaderString() + lang.get("commands.anime.episode_duration"),
                            episodeDuration + " " + lang.get("commands.anime.minutes"), true
                    );

            ctx.editAction(embed.build())
                    .setComponents(ActionRow.of(Button.link(animeData.getURL(), lang.get("commands.anime.external_link_text"))))
                    .queue(success -> {}, Throwable::printStackTrace);
        } catch (Exception e) {
            ctx.edit("commands.anime.error", EmoteReference.WARNING, e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    private static void characterData(SlashContext ctx, I18nContext lang, CharacterData character) {
        final var attributes = character.getAttributes();

        final var japName = attributes.getNames().getJa_jp();
        final var charName = attributes.getName();
        final var imageUrl = attributes.getImage().getOriginal();

        final var characterDescription = StringEscapeUtils.unescapeHtml4(
                attributes.getDescription().replace("<br>", "\n")
                        .replaceAll("<.*?>", "")
        ); // This is silly.

        var charDescription = "";
        if (attributes.getDescription() == null || attributes.getDescription().isEmpty()) {
            charDescription = lang.get("commands.character.no_info");
        } else {
            charDescription = StringUtils.limit(characterDescription, 1016);
        }

        var player = MantaroData.db().getPlayer(ctx.getAuthor());
        var badge = APIUtils.getHushBadge(charName.replace(japName, "").trim(), Utils.HushType.CHARACTER);

        if (badge != null && player.getData().addBadgeIfAbsent(badge)) {
            player.saveUpdating();
        }

        var embed = new EmbedBuilder();
        embed.setColor(Color.LIGHT_GRAY)
                .setThumbnail(imageUrl)
                .setAuthor(
                        lang.get("commands.character.information_header").formatted(charName),
                        character.getURL(), imageUrl
                )
                .setDescription(StringUtils.limit(charDescription, 1400))
                .setFooter(lang.get("commands.anime.information_notice"), ctx.getAuthor().getEffectiveAvatarUrl());

        ctx.edit(embed.build());
    }
}
