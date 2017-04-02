package net.kodehawa.mantarobot.utils.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SQLAction {
    public static final Logger LOGGER = LoggerFactory.getLogger("SQLAction");
    private static final ExecutorService SQL_SERVICE = Executors.newCachedThreadPool(r -> new Thread(r, "SQL Thread "));
    private Connection conn;
    private SQLTask task;

    SQLAction(Connection conn, SQLTask task) {
        this.conn = conn;
        this.task = task;
    }

    public void complete() throws SQLException {
        task.run(conn);
        if (!conn.isClosed())
            conn.close();
    }

    public void queue() {
        SQL_SERVICE.submit(() -> {
            try {
                complete();
            } catch (Exception e) {
                LOGGER.error("Error", e);
            }
        });
    }
}
