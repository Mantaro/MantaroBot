package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

class SafeMember extends SafeUser {
    private final Member member;

    SafeMember(Member member) {
        super(member.getUser());
        this.member = member;
    }

    public Color getColor() {
        return member.getColor();
    }

    public OffsetDateTime getJoinDate() {
        return member.getJoinDate();
    }

    public Game getGame() {
        return member.getGame();
    }

    public OnlineStatus getOnlineStatus() {
        return member.getOnlineStatus();
    }

    public String getName() {
        return member.getEffectiveName();
    }

    public boolean isOwner() {
        return member.isOwner();
    }

    public List<SafeRole> getRoles() {
        return member.getRoles().stream().map(SafeRole::new).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return member.toString();
    }
}
