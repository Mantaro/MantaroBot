package net.kodehawa.mantarobot.utils.sql;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SQLAction {
	private static final ExecutorService SQL_SERVICE = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "SQL Thread ");
		t.setDaemon(true);
		return t;
	});

	public static Logger getLog() {
		return log;
	}

	private Connection conn;
	private SQLTask task;

	SQLAction(Connection conn, SQLTask task) {
		this.conn = conn;
		this.task = task;
	}

	public void complete() throws SQLException {
	    if(SQLDatabase.DISABLED) return;
		task.run(conn);
		if (!conn.isClosed())
			conn.close();
	}

	public void queue() {
		SQL_SERVICE.submit(() -> {
			try {
				complete();
			} catch (Exception e) {
				log.error("Error", e);
			}
		});
	}
}
