package net.kodehawa.mantarobot.data;

import java.util.Collections;
import java.util.List;

public class ConnectionWatcherData {
    public final List<String> jvmargs;
    public final List<String> owners;
    public final int ping;
    public final int reboots;

    public ConnectionWatcherData(List<String> owners, List<String> jvmargs, int reboots, int ping) {
        this.owners = Collections.unmodifiableList(owners);
        this.jvmargs = Collections.unmodifiableList(jvmargs);
        this.reboots = reboots;
        this.ping = ping;
    }

    @Override
    public String toString() {
        return String.format("```prolog\nPing: %s\nReboots: %s\nOwners: %s\nJVM Args: %s```",
                ping,
                reboots,
                String.join(", ", owners.toArray(new CharSequence[0])),
                String.join(" ", jvmargs.toArray(new CharSequence[0])));
    }
}
