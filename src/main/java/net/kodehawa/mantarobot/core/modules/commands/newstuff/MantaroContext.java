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

package net.kodehawa.mantarobot.core.modules.commands.newstuff;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import sox.Sox;
import sox.command.AbstractContext;
import sox.command.argument.Arguments;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class MantaroContext extends AbstractContext<MantaroContext> {
    private final Message message;
    private final I18nContext context;
    
    protected MantaroContext(@Nonnull Sox sox, @Nonnull Arguments arguments,
                             @Nonnull Message message) {
        super(sox, arguments, new HashMap<>());
        this.message = message;
        ManagedDatabase managedDatabase = MantaroData.db();
        this.context = new I18nContext(
                managedDatabase.getGuild(message.getGuild()).getData(),
                managedDatabase.getUser(message.getAuthor()).getData()
        );
    }
    
    @Nonnull
    @CheckReturnValue
    public Message message() {
        return message;
    }
    
    @Nonnull
    @CheckReturnValue
    public MessageChannel channel() {
        return message.getChannel();
    }
    
    @Nonnull
    @CheckReturnValue
    public User author() {
        return message.getAuthor();
    }
    
    @CheckReturnValue
    public Member member() {
        return Objects.requireNonNull(message.getMember(), "This method cannot be used in DMs");
    }
    
    @CheckReturnValue
    public Guild guild() {
        return Objects.requireNonNull(message.getGuild(), "This method cannot be used in DMs");
    }
    
    @CheckReturnValue
    public TextChannel textChannel() {
        return Objects.requireNonNull(message.getTextChannel(), "This method cannot be used in DMs");
    }
    
    @Nonnull
    @CheckReturnValue
    public JDA jda() {
        return message.getJDA();
    }
    
    @Nonnull
    @CheckReturnValue
    public I18nContext languageContext() {
        return context;
    }
    
    @Nonnull
    public CompletionStage<Message> send(@Nonnull String content) {
        return channel().sendMessage(content).submit();
    }
    
    @Nonnull
    public CompletionStage<Message> send(@Nonnull EmbedBuilder embed) {
        return send(embed.build());
    }
    
    @Nonnull
    public CompletionStage<Message> send(@Nonnull MessageEmbed embed) {
        return channel().sendMessage(embed).submit();
    }
    
    @Nonnull
    public CompletionStage<Message> send(@Nonnull String content, @Nonnull EmbedBuilder embed) {
        return send(content, embed.build());
    }
    
    @Nonnull
    public CompletionStage<Message> send(@Nonnull String content, @Nonnull MessageEmbed embed) {
        return send(new MessageBuilder().setContent(content).setEmbed(embed));
    }
    
    @Nonnull
    public CompletionStage<Message> send(@Nonnull MessageBuilder message) {
        return send(message.build());
    }
    
    @Nonnull
    public CompletionStage<Message> send(@Nonnull Message message) {
        return channel().sendMessage(message).submit();
    }
    
    @Nonnull
    @Override
    public MantaroContext snapshot() {
        MantaroContext context = new MantaroContext(sox, arguments.snapshot(), message);
        if(serviceManager != null) {
            context.serviceManager = serviceManager.snapshot();
        }
        context.customProperties.putAll(customProperties);
        return context;
    }
}
