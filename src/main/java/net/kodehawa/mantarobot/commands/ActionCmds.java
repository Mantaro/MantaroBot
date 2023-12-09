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

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.action.ImageActionCmd;
import net.kodehawa.mantarobot.commands.action.ImageActionSlash;
import net.kodehawa.mantarobot.commands.action.ImageCmd;
import net.kodehawa.mantarobot.commands.action.ImageCmdSlash;
import net.kodehawa.mantarobot.commands.action.TextActionCmd;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Alias;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.command.meta.Module;
import net.kodehawa.mantarobot.core.command.helpers.CommandCategory;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.util.List;

@Module
// Most of the commands in here are just definitions, the actual implementation is done in their parent class.
public class ActionCmds {
    private static final DataManager<List<String>> BLOODSUCK = new SimpleFileDataManager("assets/mantaro/texts/bloodsuck.txt");
    private static final DataManager<List<String>> MEOW = new SimpleFileDataManager("assets/mantaro/texts/meow.txt");
    private static final DataManager<List<String>> NUZZLE = new SimpleFileDataManager("assets/mantaro/texts/nuzzle.txt");
    private static final DataManager<List<String>> TSUNDERE = new SimpleFileDataManager("assets/mantaro/texts/tsundere.txt");

    @Subscribe
    public void register(CommandRegistry cr) {
        // This all are deferred in the parent class.
        // Slash
        cr.registerSlash(Pat.class);
        cr.registerSlash(Hug.class);
        cr.registerSlash(Kiss.class);
        cr.registerSlash(Poke.class);
        cr.registerSlash(Slap.class);
        cr.registerSlash(Tickle.class);
        cr.registerSlash(Pout.class);
        cr.registerSlash(Cuddle.class);
        cr.registerSlash(Action.class); // Subcommands contain the rest due to Discord slash command # limits.

        // Text
        cr.register(PatText.class);
        cr.register(HugText.class);
        cr.register(KissText.class);
        cr.register(PokeText.class);
        cr.register(SlapText.class);
        cr.register(BiteText.class);
        cr.register(TickleText.class);
        cr.register(HighFiveText.class);
        cr.register(PoutText.class);
        cr.register(LickText.class);
        cr.register(TeeheeText.class);
        cr.register(SmileText.class);
        cr.register(StareText.class);
        cr.register(HoldHandsText.class);
        cr.register(CuddleText.class);
        cr.register(BlushText.class);
        cr.register(NuzzleText.class);
        cr.register(BloodsuckText.class);
        cr.register(Tsundere.class);
        cr.register(Lewd.class);
        cr.register(Meow.class);
        cr.register(Nom.class);
        cr.register(Facedesk.class);
    }

