package com.is32.facades.populators;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.model.IS32PromotionTagModel;
import com.is32.core.model.IS32RewardModel;
import com.is32.core.service.IS32PromotionService;
import com.is32.facades.dto.PromotionDisplayData;
import com.is32.facades.dto.RewardDisplayData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IS32PromotionPopulator implements Populator<IS32PromotionModel, PromotionDisplayData>
{
    private static final Logger LOG = Logger.getLogger(IS32PromotionPopulator.class);

    private IS32PromotionService is32PromotionService;

    @Override
    public void populate(final IS32PromotionModel source, final PromotionDisplayData target)
            throws ConversionException
    {
        target.setUid(source.getUid());
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setStatus(source.getStatus() != null ? source.getStatus().getCode() : null);
        target.setSuspended(Boolean.TRUE.equals(source.getSuspended()));
        target.setActive(Boolean.TRUE.equals(source.getIsActive()));
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setPriority(source.getPriority() != null ? source.getPriority() : 0);
        target.setImageUrl(source.getImageUrl());
        target.setTermsAndConditions(source.getTermsAndConditions());
        target.setMaxRedemptionPerUser(source.getMaxRedemptionPerUser());
        target.setTotalRedemptionLimit(source.getTotalRedemptionLimit());

        populatePromotionTag(source, target);
        populateRewards(source, target);
    }

    protected void populatePromotionTag(final IS32PromotionModel source, final PromotionDisplayData target)
    {
        final IS32PromotionTagModel tag = source.getPromotionTag();
        if (tag != null)
        {
            target.setPromotionTagCode(tag.getCode());
            target.setPromotionTagName(tag.getName());
            target.setDisplayType(tag.getElabPromotionDisplayType() != null
                    ? tag.getElabPromotionDisplayType().getCode() : null);
        }
    }

    protected void populateRewards(final IS32PromotionModel source, final PromotionDisplayData target)
    {
        final Collection<IS32RewardModel> rewards = source.getRewards();
        if (rewards == null || rewards.isEmpty())
        {
            return;
        }

        final List<RewardDisplayData> rewardDataList = new ArrayList<>();

        for (final IS32RewardModel reward : rewards)
        {
            final RewardDisplayData rewardData = new RewardDisplayData();
            rewardData.setRewardType(reward.getRewardType() != null
                    ? reward.getRewardType().getCode() : null);
            rewardData.setRewardDescription(reward.getRewardDescription());
            rewardData.setRewardValue(reward.getRewardValue());
            rewardData.setAccountId(reward.getIncreaseMemberAccountId());
            rewardData.setMinSpend(reward.getMinSpend());
            rewardData.setMaxRewardCap(reward.getMaxRewardCap());
            rewardDataList.add(rewardData);
        }

        target.setRewards(rewardDataList);
    }

    @Required
    public void setIs32PromotionService(final IS32PromotionService is32PromotionService)
    {
        this.is32PromotionService = is32PromotionService;
    }
}
