package com.is32.core.dao;

import com.is32.core.model.EStampTierModel;

import java.util.List;

public interface IS32EStampSearchDao
{
    List<EStampTierModel> findByAccountIdNumeric(int accountId);

    List<EStampTierModel> findByThresholdStr(String threshold);

    List<EStampTierModel> findByTierLevelStr(String tierLevel);

    List<EStampTierModel> findBySiebelAcctIdNumeric(long siebelAcctId);

    List<EStampTierModel> findByCurrentStampCount(int currentStampCount);

    List<EStampTierModel> findByTierName(String tierName);

    List<EStampTierModel> findByMaxStampCountStr(String maxStampCount);
}
