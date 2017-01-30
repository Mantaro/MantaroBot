package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.function.Predicate;

public enum CommandPermission implements Predicate<Member> {
	USER(member -> true), ADMIN(member -> member.getPermissions().contains(Permission.ADMINISTRATOR)), BOT_OWNER(MantaroData.getConfig().get()::isOwner);

	private final Predicate<Member> memberPredicate;

	CommandPermission(Predicate<Member> memberPredicate) {
		this.memberPredicate = memberPredicate;
	}

	@Override
	public boolean test(Member member) {
		return memberPredicate.test(member);
	}
}
