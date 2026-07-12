package data;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserInfo {
    private String customerId;
    private String sessionKey;
    private LocalDateTime lastValidTime;


    public UserInfo(String customerId, String sessionKey) {
        this.customerId = customerId;
        this.sessionKey = sessionKey;
        this.lastValidTime = LocalDateTime.now();
    }


    public LocalDateTime getLastValidTime() {
        return lastValidTime;
    }
    public void setLastValidTime(LocalDateTime lastValidTime) {
        this.lastValidTime = lastValidTime;
    }
    public String getCustomerId() {
        return customerId;
    }
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    public String getSessionKey() {
        return sessionKey;
    }
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public boolean isSessionValid() {
        return !LocalDateTime.now().isBefore(lastValidTime.plusMinutes(10));
    }

    public void resetLastValidTimeTime() {
        this.lastValidTime = LocalDateTime.now();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(customerId);
    }
}
