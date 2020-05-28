package models;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "grantType",
        "clientId",
        "userName",
        "password"
})
public class UserAuth {

    @JsonProperty("grantType")
    private String grantType;
    @JsonProperty("clientId")
    private String clientId;
    @JsonProperty("userName")
    private String userName;
    @JsonProperty("password")
    private String password;

    @JsonProperty("grantType")
    public String getGrantType() {
        return grantType;
    }
    @JsonProperty("grantType")
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    @JsonProperty("clientId")
    public String getClientId() {
        return clientId;
    }
    @JsonProperty("clientId")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("userName")
    public String getUserName() {
        return userName;
    }
    @JsonProperty("userName")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @JsonProperty("password")
    public String getPassword() {
        return password;
    }
    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }
}
