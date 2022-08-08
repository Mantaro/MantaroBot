/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.action.*;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.util.List;

@Module
public class ActionCmds {
    private static final DataManager<List<String>> BLOODSUCK = new SimpleFileDataManager("assets/mantaro/texts/bloodsuck.txt");
    private static final DataManager<List<String>> MEOW = new SimpleFileDataManager("assets/mantaro/texts/meow.txt");
    private static final DataManager<List<String>> NUZZLE = new SimpleFileDataManager("assets/mantaro/texts/nuzzle.txt");
    private static final DataManager<List<String>> TSUNDERE = new SimpleFileDataManager("assets/mantaro/texts/tsundere.txt");

    @Name("action")
    @Description("A bunch of action commands that didn't fit into a separate command")
    @Category(CommandCategory.ACTION)
    public static class Action extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) { }

        @Name("holdhands")
        @Description("Holds the hand of a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to hold hands with.", required = true))
        public static class HoldHands extends ImageActionSlash {
            public HoldHands() {
                super("Hold Hands", "Hold someone's hands", EmoteReference.HEART,
                        "commands.action.holdhands", "handholding", "commands.action.lonely.holdhands", "commands.action.self.holdhands", true);
            }
        }

        @Name("stare")
        @Description("Stares at a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to stare at.", required = true))
        public static class Stare extends ImageActionSlash {
            public Stare() {
                super("Stare", "Stares at someone", EmoteReference.EYES,
                        "commands.action.stare", "stare", "commands.action.lonely.stare", "commands.action.self.stare", true);
            }
        }

        @Name("blush")
        @Description("Blushes at a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to blush at.", required = true))
        public static class Blush extends ImageActionSlash {
            public Blush() {
                super("Blush", "Blushes at someone", EmoteReference.HEART,
                        "commands.action.blush", "blush", "commands.action.lonely.blush", "commands.action.self.blush", true);
            }
        }

        @Name("nuzzle")
        @Description("Nuzzles a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to nuzzle.", required = true))
        public static class Nuzzle extends ImageActionSlash {
            public Nuzzle() {
                super("Nuzzle Command", "Nuzzles the specified user.", EmoteReference.TALKING,
                        "commands.action.nuzzle", NUZZLE.get(), "commands.action.lonely.nuzzle", "commands.action.self.nuzzle", true);
            }
        }

        @Name("bloodsuck")
        @Description("Sucks the blood of a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to suck the blood of.", required = true))
        public static class BloodSuck extends ImageActionSlash {
            public BloodSuck() {
                super("Bloodsuck command", "Sucks the blood of a user", EmoteReference.TALKING,
                        "commands.action.bloodsuck", BLOODSUCK.get(), "commands.action.lonely.bloodsuck", "commands.action.self.bloodsuck", true);
            }
        }

        @Name("highfive")
        @Description("High-five a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to high-five.", required = true))
        public static class HighFive extends ImageActionSlash {
            public HighFive() {
                super("Highfive", "Highfives with the specified user.", EmoteReference.TALKING,
                        "commands.action.highfive", "highfive", "commands.action.lonely.highfive", "commands.action.self.highfive", true
                );
            }
        }

        @Name("lick")
        @Description("Lick a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to lick.", required = true))
        public static class Lick extends ImageActionSlash {
            public Lick() {
                super("lick", "Licks the specified user.", EmoteReference.TALKING,
                        "commands.action.lick", "lick", "commands.action.lonely.lick", "commands.action.self.lick"
                );
            }
        }

        @Name("smile")
        @Description("Smile at a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to smile at.", required = true))
        public static class Smile extends ImageActionSlash {
            public Smile() {
                super("Smile", "Smiles at someone", EmoteReference.TALKING,
                        "commands.action.smile", "smile", "commands.action.lonely.smile", "commands.action.self.smile", true
                );
            }
        }

        @Name("bite")
        @Description("Bite a user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to bite.", required = true))
        public static class Bite extends ImageActionSlash {
            public Bite() {
                super("Bite", "Bites the specified user.", EmoteReference.TALKING,
                        "commands.action.bite", "bite", "commands.action.lonely.bite", "commands.action.self.bite"
                );
            }
        }

        @Name("teehee")
        @Description("Teehee~")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to tee-hee at~.", required = true))
        public static class Teehee extends ImageActionSlash {
            public Teehee() {
                super("Teehee", "Teehee~", EmoteReference.EYES,
                        "commands.action.teehee", "teehee", "commands.action.lonely.teehee", "commands.action.self.teehee", true);
            }
        }

        @Name("lewd")
        @Description("Random image that says lewd. Command itself is not lewd.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user that's being lewd."))
        public static class Lewd extends ImageCmdSlash {
            public Lewd() {
                super("T-Too lewd!", "lewd", "lewd", "commands.action.lewd");
            }
        }

        @Name("meow")
        @Description("Meows at the specific user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to meow to."))
        public static class Meow extends ImageCmdSlash {
            public Meow() {
                super("Meows at the specified user.", "meow", MEOW.get(), "commands.action.meow");
            }
        }

        @Name("nom")
        @Description("Noms the specified user.")
        @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to nom."))
        public static class Nom extends ImageCmdSlash {
            public Nom() {
                super("*nom nom*", "nom", "nom", "commands.action.nom");
            }
        }

        @Name("facedesk")
        @Description("When it's just too much to handle.")
        public static class FaceDesk extends ImageCmdSlash {
            public FaceDesk() {
                super("When it's just too much to handle.", "facedesk", "banghead", "commands.action.facedesk", true);
            }
        }
    }

    @Name("pat")
    @Description("Pat a user.")
    @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to pat.", required = true))
    public static class Pat extends ImageActionSlash {
        public Pat() {
            super("Pat", "Pats the specified user.", EmoteReference.TALKING,
                    "commands.action.pat", "pat", "commands.action.lonely.pat", "commands.action.self.pat");
        }
    }

    @Name("hug")
    @Description("Hug a user.")
    @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to hug.", required = true))
    public static class Hug extends ImageActionSlash {
        public Hug() {
            super("Hug", "Hugs the specified user.", EmoteReference.TALKING,
                    "commands.action.hug", "hug", "commands.action.lonely.hug", "commands.action.self.hug"
            );
        }
    }

    @Name("kiss")
    @Description("Kiss a user.")
    @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to kiss.", required = true))
    public static class Kiss extends ImageActionSlash {
        public Kiss() {
            super("Kiss", "Kisses the specified user.", EmoteReference.TALKING,
                    "commands.action.kiss", "kiss", "commands.action.lonely.kiss", "commands.action.self.kiss"
            );
        }
    }

    @Name("poke")
    @Description("Poke a user.")
    @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to poke.", required = true))
    public static class Poke extends ImageActionSlash {
        public Poke() {
            super("Poke", "Pokes the specified user.", EmoteReference.TALKING,
                    "commands.action.poke", "poke", "commands.action.lonely.poke", "commands.action.self.poke"
            );
        }
    }

    @Name("slap")
    @Description("Slap a user.")
    @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to slap.", required = true))
    public static class Slap extends ImageActionSlash {
        public Slap() {
            super("Slap", "Slaps the specified user ;).", EmoteReference.TALKING,
                    "commands.action.slap", "slap", "commands.action.lonely.slap", "commands.action.self.slap"
            );
        }
    }

    @Name("tickle")
    @Description("Tickle a user.")
    @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to tickle.", required = true))
    public static class Tickle extends ImageActionSlash {
        public Tickle() {
            super("Tickle", "Tickles the specified user.", EmoteReference.JOY,
                    "commands.action.tickle", "tickle", "commands.action.lonely.tickle", "commands.action.self.tickle"
            );
        }
    }
    @Name("pout")
    @Description("Pout at a user.")
    @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to pout at.", required = true))
    public static class Pout extends ImageActionSlash {
        public Pout() {
            super("Pout", "Pouts at the specified user.", EmoteReference.TALKING,
                    "commands.action.pout", "pout", "commands.action.lonely.pout", "commands.action.self.pout", true
            );
        }
    }

    @Name("cuddle")
    @Options(@Options.Option(type = OptionType.USER, name = "user", description = "The user to cuddle.", required = true))
    public static class Cuddle extends ImageActionSlash {
        public Cuddle() {
            super("Cuddle", "Cuddles someone", EmoteReference.HEART,
                    "commands.action.cuddle", "cuddle", "commands.action.lonely.cuddle", "commands.action.self.cuddle"
            );
        }
    }

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Pat.class);
        cr.registerSlash(Hug.class);
        cr.registerSlash(Kiss.class);
        cr.registerSlash(Poke.class);
        cr.registerSlash(Slap.class);
        cr.registerSlash(Tickle.class);
        cr.registerSlash(Pout.class);
        cr.registerSlash(Cuddle.class);
        // Do the rest on subcommands?
        cr.registerSlash(Action.class);

        //pat();
        cr.register("pat", new ImageActionCmd(
                "Pat", "Pats the specified user.", EmoteReference.TALKING,
                "commands.action.pat", "pat", "commands.action.lonely.pat", "commands.action.self.pat"
        ));

        //hug();
        cr.register("hug", new ImageActionCmd(
                "Hug", "Hugs the specified user.", EmoteReference.TALKING,
                "commands.action.hug", "hug", "commands.action.lonely.hug", "commands.action.self.hug"
        ));

        //kiss();
        cr.register("kiss", new ImageActionCmd(
                "Kiss", "Kisses the specified user.", EmoteReference.TALKING,
                "commands.action.kiss", "kiss", "commands.action.lonely.kiss", "commands.action.self.kiss"
        ));

        //poke();
        cr.register("poke", new ImageActionCmd(
                "Poke", "Pokes the specified user.", EmoteReference.TALKING,
                "commands.action.poke", "poke", "commands.action.lonely.poke", "commands.action.self.poke"
        ));

        //slap();
        cr.register("slap", new ImageActionCmd(
                "Slap", "Slaps the specified user ;).", EmoteReference.TALKING,
                "commands.action.slap", "slap", "commands.action.lonely.slap", "commands.action.self.slap"
        ));

        //bite();
        cr.register("bite", new ImageActionCmd(
                "Bite", "Bites the specified user.", EmoteReference.TALKING,
                "commands.action.bite", "bite", "commands.action.lonely.bite", "commands.action.self.bite"
        ));

        //tickle();
        cr.register("tickle", new ImageActionCmd(
                "Tickle", "Tickles the specified user.", EmoteReference.JOY,
                "commands.action.tickle", "tickle", "commands.action.lonely.tickle", "commands.action.self.tickle"
        ));

        //highfive();
        cr.register("highfive", new ImageActionCmd(
                "Highfive", "Highfives with the specified user.", EmoteReference.TALKING,
                "commands.action.highfive", "highfive", "commands.action.lonely.highfive", "commands.action.self.highfive", true
        ));

        //pout();
        cr.register("pout", new ImageActionCmd(
                "Pout", "Pouts at the specified user.", EmoteReference.TALKING,
                "commands.action.pout", "pout", "commands.action.lonely.pout", "commands.action.self.pout", true
        ));

        //lick();
        cr.register("lick", new ImageActionCmd(
                "lick", "Licks the specified user.", EmoteReference.TALKING,
                "commands.action.lick", "lick", "commands.action.lonely.lick", "commands.action.self.lick"
        ));

        //teehee();
        cr.register("teehee", new ImageActionCmd("Teehee", "Teehee~", EmoteReference.EYES,
                "commands.action.teehee", "teehee", "commands.action.lonely.teehee", "commands.action.self.teehee", true));

        //smile();
        cr.register("smile", new ImageActionCmd("Smile", "Smiles at someone", EmoteReference.TALKING,
                "commands.action.smile", "smile", "commands.action.lonely.smile", "commands.action.self.smile", true));

        //stare();
        cr.register("stare", new ImageActionCmd("Stare", "Stares at someone", EmoteReference.EYES,
                "commands.action.stare", "stare", "commands.action.lonely.stare", "commands.action.self.stare", true));

        //holdhands();
        cr.register("holdhands", new ImageActionCmd("Hold Hands", "Hold someone's hands", EmoteReference.HEART,
                "commands.action.holdhands", "handholding", "commands.action.lonely.holdhands", "commands.action.self.holdhands", true));

        //cuddle();
        cr.register("cuddle", new ImageActionCmd("Cuddle", "Cuddles someone", EmoteReference.HEART,
                "commands.action.cuddle", "cuddle", "commands.action.lonely.cuddle", "commands.action.self.cuddle"));

        //blush();
        cr.register("blush", new ImageActionCmd("Blush", "Blushes at someone", EmoteReference.HEART,
                "commands.action.blush", "blush", "commands.action.lonely.blush", "commands.action.self.blush", true));

        //nuzzle();
        cr.register("nuzzle", new ImageActionCmd("Nuzzle Command", "Nuzzles the specified user.", EmoteReference.TALKING,
                "commands.action.nuzzle", NUZZLE.get(), "commands.action.lonely.nuzzle", "commands.action.self.nuzzle", true
        ));

        //bloodsuck();
        cr.register("bloodsuck", new ImageActionCmd("Bloodsuck command", "Sucks the blood of a user", EmoteReference.TALKING,
                "commands.action.bloodsuck", BLOODSUCK.get(), "commands.action.lonely.bloodsuck", "commands.action.self.bloodsuck", true)
        );

        //tsundere();
        cr.register("tsundere", new TextActionCmd("Y-You baka!", EmoteReference.MEGA + "%s", TSUNDERE.get()));

        //lewd();
        cr.register("lewd", new ImageCmd("T-Too lewd!", "lewd", "lewd", "commands.action.lewd"));

        //meow();
        cr.register("meow", new ImageCmd("Meows at the specified user.", "meow", MEOW.get(), "commands.action.meow"));
        cr.registerAlias("meow", "mew");

        //nom();
        cr.register("nom", new ImageCmd("*nom nom*", "nom", "nom", "commands.action.nom"));

        //facedesk();
        cr.register("facedesk", new ImageCmd("When it's just too much to handle.", "facedesk", "banghead",
                "commands.action.facedesk", true));
    }
}
