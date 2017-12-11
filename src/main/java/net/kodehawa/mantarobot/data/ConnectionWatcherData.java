/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.data;

public class ConnectionWatcherData {
    public final String jvmargs;
    public final String owners;
    public final long ping;
    public final int reboots;

    public ConnectionWatcherData(String owners, String jvmargs, int reboots, long ping) {
        this.owners = owners;
        this.jvmargs = jvmargs;
        this.reboots = reboots;
        this.ping = ping;
    }

    @Override
    public String toString() {
        return String.format("```prolog\nPing: %s\nReboots: %s\nOwners: %s\nJVM Args: %s```",
                ping,
                reboots,
                owners.substring(1, owners.length() - 1),
                jvmargs.substring(1, jvmargs.length() - 1).replace(",", "")
        );
    }
}
