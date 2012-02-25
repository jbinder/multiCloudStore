package at.ac.tuwien.multicloudstore.android.common;

import java.io.Serializable;

public class UserAccount implements Serializable {

    private String type;
    private String serverAddress;
    private String userName;
    private String password;
    private Boolean enabled = true;

    public UserAccount(String type, String serverAddress, String userName, String password) {
        this.type = type;
        this.serverAddress = serverAddress;
        this.userName = userName;
        this.password = password;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String toString() {
        return type + "://" + userName + ":" + password + "@" + serverAddress;
    }
}
