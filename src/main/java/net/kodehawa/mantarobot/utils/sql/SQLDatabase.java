package net.kodehawa.mantarobot.utils.sql;

import com.mysql.cj.jdbc.MysqlDataSource;
import net.kodehawa.mantarobot.data.MantaroData;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLDatabase {
    public static final boolean DISABLED = System.getProperty("mantaro.nosql", null) != null;

	private static final SQLDatabase sql;

	static {
		sql = new SQLDatabase();
	}

	public static SQLDatabase getInstance() {
		return sql;
	}

	private MysqlDataSource dataSource;

	public SQLDatabase() {
	    if(DISABLED) return;
		dataSource = new MysqlDataSource();
		dataSource.setDatabaseName("mantarologs");
		dataSource.setUser("root");
		dataSource.setPassword(MantaroData.config().get().getSqlPassword());
		dataSource.setServerName("localhost");
		dataSource.setURL(dataSource.getURL() + "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true&useSSL=false");
	}

	public Connection getConnection() throws SQLException {
	    if(DISABLED) return null;
		return dataSource.getConnection();
	}

	public SQLAction run(SQLTask task) throws SQLException {
		return new SQLAction(getConnection(), task);
	}
}
