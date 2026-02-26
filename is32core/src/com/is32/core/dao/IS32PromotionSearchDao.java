package com.is32.core.dao;

import com.is32.core.model.IS32PromotionModel;

import java.util.Date;
import java.util.List;

public interface IS32PromotionSearchDao
{
    List<IS32PromotionModel> findByPriority(String priority);

    List<IS32PromotionModel> findByUidNumeric(long promotionId);

    List<IS32PromotionModel> findByMaxRedemption(String maxRedemption);

    List<IS32PromotionModel> findByTotalRedemptionLimit(String limit);

    List<IS32PromotionModel> findByPriorityRange(String minPriority, String maxPriority);

    List<IS32PromotionModel> findPromotionsWithRewardValue(String minRewardValue);

    List<List<Object>> getPromotionSummaryByTag(int tagCode);
}
