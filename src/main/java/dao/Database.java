package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
	private static final String DB_URL = "jdbc:sqlite:application.db";

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(DB_URL);
	}

	/** Only create the users table here — everything else is in its DAO. */
	public static void init() throws SQLException {
		try (Connection conn = getConnection();
			 Statement stmt = conn.createStatement()) {
			stmt.execute("""
        CREATE TABLE IF NOT EXISTS users (
          username       TEXT    PRIMARY KEY,
          password       TEXT    NOT NULL,
          preferred_name TEXT    NOT NULL
        )
        """);
		}
	}
}
