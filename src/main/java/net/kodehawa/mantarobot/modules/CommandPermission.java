package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.function.Predicate;

public enum CommandPermission implements Predicate<Member> {
	USER(member -> true, "User"),
	BOT_OWNER(MantaroData.config().get()::isOwner, "Bot Owner"),
	ADMIN(member -> BOT_OWNER.test(member) || member.getPermissions().contains(Permission.ADMINISTRATOR) || member.isOwner(), "Adminstrator");

	private final Predicate<Member> memberPredicate;
	private String verbose;

	CommandPermission(Predicate<Member> memberPredicate, String s) {
		this.memberPredicate = memberPredicate;
		verbose = s;
	}

	@Override
	public boolean test(Member member) {
		return memberPredicate.test(member);
	}

	@Override
	public String toString() {
		return verbose;
	}
}
