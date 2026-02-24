package com.is32.core.service.impl;

import com.is32.core.dao.IS32RewardDao;
import com.is32.core.model.IS32RewardModel;
import com.is32.core.enums.IS32RewardType;
import com.is32.core.service.IS32RewardService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

public class DefaultIS32RewardService implements IS32RewardService
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32RewardService.class);

    private IS32RewardDao is32RewardDao;
    private ModelService modelService;

    @Override
    public List<IS32RewardModel> getRewardsForPromotion(final String promotionUid)
    {
        return is32RewardDao.findByPromotionUid(promotionUid);
    }

    @Override
    public List<IS32RewardModel> getRewardsByType(final IS32RewardType rewardType)
    {
        return is32RewardDao.findByRewardType(rewardType);
    }

    @Override
    public List<IS32RewardModel> getRewardsForAccount(final String accountId)
    {
        return is32RewardDao.findByAccountId(accountId);
    }

    @Override
    public IS32RewardModel getRewardByPromotionAndType(final String promotionUid, final IS32RewardType rewardType)
    {
        return is32RewardDao.findByPromotionUidAndRewardType(promotionUid, rewardType);
    }

    @Override
    public void saveReward(final IS32RewardModel reward)
    {
        modelService.save(reward);
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Saved reward for promotion [" + reward.getPromotionUid()
                    + "] of type [" + reward.getRewardType() + "]");
        }
    }

    @Override
    public void removeReward(final IS32RewardModel reward)
    {
        LOG.info("Removing reward for promotion [" + reward.getPromotionUid() + "]");
        modelService.remove(reward);
    }

    @Required
    public void setIs32RewardDao(final IS32RewardDao is32RewardDao)
    {
        this.is32RewardDao = is32RewardDao;
    }

    @Required
    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
