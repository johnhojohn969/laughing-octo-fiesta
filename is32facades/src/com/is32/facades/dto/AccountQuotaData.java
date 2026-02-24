package com.is32.facades.dto;

import java.io.Serializable;

public class AccountQuotaData implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String accountId;
    private String siebelAcctId;
    private int threshold;
    private int orderedAmt;
    private int remainingQuota;

    public String getAccountId()
    {
        return accountId;
    }

    public void setAccountId(final String accountId)
    {
        this.accountId = accountId;
    }

    public String getSiebelAcctId()
    {
        return siebelAcctId;
    }

    public void setSiebelAcctId(final String siebelAcctId)
    {
        this.siebelAcctId = siebelAcctId;
    }

    public int getThreshold()
    {
        return threshold;
    }

    public void setThreshold(final int threshold)
    {
        this.threshold = threshold;
    }

    public int getOrderedAmt()
    {
        return orderedAmt;
    }

    public void setOrderedAmt(final int orderedAmt)
    {
        this.orderedAmt = orderedAmt;
    }

    public int getRemainingQuota()
    {
        return remainingQuota;
    }

    public void setRemainingQuota(final int remainingQuota)
    {
        this.remainingQuota = remainingQuota;
    }
}
