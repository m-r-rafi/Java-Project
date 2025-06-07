package model;

public class User {
	private String username;
	private String password;
	private String preferredName;

	public User() {
	}

	public User(String username, String password, String preferredName) {
		this.username = username;
		this.password = password;
		this.preferredName = preferredName;
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getPreferredName() {
		return preferredName;
	}
	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
	}
}
