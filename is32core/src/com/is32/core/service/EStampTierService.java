package com.is32.core.service;

import com.is32.core.model.EStampTierModel;

import java.util.List;

public interface EStampTierService
{
    EStampTierModel getTierByAccountId(String accountId);

    List<EStampTierModel> getTiersBySiebelAcctId(String siebelAcctId);

    List<EStampTierModel> getTiersByLevel(int tierLevel);

    List<EStampTierModel> getActiveTiers();

    void incrementStampCount(String accountId, int count);

    void resetStampCount(String accountId);

    void saveTier(EStampTierModel tier);
}
