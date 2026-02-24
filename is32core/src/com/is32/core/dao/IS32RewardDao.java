package com.is32.core.dao;

import com.is32.core.model.IS32RewardModel;
import com.is32.core.enums.IS32RewardType;

import java.util.List;

public interface IS32RewardDao
{
    List<IS32RewardModel> findByPromotionUid(String promotionUid);

    List<IS32RewardModel> findByRewardType(IS32RewardType rewardType);

    List<IS32RewardModel> findByAccountId(String accountId);

    IS32RewardModel findByPromotionUidAndRewardType(String promotionUid, IS32RewardType rewardType);
}
