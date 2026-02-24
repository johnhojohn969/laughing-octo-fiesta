package com.is32.facades.dto;

import java.io.Serializable;

public class RewardDisplayData implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String rewardType;
    private String rewardDescription;
    private Double rewardValue;
    private String accountId;
    private Double minSpend;
    private Double maxRewardCap;

    public String getRewardType()
    {
        return rewardType;
    }

    public void setRewardType(final String rewardType)
    {
        this.rewardType = rewardType;
    }

    public String getRewardDescription()
    {
        return rewardDescription;
    }

    public void setRewardDescription(final String rewardDescription)
    {
        this.rewardDescription = rewardDescription;
    }

    public Double getRewardValue()
    {
        return rewardValue;
    }

    public void setRewardValue(final Double rewardValue)
    {
        this.rewardValue = rewardValue;
    }

    public String getAccountId()
    {
        return accountId;
    }

    public void setAccountId(final String accountId)
    {
        this.accountId = accountId;
    }

    public Double getMinSpend()
    {
        return minSpend;
    }

    public void setMinSpend(final Double minSpend)
    {
        this.minSpend = minSpend;
    }

    public Double getMaxRewardCap()
    {
        return maxRewardCap;
    }

    public void setMaxRewardCap(final Double maxRewardCap)
    {
        this.maxRewardCap = maxRewardCap;
    }
}
