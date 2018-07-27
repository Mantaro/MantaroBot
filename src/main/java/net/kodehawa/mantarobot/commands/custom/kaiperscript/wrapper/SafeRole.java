package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

public class SafeRole extends SafeJDAObject<Role> {
    SafeRole(Role role) {
        super(role);
    }

    public String getName() {
        return object.getName();
    }

    public List<Permission> getPermissions() {
        return object.getPermissions();
    }

    public long getPermissionsRaw() {
        return object.getPermissionsRaw();
    }

    public int getPosition() {
        return object.getPosition();
    }

    public int getPositionRaw() {
        return object.getPositionRaw();
    }

    public boolean getIsManaged() {
        return object.isManaged();
    }

    public boolean getIsPublicRole() {
        return object.isPublicRole();
    }

    public boolean getIsMentionable() {
        return object.isMentionable();
    }

    public boolean getIsSeparate() {
        return object.isHoisted();
    }

    @Override
    public String toString() {
        return "Role(" + getId() + ")";
    }
}
