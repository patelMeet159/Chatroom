/**
 * Person
 *
 * Represents a chat user.
 */
public class Person {
    private String userName;
    private String password;
    private String ipAddress;
    private int portNumber;

    public Person(String userName) {
        this.userName = userName;
    }

    public Person(String userName, String password, String ipAddress, int portNumber) {
        this.userName = userName;
        this.password = password;
        this.ipAddress = ipAddress;
        this.portNumber = portNumber;
    }

    public String getUsername() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setUsername(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public String toString() {
        return userName;
    }
}