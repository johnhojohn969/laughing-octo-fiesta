package com.is32.core.dao;

import com.is32.core.model.EStampTierModel;

import java.util.List;

public interface EStampTierDao
{
    EStampTierModel findByAccountId(String accountId);

    List<EStampTierModel> findBySiebelAcctId(String siebelAcctId);

    List<EStampTierModel> findByTierLevel(int tierLevel);

    List<EStampTierModel> findAllActiveTiers();
}