    // Slash:
    @Name("action")
    @Description("A bunch of action commands that didn't fit into a separate command")
    @Category(CommandCategory.ACTION)
    public static class Action extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) { }

        @Name("holdhands")
        @Description("Holds the hand of a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to hold hands with.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to hold hands with and maybe a message you want to send.")
        })
        public static class HoldHands extends ImageActionSlash {
            public HoldHands() {
                super("Hold Hands", "Hold someone's hands", EmoteReference.HEART,
                        "commands.action.holdhands", "handholding", "commands.action.lonely.holdhands", "commands.action.self.holdhands", true, true);
            }
        }

        @Name("stare")
        @Description("Stares at a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to stare at.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to stare at and maybe a message you want to send.")
        })
        public static class Stare extends ImageActionSlash {
            public Stare() {
                super("Stare", "Stares at someone", EmoteReference.EYES,
                        "commands.action.stare", "stare", "commands.action.lonely.stare", "commands.action.self.stare", true, true);
            }
        }

        @Name("blush")
        @Description("Blushes at a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to blush at.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to blust at and maybe a message you want to send.")
        })
        public static class Blush extends ImageActionSlash {
            public Blush() {
                super("Blush", "Blushes at someone", EmoteReference.HEART,
                        "commands.action.blush", "blush", "commands.action.lonely.blush", "commands.action.self.blush", true, true);
            }
        }

        @Name("nuzzle")
        @Description("Nuzzles a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to nuzzle.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to nuzzle and maybe a message you want to send.")
        })
        public static class Nuzzle extends ImageActionSlash {
            public Nuzzle() {
                super("Nuzzle Command", "Nuzzles the specified user.", EmoteReference.TALKING,
                        "commands.action.nuzzle", NUZZLE.get(), "commands.action.lonely.nuzzle", "commands.action.self.nuzzle", true, true);
            }
        }

        @Name("bloodsuck")
        @Description("Sucks the blood of a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to suck the blood of.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to suck the blood of and maybe a message you want to send.")
        })
        public static class BloodSuck extends ImageActionSlash {
            public BloodSuck() {
                super("Bloodsuck command", "Sucks the blood of a user", EmoteReference.TALKING,
                        "commands.action.bloodsuck", BLOODSUCK.get(), "commands.action.lonely.bloodsuck", "commands.action.self.bloodsuck", true, true);
            }
        }

        @Name("highfive")
        @Description("High-five a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to high-five.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to high-five and maybe a message you want to send.")
        })
        public static class HighFive extends ImageActionSlash {
            public HighFive() {
                super("Highfive", "Highfives with the specified user.", EmoteReference.TALKING,
                        "commands.action.highfive", "highfive", "commands.action.lonely.highfive", "commands.action.self.highfive", true, true
                );
            }
        }

        @Name("lick")
        @Description("Lick a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to lick.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to lick and maybe a message you want to send.")
        })
        public static class Lick extends ImageActionSlash {
            public Lick() {
                super("lick", "Licks the specified user.", EmoteReference.TALKING,
                        "commands.action.lick", "lick", "commands.action.lonely.lick", "commands.action.self.lick", false, true
                );
            }
        }

        @Name("smile")
        @Description("Smile at a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to smile at.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to smile at and maybe a message you want to send.")
        })
        public static class Smile extends ImageActionSlash {
            public Smile() {
                super("Smile", "Smiles at someone", EmoteReference.TALKING,
                        "commands.action.smile", "smile", "commands.action.lonely.smile", "commands.action.self.smile", true, true
                );
            }
        }

        @Name("bite")
        @Description("Bite a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to bite.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to bite and maybe a message you want to send.")
        })
        public static class Bite extends ImageActionSlash {
            public Bite() {
                super("Bite", "Bites the specified user.", EmoteReference.TALKING,
                        "commands.action.bite", "bite", "commands.action.lonely.bite", "commands.action.self.bite", false, true
                );
            }
        }

        @Name("teehee")
        @Description("Teehee~")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to tee-hee at~.", required = true),
                @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to tee-hee at and maybe a message you want to send.")
        })
        public static class Teehee extends ImageActionSlash {
            public Teehee() {
                super("Teehee", "Teehee~", EmoteReference.EYES,
                        "commands.action.teehee", "teehee", "commands.action.lonely.teehee", "commands.action.self.teehee", true, true);
            }
        }

        @Name("lewd")
        @Description("Random image that says lewd. Command itself is not lewd.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user that's being lewd.")
        })
        public static class Lewd extends ImageCmdSlash {
            public Lewd() {
                super("lewd", "commands.action.lewd");
            }
        }

        @Name("meow")
        @Description("Meows at the specific user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to meow to.")
        })
        public static class Meow extends ImageCmdSlash {
            public Meow() {
                super(MEOW.get(), "commands.action.meow");
            }
        }

        @Name("nom")
        @Description("Noms the specified user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to nom.")
        })
        public static class Nom extends ImageCmdSlash {
            public Nom() {
                super("nom", "commands.action.nom");
            }
        }

        @Name("facedesk")
        @Description("When it's just too much to handle.")
        public static class FaceDesk extends ImageCmdSlash {
            public FaceDesk() {
                super("banghead", "commands.action.facedesk", true);
            }
        }
    }

    @Name("pat")
    @Description("Pat a user.")
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to pat.", required = true),
            @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to pat and maybe a message you want to send.")
    })
    public static class Pat extends ImageActionSlash {
        public Pat() {
            super("Pat", "Pats the specified user.", EmoteReference.TALKING,
                    "commands.action.pat", "pat", "commands.action.lonely.pat", "commands.action.self.pat");
        }
    }

    @Name("hug")
    @Description("Hug a user.")
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to hug.", required = true),
            @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to hug and maybe a message you want to send.")
    })
    public static class Hug extends ImageActionSlash {
        public Hug() {
            super("Hug", "Hugs the specified user.", EmoteReference.TALKING,
                    "commands.action.hug", "hug", "commands.action.lonely.hug", "commands.action.self.hug"
            );
        }
    }

    @Name("kiss")
    @Description("Kiss a user.")
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to kiss.", required = true),
            @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to kiss and maybe a message you want to send.")
    })
    public static class Kiss extends ImageActionSlash {
        public Kiss() {
            super("Kiss", "Kisses the specified user.", EmoteReference.TALKING,
                    "commands.action.kiss", "kiss", "commands.action.lonely.kiss", "commands.action.self.kiss"
            );
        }
    }

    @Name("poke")
    @Description("Poke a user.")
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to poke.", required = true),
            @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to poke and maybe a message you want to send.")
    })
    public static class Poke extends ImageActionSlash {
        public Poke() {
            super("Poke", "Pokes the specified user.", EmoteReference.TALKING,
                    "commands.action.poke", "poke", "commands.action.lonely.poke", "commands.action.self.poke"
            );
        }
    }

    @Name("slap")
    @Description("Slap a user.")
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to slap.", required = true),
            @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to slap and maybe a message you want to send.")
    })
    public static class Slap extends ImageActionSlash {
        public Slap() {
            super("Slap", "Slaps the specified user ;).", EmoteReference.TALKING,
                    "commands.action.slap", "slap", "commands.action.lonely.slap", "commands.action.self.slap"
            );
        }
    }

    @Name("tickle")
    @Description("Tickle a user.")
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to tickle.", required = true),
            @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to tickle and maybe a message you want to send.")
    })
    public static class Tickle extends ImageActionSlash {
        public Tickle() {
            super("Tickle", "Tickles the specified user.", EmoteReference.JOY,
                    "commands.action.tickle", "tickle", "commands.action.lonely.tickle", "commands.action.self.tickle"
            );
        }
    }
    @Name("pout")
    @Description("Pout at a user.")
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to pout at.", required = true),
            @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to pout at and maybe a message you want to send.")
    })
    public static class Pout extends ImageActionSlash {
        public Pout() {
            super("Pout", "Pouts at the specified user.", EmoteReference.TALKING,
                    "commands.action.pout", "pout", "commands.action.lonely.pout", "commands.action.self.pout", true, false
            );
        }
    }

    @Name("cuddle")
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to cuddle.", required = true),
            @Options.Option(type = OptionType.STRING, name = "extra", description = "Extra users to cuddle and maybe a message you want to send.")
    })
    public static class Cuddle extends ImageActionSlash {
        public Cuddle() {
            super("Cuddle", "Cuddles someone", EmoteReference.HEART,
                    "commands.action.cuddle", "cuddle", "commands.action.lonely.cuddle", "commands.action.self.cuddle"
            );
        }
    }

    // Text:
    @Name("pat")
    public static class PatText extends ImageActionCmd {
        public PatText() {
            super(
                    "Pat", "Pats the specified user.", EmoteReference.TALKING,
                    "commands.action.pat", "pat", "commands.action.lonely.pat", "commands.action.self.pat"
            );
        }
    }

    @Name("hug")
    public static class HugText extends ImageActionCmd {
        public HugText() {
            super(
                    "Hug", "Hugs the specified user.", EmoteReference.TALKING,
                    "commands.action.hug", "hug", "commands.action.lonely.hug", "commands.action.self.hug"
            );
        }
    }

    @Name("kiss")
    public static class KissText extends ImageActionCmd {
        public KissText() {
            super(
                    "Kiss", "Kisses the specified user.", EmoteReference.TALKING,
                    "commands.action.kiss", "kiss", "commands.action.lonely.kiss", "commands.action.self.kiss"
            );
        }
    }

    @Name("poke")
    public static class PokeText extends ImageActionCmd {
        public PokeText() {
            super(
                    "Poke", "Pokes the specified user.", EmoteReference.TALKING,
                    "commands.action.poke", "poke", "commands.action.lonely.poke", "commands.action.self.poke"
            );
        }
    }

    @Name("slap")
    public static class SlapText extends ImageActionCmd {
        public SlapText() {
            super(
                    "Slap", "Slaps the specified user ;).", EmoteReference.TALKING,
                    "commands.action.slap", "slap", "commands.action.lonely.slap", "commands.action.self.slap"
            );
        }
    }

    @Name("bite")
    public static class BiteText extends ImageActionCmd {
        public BiteText() {
            super(
                    "Bite", "Bites the specified user.", EmoteReference.TALKING,
                    "commands.action.bite", "bite", "commands.action.lonely.bite", "commands.action.self.bite"
            );
        }
    }

    @Name("tickle")
    public static class TickleText extends ImageActionCmd {
        public TickleText() {
            super(
                    "Tickle", "Tickles the specified user.", EmoteReference.JOY,
                    "commands.action.tickle", "tickle", "commands.action.lonely.tickle", "commands.action.self.tickle"
            );
        }
    }

    @Name("highfive")
    public static class HighFiveText extends ImageActionCmd {
        public HighFiveText() {
            super(
                    "Highfive", "Highfives with the specified user.", EmoteReference.TALKING,
                    "commands.action.highfive", "highfive", "commands.action.lonely.highfive", "commands.action.self.highfive", true
            );
        }
    }

    @Name("pout")
    public static class PoutText extends ImageActionCmd {
        public PoutText() {
            super(
                    "Pout", "Pouts at the specified user.", EmoteReference.TALKING,
                    "commands.action.pout", "pout", "commands.action.lonely.pout", "commands.action.self.pout", true
            );
        }
    }

    @Name("lick")
    public static class LickText extends ImageActionCmd {
        public LickText() {
            super(
                    "lick", "Licks the specified user.", EmoteReference.TALKING,
                    "commands.action.lick", "lick", "commands.action.lonely.lick", "commands.action.self.lick"
            );
        }
    }

    @Name("teehee")
    public static class TeeheeText extends ImageActionCmd {
        public TeeheeText() {
            super(
                    "Teehee", "Teehee~", EmoteReference.EYES,
                    "commands.action.teehee", "teehee", "commands.action.lonely.teehee", "commands.action.self.teehee", true
            );
        }
    }

    @Name("smile")
    public static class SmileText extends ImageActionCmd {
        public SmileText() {
            super(
                    "Smile", "Smiles at someone", EmoteReference.TALKING,
                    "commands.action.smile", "smile", "commands.action.lonely.smile", "commands.action.self.smile", true
            );
        }
    }

    @Name("stare")
    public static class StareText extends ImageActionCmd {
        public StareText() {
            super(
                    "Stare", "Stares at someone", EmoteReference.EYES,
                    "commands.action.stare", "stare", "commands.action.lonely.stare", "commands.action.self.stare", true
            );
        }
    }

    @Name("holdhands")
    public static class HoldHandsText extends ImageActionCmd {
        public HoldHandsText() {
            super(
                    "Hold Hands", "Hold someone's hands", EmoteReference.HEART,
                    "commands.action.holdhands", "handholding", "commands.action.lonely.holdhands", "commands.action.self.holdhands", true
            );
        }
    }

    @Name("cuddle")
    public static class CuddleText extends ImageActionCmd {
        public CuddleText() {
            super(
                    "Cuddle", "Cuddles someone", EmoteReference.HEART,
                    "commands.action.cuddle", "cuddle", "commands.action.lonely.cuddle", "commands.action.self.cuddle"
            );
        }
    }

    @Name("blush")
    public static class BlushText extends ImageActionCmd {
        public BlushText() {
            super(
                    "Blush", "Blushes at someone", EmoteReference.HEART,
                    "commands.action.blush", "blush", "commands.action.lonely.blush", "commands.action.self.blush", true
            );
        }
    }

    @Name("nuzzle")
    public static class NuzzleText extends ImageActionCmd {
        public NuzzleText() {
            super(
                    "Nuzzle", "Nuzzles the specified user.", EmoteReference.TALKING,
                    "commands.action.nuzzle", NUZZLE.get(), "commands.action.lonely.nuzzle", "commands.action.self.nuzzle", true
            );
        }
    }

    @Name("bloodsuck")
    public static class BloodsuckText extends ImageActionCmd {
        public BloodsuckText() {
            super(
                    "Bloodsuck", "Sucks the blood of a user", EmoteReference.TALKING,
                    "commands.action.bloodsuck", BLOODSUCK.get(), "commands.action.lonely.bloodsuck", "commands.action.self.bloodsuck", true
            );
        }
    }

    public static class Tsundere extends TextActionCmd {
        public Tsundere() {
            super("Y-You baka!", EmoteReference.MEGA + "%s", TSUNDERE.get());
        }
    }

    public static class Lewd extends ImageCmd {
        public Lewd() {
            super("T-Too lewd!", "lewd", "commands.action.lewd");
        }
    }

    @Alias("mew")
    public static class Meow extends ImageCmd {
        public Meow() {
            super("Meows at the specified user.", MEOW.get(), "commands.action.meow");
        }

    }

    public static class Nom extends ImageCmd {
        public Nom() {
            super("*nom nom*", "nom", "commands.action.nom");
        }
    }

    public static class Facedesk extends ImageCmd {
        public Facedesk() {
            super("When it's just too much to handle.", "banghead", "commands.action.facedesk", true);
        }
    }
}
