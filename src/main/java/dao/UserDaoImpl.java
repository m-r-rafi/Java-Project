package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import model.User;

public class UserDaoImpl implements UserDao {
	private static final String TABLE_NAME = "users";

	public UserDaoImpl() {
	}

	@Override
	public void setup() throws SQLException {
		try (Connection connection = Database.getConnection();
			 Statement stmt = connection.createStatement()) {
			String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
					+ "username       TEXT    PRIMARY KEY,"
					+ "password       TEXT    NOT NULL,"
					+ "preferred_name TEXT    NOT NULL"
					+ ")";
			stmt.executeUpdate(sql);
		}
	}

	@Override
	public User getUser(String username, String password) throws SQLException {
		String sql = "SELECT username, password, preferred_name "
				+ "FROM " + TABLE_NAME + " "
				+ "WHERE username = ? AND password = ?";
		try (Connection conn = Database.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, username);
			stmt.setString(2, password);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					User user = new User();
					user.setUsername(rs.getString("username"));
					user.setPassword(rs.getString("password"));
					user.setPreferredName(rs.getString("preferred_name"));
					return user;
				}
				return null;
			}
		}
	}

	@Override
	public User createUser(String username, String password, String preferredName) throws SQLException {
		String sql = "INSERT INTO " + TABLE_NAME
				+ " (username, password, preferred_name) VALUES (?, ?, ?)";
		try (Connection conn = Database.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, username);
			stmt.setString(2, password);
			stmt.setString(3, preferredName);
			stmt.executeUpdate();

			// return the newly created User object
			User user = new User();
			user.setUsername(username);
			user.setPassword(password);
			user.setPreferredName(preferredName);
			return user;
		}
	}
}
