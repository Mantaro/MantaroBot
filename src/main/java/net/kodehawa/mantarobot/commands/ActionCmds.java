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
import net.kodehawa.mantarobot.commands.action.ImageActionCmd;
import net.kodehawa.mantarobot.commands.action.ImageCmd;
import net.kodehawa.mantarobot.commands.action.TextActionCmd;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.awt.*;
import java.util.List;

@Module
@SuppressWarnings("unused")
public class ActionCmds {
    
    //Images.
    private final DataManager<List<String>> BLOODSUCK = new SimpleFileDataManager("assets/mantaro/texts/bloodsuck.txt");
    //Speech.
    private final DataManager<List<String>> GREETINGS = new SimpleFileDataManager("assets/mantaro/texts/greetings.txt");
    private final DataManager<List<String>> MEOW = new SimpleFileDataManager("assets/mantaro/texts/meow.txt");
    private final DataManager<List<String>> NUZZLE = new SimpleFileDataManager("assets/mantaro/texts/nuzzle.txt");
    private final DataManager<List<String>> TSUNDERE = new SimpleFileDataManager("assets/mantaro/texts/tsundere.txt");
    
    @Subscribe
    public void register(CommandRegistry cr) {
        //pat();
        cr.register("pat", new ImageActionCmd(
                "Pat", "Pats the specified user.", Color.PINK,
                "pat", EmoteReference.TALKING, "commands.action.pat", "pat", "commands.action.lonely.pat", "commands.action.self.pat"
        ));
        
        //hug();
        cr.register("hug", new ImageActionCmd(
                "Hug", "Hugs the specified user.", Color.PINK,
                "hug", EmoteReference.TALKING, "commands.action.hug", "hug", "commands.action.lonely.hug", "commands.action.self.hug"
        ));
        
        //kiss();
        cr.register("kiss", new ImageActionCmd(
                "Kiss", "Kisses the specified user.", Color.PINK,
                "kiss", EmoteReference.TALKING, "commands.action.kiss", "kiss", "commands.action.lonely.kiss", "commands.action.self.kiss"
        ));
        
        //poke();
        cr.register("poke", new ImageActionCmd(
                "Poke", "Pokes the specified user.", Color.PINK,
                "poke", EmoteReference.TALKING, "commands.action.poke", "poke", "commands.action.lonely.poke", "commands.action.self.poke"
        ));
        
        //slap();
        cr.register("slap", new ImageActionCmd(
                "Slap", "Slaps the specified user ;).", Color.PINK,
                "slap", EmoteReference.TALKING, "commands.action.slap", "slap", "commands.action.lonely.slap", "commands.action.self.slap"
        ));
        
        //bite();
        cr.register("bite", new ImageActionCmd(
                "Bite", "Bites the specified user.", Color.PINK,
                "bite", EmoteReference.TALKING, "commands.action.bite", "bite", "commands.action.lonely.bite", "commands.action.self.bite"
        ));
        
        //tickle();
        cr.register("tickle", new ImageActionCmd(
                "Tickle", "Tickles the specified user.", Color.PINK,
                "tickle", EmoteReference.JOY, "commands.action.tickle", "tickle", "commands.action.lonely.tickle", "commands.action.self.tickle"
        ));
        
        //highfive();
        cr.register("highfive", new ImageActionCmd(
                "Highfive", "Highfives with the specified user.", Color.PINK,
                "highfive", EmoteReference.TALKING, "commands.action.highfive", "highfive", "commands.action.lonely.highfive", "commands.action.self.highfive", true
        ));
        
        //pout();
        cr.register("pout", new ImageActionCmd(
                "Pout", "Pouts at the specified user.", Color.PINK,
                "pout", EmoteReference.TALKING, "commands.action.pout", "pout", "commands.action.lonely.pout", "commands.action.self.pout", true
        ));
        
        //lick();
        cr.register("lick", new ImageActionCmd(
                "lick", "Licks the specified user.", Color.PINK,
                "lick", EmoteReference.TALKING, "commands.action.lick", "lick", "commands.action.lonely.lick", "commands.action.self.lick"
        ));
        
        //teehee()
        cr.register("teehee", new ImageActionCmd("Teehee", "Teehee~", Color.PINK,
                "teehee", EmoteReference.EYES, "commands.action.teehee", "teehee", "commands.action.lonely.teehee", "commands.action.self.teehee", true));
        
        //smile()
        cr.register("smile", new ImageActionCmd("Smile", "Smiles at someone", Color.PINK,
                "smile", EmoteReference.TALKING, "commands.action.smile", "smile", "commands.action.lonely.smile", "commands.action.self.smile", true));
        
        //stare()
        cr.register("stare", new ImageActionCmd("Stare", "Stares at someone", Color.PINK,
                "stare", EmoteReference.EYES, "commands.action.stare", "stare", "commands.action.lonely.stare", "commands.action.self.stare", true));
        
        //holdhands()
        cr.register("holdhands", new ImageActionCmd("Hold Hands", "Hold someone's hands", Color.PINK,
                "holdhands", EmoteReference.HEART, "commands.action.holdhands", "handholding", "commands.action.lonely.holdhands", "commands.action.self.holdhands", true));
        
        //cuddle()
        cr.register("cuddle", new ImageActionCmd("Cuddle", "Cuddles someone", Color.PINK,
                "cuddle", EmoteReference.HEART, "commands.action.cuddle", "cuddle", "commands.action.lonely.cuddle", "commands.action.self.cuddle"));
        
        //greet();
        cr.register("greet", new TextActionCmd(
                "Greet", "Sends a random greeting", Color.DARK_GRAY,
                EmoteReference.TALKING + "%s", GREETINGS.get()
        ));
        
        //tsundere();
        cr.register("tsundere", new TextActionCmd(
                "Tsundere Command", "Y-You baka!", Color.PINK,
                EmoteReference.MEGA + "%s", TSUNDERE.get()
        ));
        
        //nuzzle()
        cr.register("nuzzle", new ImageActionCmd(
                "Nuzzle Command", "Nuzzles the specified user.", Color.PINK,
                "nuzzle", EmoteReference.TALKING, "commands.action.nuzzle", NUZZLE.get(), "commands.action.lonely.nuzzle", "commands.action.self.nuzzle", true
        ));
        
        //bloodsuck()
        cr.register("bloodsuck", new ImageActionCmd("Bloodsuck command", "Sucks the blood of an user", Color.PINK, "bloodsuck",
                EmoteReference.TALKING, "commands.action.bloodsuck", BLOODSUCK.get(), "commands.action.lonely.bloodsuck", "commands.action.self.bloodsuck", true));
        
        //lewd()
        cr.register("lewd", new ImageCmd("Lewd", "T-Too lewd!", "lewd", "lewd", "commands.action.lewd"));
        
        //meow()
        cr.register("meow", new ImageCmd("Meow", "Meows at the specified user.", "meow", MEOW.get(), "commands.action.meow"));
        cr.registerAlias("meow", "mew");
        
        //nom()
        cr.register("nom", new ImageCmd("Nom", "*nom nom*", "nom", "nom", "commands.action.nom"));
        
        //facedesk()
        cr.register("facedesk", new ImageCmd("Facedesk", "When it's just too much to handle.", "facedesk", "banghead",
                "commands.action.facedesk", true));
    }
}
