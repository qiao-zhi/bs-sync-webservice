package cn.bs.qlq.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模拟一个简单的连接池并且执行SQL
 * 
 * @author Administrator
 *
 */
public class JDBCUtils {

	private static final Logger log = LoggerFactory.getLogger(JDBCUtils.class);

	private static String JDBC_DRIVER = ResourcesUtil.getValue("db", "jdbc.driver");
	private static String JDBC_URL = ResourcesUtil.getValue("db", "jdbc.url");
	private static String JDBC_USERNAME = ResourcesUtil.getValue("db", "jdbc.username");
	private static String JDBC_PASSWORD = ResourcesUtil.getValue("db", "jdbc.password");

	private static LinkedList<Connection> connections = new LinkedList<>();

	public static Object executeSQL(String sql, Object... params) {
		Connection connection = null;
		try {
			connection = getConnection();
			PreparedStatement statement = connection.prepareStatement(sql);

			// 设置参数(注意JDBC的所有下标从1开始)
			if (params != null && params.length > 0) {
				for (int i = 0, length_1 = params.length; i < length_1; i++) {
					Object param = params[i];
					if (param instanceof String) {
						statement.setString(i + 1, (String) param);
					} else if (param instanceof Long || param instanceof Integer) {
						statement.setLong(i + 1, Long.valueOf(param.toString()));
					}
				}
			}

			// 查询
			if (sql.contains("select")) {
				ResultSet result = statement.executeQuery();

				// 遍历每一行的数据
				while (result.next()) {
					return result.getInt(1);
				}

				return -1;
			}

			// 更新
			statement.executeUpdate();
		} catch (Exception e) {
			log.error("执行SQL出错,sql -> {}", sql, e);
		} finally {
			if (connection != null) {
				releaseConnection(connection);
			}
		}
		return null;
	}

	private static Connection getConnection() throws ClassNotFoundException, SQLException {
		if (connections.size() == 0) {
			initConnections();
		}

		return connections.removeFirst();
	}

	private static void releaseConnection(Connection connection) {
		connections.add(connection);
	}

	private static void initConnections() {
		try {
			Class.forName(JDBC_DRIVER);
			for (int i = 0; i < 5; i++) {
				Connection conn = (Connection) DriverManager.getConnection(JDBC_URL, JDBC_USERNAME, JDBC_PASSWORD);
				conn.setAutoCommit(true);
				connections.add(conn);
			}
		} catch (Exception e) {
			// 记录日志
		}
	}

	public static void main(String[] args) {
		executeSQL("select * from ");
	}
}