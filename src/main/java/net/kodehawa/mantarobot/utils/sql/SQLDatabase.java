package net.kodehawa.mantarobot.utils.sql;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLDatabase {
    private static final SQLDatabase sql;

    static {
        sql = new SQLDatabase();
    }

    private MysqlDataSource dataSource;

    public SQLDatabase() {
        dataSource = new MysqlDataSource();
        dataSource.setDatabaseName("mantarologs");
        dataSource.setUser("root");
        //TODO dataSource.setPassword();
        dataSource.setServerName("localhost");
        dataSource.setURL(dataSource.getURL() + "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true&useSSL=false");
    }

    public static SQLDatabase getInstance() {
        return sql;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public SQLAction run(SQLTask task) throws SQLException {
        return new SQLAction(getConnection(), task);
    }
}
