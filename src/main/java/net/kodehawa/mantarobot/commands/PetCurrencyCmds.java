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

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.pets.Pet;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Map;

@Module
public class PetCurrencyCmds {
    //@Subscribe
    public void petInventory(CommandRegistry cr) {
        cr.register("petinventory", new SimpleCommand(Category.PETS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                ManagedDatabase db = MantaroData.db();
                Player player = db.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();
                
                String petName = content.trim();
                Map<String, Pet> profilePets = playerData.getPets();
                if(!profilePets.containsKey(petName)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.petactions.pet.no_pet"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Pet pet = profilePets.get(petName);
                Inventory petInventory = pet.getPetInventory();
                
                event.getChannel().sendMessageFormat("%1$sCurrent pet inventory: %2$s", EmoteReference.POPPER, ItemStack.toString(petInventory.asList())).queue();
            }
        });
    }
    
    //@Subscribe
    public void petMarket(CommandRegistry cr) {
        TreeCommand petMarketCmd = (TreeCommand) cr.register("petmarket", new TreeCommand(Category.PETS) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        //do i just copy market cmd here or make a new interface?
                    }
                };
            }
        });
    }
}
