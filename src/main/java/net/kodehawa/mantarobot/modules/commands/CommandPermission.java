package net.kodehawa.mantarobot.modules.commands;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.kodehawa.mantarobot.data.MantaroData;

public enum CommandPermission {
    USER() {
        @Override
        public boolean test(Member member) {
            return true;
        }
    },
    ADMIN() {
        @Override
        public boolean test(Member member) {
            return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR) ||
                    member.hasPermission(Permission.MANAGE_SERVER) || OWNER.test(member) ||
                    member.getRoles().stream().anyMatch(role -> role.getName().equals("Bot Commander"));
        }
    },
    OWNER() {
        @Override
        public boolean test(Member member) {
            return MantaroData.config().get().isOwner(member);
        }
    };

    public abstract boolean test(Member member);

    @Override
    public String toString() {
        String name = name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
