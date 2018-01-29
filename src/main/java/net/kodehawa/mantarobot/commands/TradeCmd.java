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
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static net.kodehawa.mantarobot.utils.StringUtils.advancedSplitArgs;

@Module
public class TradeCmd {

    private Map<Long, TradeSession> currentSessions = new ConcurrentHashMap<>();

    //TODO: comment this
    @Subscribe
    public void trade(CommandRegistry cr) {
        TreeCommand tradeCommand = (TreeCommand) cr.register("trade", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(final GuildMessageReceivedEvent event, I18nContext languageContext, final String content) {
                        if(content.length() < 2) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention the user to start the trade with, and the initial offer.").queue();
                            return;
                        }

                        if(event.getMessage().getMentionedUsers().isEmpty()) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention the user to start the trade with.").queue();
                            return;
                        }

                        //Expect: person to trade with, initial offer
                        final String[] args = advancedSplitArgs(content, 2);
                        boolean isMoneyOffer = false;
                        String offer = args[1];
                        //user id, trade amount
                        //This map is used to keep track of all the items and money offered on this trade session.
                        Map<Long, TradeItem> tradeItemMap = new HashMap<>();
                        User traderUser = event.getAuthor();
                        User tradedWithUser = event.getMessage().getMentionedUsers().get(0);
                        Player trader = MantaroData.db().getPlayer(traderUser);
                        Player tradedWith = MantaroData.db().getPlayer(tradedWithUser);

                        if(trader.isLocked() || tradedWith.isLocked()) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "One of the players is locked. Cannot continue...").queue();
                            return;
                        }

                        long offerLong = 0;

                        if(offer.matches("([0-9])+")) {
                            isMoneyOffer = true;
                            try {
                                offerLong = Long.parseLong(offer);
                            } catch (NumberFormatException e) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number of credits to offer.").queue();
                                return;
                            }
                        }

                        if(isMoneyOffer) {
                            if(trader.getMoney() < offerLong) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have enough credits to offer " + offerLong + " credits.").queue();
                                return;
                            }

                            tradeItemMap.put(traderUser.getIdLong(), new TradeItem(offerLong, new ArrayList<>()));
                        } else {
                            Item offered = Items.fromAnyNoId(offer).orElse(null);
                            if(offered == null) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a valid item to offer!").queue();
                                return;
                            }

                            if(offered.isHidden()) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot trade this item. (Hidden)").queue();
                                return;
                            }

                            if(!trader.getInventory().containsItem(offered)) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have this item...").queue();
                                return;
                            }

                            List<Item> toAdd = new ArrayList<>();
                            toAdd.add(offered);
                            tradeItemMap.put(traderUser.getIdLong(), new TradeItem(0, toAdd));
                        }

                        //Lock for 6 minutes.
                        trader.getData().setLockedUntil(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(6));
                        tradedWith.getData().setLockedUntil(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(6));
                        trader.save();
                        tradedWith.save();

                        event.getChannel().sendMessage(String.format("%sTrade session about to start between %s#%s and %s#%s\n%s%s, are you sure? Reply with yes within 30 seconds to continue, no to cancel.",
                                EmoteReference.MEGA, traderUser.getName(), traderUser.getDiscriminator(), tradedWithUser.getName(), tradedWithUser.getDiscriminator(),
                                EmoteReference.STOPWATCH, tradedWithUser.getName())).queue();
                        InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(),30, e -> {
                            if (e.getAuthor().getIdLong() != tradedWithUser.getIdLong())
                                return Operation.IGNORED;

                            String message = e.getMessage().getContentRaw();
                            if (message.equalsIgnoreCase("yes")) {
                                e.getChannel().sendMessage(String.format("%sTrade session started between %s#%s and %s#%s\n" +
                                        "%sInitial offer: %s", EmoteReference.MEGA, traderUser.getName(), traderUser.getDiscriminator(), tradedWithUser.getName(), tradedWithUser.getDiscriminator(), EmoteReference.CREDITCARD, tradeItemMap.entrySet().stream().map((s) -> s.getValue().amount + " credits and " + s.getValue().items.size() + " items."))).queue();
                                //TODO start new operation.
                                List<Long> users = new ArrayList<>();
                                users.add(tradedWithUser.getIdLong());
                                users.add(traderUser.getIdLong());
                                currentSessions.put(event.getChannel().getIdLong(), new TradeSession(users, tradeItemMap));

                                return Operation.COMPLETED;
                            } else if (message.equalsIgnoreCase("no")) {
                                trader.getData().setLockedUntil(System.currentTimeMillis());
                                tradedWith.getData().setLockedUntil(System.currentTimeMillis());
                                trader.save();
                                tradedWith.save();
                                e.getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled trade.").queue();
                                return Operation.COMPLETED;
                            }

                            //For everything else in between.
                            return Operation.IGNORED;
                        });
                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return null;
            }
        });

        tradeCommand.setPredicate(event -> {
            TradeSession currentSession = currentSessions.get(event.getChannel().getIdLong());
            if(currentSession == null) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "There isn't any trading session active on this channel").queue();
                return false;
            }

            if(currentSession.getUsers().stream().noneMatch(p -> p.equals(event.getAuthor().getIdLong()))) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "You aren't a member of this trading session!").queue();
                return false;
            }

            return true;
        });

        tradeCommand.addSubCommand("additem", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TradeSession currentSession = currentSessions.get(event.getChannel().getIdLong());
                Item item = Items.fromAnyNoId(content).orElse(null);
                if(item == null) {
                    //insert message here
                    return;
                }

                User tradedWithUser = currentSession.getUsers().stream()
                        .filter(u -> !u.equals(event.getAuthor().getIdLong()))
                        .map(id -> MantaroBot.getInstance().getUserById(id))
                        .findFirst()
                        .orElse(null);

                if(tradedWithUser == null) {
                    //stop the operation and unlock players
                    return;
                }

                Player trader = MantaroData.db().getPlayer(event.getAuthor());
                Player tradedWith = MantaroData.db().getPlayer(tradedWithUser);
            }
        });
    }

    @AllArgsConstructor
    @Data
    private class TradeItem {
        private long amount;
        private List<Item> items;
    }

    @AllArgsConstructor
    @Data
    private class TradeSession {
        private List<Long> users;
        private Map<Long, TradeItem> tradeItemMap;
    }
}
