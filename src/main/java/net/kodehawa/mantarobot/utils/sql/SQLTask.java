package net.kodehawa.mantarobot.utils.sql;

import java.sql.Connection;

public interface SQLTask {
	void run(Connection c);
}
