package top.nomelin.iot.model;


public class User {
    private Integer id;
    private String name;
    private String password;

    private String pushplusToken;//pushplus平台的好友令牌.
    private String token;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPushplusToken() {
        return pushplusToken;
    }

    public void setPushplusToken(String pushplusToken) {
        this.pushplusToken = pushplusToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", pushPlusToken='" + pushplusToken + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
