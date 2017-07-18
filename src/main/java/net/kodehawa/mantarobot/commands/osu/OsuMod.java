package net.kodehawa.mantarobot.commands.osu;

import com.osu.api.ciyfhx.Mod;

import java.util.Arrays;

public enum OsuMod {
    HIDDEN(Mod.HIDDEN, "HD"),
    HARD_ROCK(Mod.HARD_ROCK, "HR"),
    DOUBLE_TIME(Mod.DOUBLE_TIME, "DT"),
    FLASHLIGHT(Mod.FLASHLIGHT, "FL"),
    NO_FAIL(Mod.NO_FAIL, "NF"),
    AUTOPLAY(Mod.AUTOPLAY, "AP"),
    HALF_TIME(Mod.HALF_TIME, "HT"),
    EASY(Mod.EASY, "EZ"),
    NIGHTCORE(Mod.NIGHTCORE, "NC"),
    RELAX(Mod.RELAX, "RX"),
    SPUN_OUT(Mod.SPUN_OUT, "SO"),
    SUDDEN_DEATH(Mod.SUDDEN_DEATH, "SD"),
    PERFECT(Mod.PERFECT, "PF");

    private final String abbreviation;
    private final Mod mod;
    OsuMod(Mod mod, String abbreviation) {

        this.mod = mod;
        this.abbreviation = abbreviation;
    }

    public static OsuMod get(Mod mod) {
        return Arrays.stream(values())
                .filter(osuMod -> osuMod.mod == mod)
                .findFirst().orElse(null);
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public Mod getMod() {
        return mod;
    }
}
