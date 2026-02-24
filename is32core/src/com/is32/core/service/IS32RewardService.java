package com.is32.core.service;

import com.is32.core.model.IS32RewardModel;
import com.is32.core.enums.IS32RewardType;

import java.util.List;

public interface IS32RewardService
{
    List<IS32RewardModel> getRewardsForPromotion(String promotionUid);

    List<IS32RewardModel> getRewardsByType(IS32RewardType rewardType);

    List<IS32RewardModel> getRewardsForAccount(String accountId);

    IS32RewardModel getRewardByPromotionAndType(String promotionUid, IS32RewardType rewardType);

    void saveReward(IS32RewardModel reward);

    void removeReward(IS32RewardModel reward);
}
