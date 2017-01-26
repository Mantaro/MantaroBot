package net.kodehawa.mantarobot;

import net.dv8tion.jda.core.JDAInfo;

public final class MantaroInfo {
	public static final String BUILD = String.format("%s.%s.%s", "@versionMajor@", "@versionMinor@", "@versionRevision@");
	public static final String DATE = "@versionBuild@";
	public static final String VERSION = BUILD + DATE + "_J" + JDAInfo.VERSION;
}
