package data;

import java.time.LocalDateTime;

public class Stake {
    private int stakeAmount;
    private LocalDateTime stakeTime;
    private UserInfo userInfo;

    public Stake(int stakeAmount, LocalDateTime stakeTime, UserInfo userInfo) {
        this.stakeAmount = stakeAmount;
        this.stakeTime = stakeTime;
        this.userInfo = userInfo;
    }

    public void setStakeAmount(int stakeAmount) {
        this.stakeAmount = stakeAmount;
    }
    public int getStakeAmount() {
        return stakeAmount;
    }

    public LocalDateTime getStakeTime() {
        return stakeTime;
    }
    public void setStakeTime(LocalDateTime stakeTime) {
        this.stakeTime = stakeTime;
    }
    public UserInfo getUserInfo() {
        return userInfo;
    }
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
