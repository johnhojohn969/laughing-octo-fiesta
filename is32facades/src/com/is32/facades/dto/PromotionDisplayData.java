package com.is32.facades.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class PromotionDisplayData implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String uid;
    private String title;
    private String description;
    private String displayType;
    private String status;
    private boolean suspended;
    private boolean active;
    private Date startDate;
    private Date endDate;
    private int priority;
    private String imageUrl;
    private String termsAndConditions;
    private Integer maxRedemptionPerUser;
    private Integer totalRedemptionLimit;
    private String promotionTagCode;
    private String promotionTagName;
    private List<RewardDisplayData> rewards;
    private List<AccountQuotaData> accountQuotas;

    public String getUid()
    {
        return uid;
    }

    public void setUid(final String uid)
    {
        this.uid = uid;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(final String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public String getDisplayType()
    {
        return displayType;
    }

    public void setDisplayType(final String displayType)
    {
        this.displayType = displayType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public boolean isSuspended()
    {
        return suspended;
    }

    public void setSuspended(final boolean suspended)
    {
        this.suspended = suspended;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(final boolean active)
    {
        this.active = active;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(final Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(final Date endDate)
    {
        this.endDate = endDate;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(final int priority)
    {
        this.priority = priority;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(final String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getTermsAndConditions()
    {
        return termsAndConditions;
    }

    public void setTermsAndConditions(final String termsAndConditions)
    {
        this.termsAndConditions = termsAndConditions;
    }

    public Integer getMaxRedemptionPerUser()
    {
        return maxRedemptionPerUser;
    }

    public void setMaxRedemptionPerUser(final Integer maxRedemptionPerUser)
    {
        this.maxRedemptionPerUser = maxRedemptionPerUser;
    }

    public Integer getTotalRedemptionLimit()
    {
        return totalRedemptionLimit;
    }

    public void setTotalRedemptionLimit(final Integer totalRedemptionLimit)
    {
        this.totalRedemptionLimit = totalRedemptionLimit;
    }

    public String getPromotionTagCode()
    {
        return promotionTagCode;
    }

    public void setPromotionTagCode(final String promotionTagCode)
    {
        this.promotionTagCode = promotionTagCode;
    }

    public String getPromotionTagName()
    {
        return promotionTagName;
    }

    public void setPromotionTagName(final String promotionTagName)
    {
        this.promotionTagName = promotionTagName;
    }

    public List<RewardDisplayData> getRewards()
    {
        return rewards;
    }

    public void setRewards(final List<RewardDisplayData> rewards)
    {
        this.rewards = rewards;
    }

    public List<AccountQuotaData> getAccountQuotas()
    {
        return accountQuotas;
    }

    public void setAccountQuotas(final List<AccountQuotaData> accountQuotas)
    {
        this.accountQuotas = accountQuotas;
    }
}
