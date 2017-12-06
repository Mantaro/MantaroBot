package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

class SafeRole extends SafeISnowflake<Role> {
    SafeRole(Role role) {
        super(role);
    }

    public String getName() {
        return snowflake.getName();
    }

    public List<Permission> getPermissions() {
        return snowflake.getPermissions();
    }

    public long getPermissionsRaw() {
        return snowflake.getPermissionsRaw();
    }

    public int getPosition() {
        return snowflake.getPosition();
    }

    public int getPositionRaw() {
        return snowflake.getPositionRaw();
    }

    public boolean isManaged() {
        return snowflake.isManaged();
    }

    public boolean isPublicRole() {
        return snowflake.isPublicRole();
    }

    public boolean isMentionable() {
        return snowflake.isMentionable();
    }

    public boolean isSeparate() {
        return snowflake.isHoisted();
    }
}
