package com.is32.core.strategies;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.model.IS32RewardModel;
import com.is32.core.model.EStampTierModel;
import com.is32.core.enums.IS32RewardType;
import com.is32.core.service.IS32AccountQuotaService;
import com.is32.core.service.IS32PromotionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Strategy for evaluating IS32 promotion eligibility. Determines whether a customer
 * qualifies for a promotion based on their account quota, e-stamp tier, and
 * promotion activity constraints.
 */
public class IS32PromotionEvaluationStrategy
{
    private static final Logger LOG = Logger.getLogger(IS32PromotionEvaluationStrategy.class);

    private static final String CONFIG_MAX_EVALUATION_RESULTS = "is32core.promotion.evaluation.max.results";
    private static final int DEFAULT_MAX_RESULTS = 100;

    private IS32PromotionService is32PromotionService;
    private IS32AccountQuotaService is32AccountQuotaService;
    private ModelService modelService;
    private ConfigurationService configurationService;

    /**
     * Evaluates which active promotions a customer is eligible for based on their
     * account quotas and redemption history.
     *
     * @param customer       the customer to evaluate
     * @param catalogVersion the catalog version context
     * @return list of eligible promotions
     */
    public List<IS32PromotionModel> evaluateEligiblePromotions(final CustomerModel customer,
                                                                final CatalogVersionModel catalogVersion)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Evaluating eligible promotions for customer [" + customer.getUid() + "]");
        }

        final List<IS32PromotionModel> eligiblePromotions = new ArrayList<>();

        final List<Map<String, Object>> accountQuotas =
                is32AccountQuotaService.getAccountQuotas(customer, catalogVersion);

        final List<IS32PromotionModel> activePromotions =
                is32PromotionService.getActiveNonSuspendedPromotions();

        for (final IS32PromotionModel promotion : activePromotions)
        {
            if (isPromotionEligible(promotion, accountQuotas, customer))
            {
                eligiblePromotions.add(promotion);

                final int maxResults = configurationService.getConfiguration()
                        .getInt(CONFIG_MAX_EVALUATION_RESULTS, DEFAULT_MAX_RESULTS);

                if (eligiblePromotions.size() >= maxResults)
                {
                    LOG.info("Reached maximum evaluation results limit of [" + maxResults + "]");
                    break;
                }
            }
        }

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Found [" + eligiblePromotions.size() + "] eligible promotions for customer ["
                    + customer.getUid() + "]");
        }

        return eligiblePromotions;
    }

    /**
     * Checks whether a specific promotion is eligible for the customer based on
     * reward configuration and account quota thresholds.
     */
    protected boolean isPromotionEligible(final IS32PromotionModel promotion,
                                          final List<Map<String, Object>> accountQuotas,
                                          final CustomerModel customer)
    {
        final Collection<IS32RewardModel> rewards = promotion.getRewards();
        if (rewards == null || rewards.isEmpty())
        {
            return false;
        }

        for (final IS32RewardModel reward : rewards)
        {
            if (IS32RewardType.INCREASE_MEMBER_ACCOUNT.equals(reward.getRewardType()))
            {
                final String accountId = reward.getIncreaseMemberAccountId();
                if (accountId == null)
                {
                    continue;
                }

                for (final Map<String, Object> quota : accountQuotas)
                {
                    if (accountId.equals(quota.get("accountId")))
                    {
                        final int threshold = (Integer) quota.get("threshold");
                        final int orderedAmt = (Integer) quota.get("orderedAmt");

                        if (orderedAmt < threshold)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        // Check redemption limits
        if (promotion.getMaxRedemptionPerUser() != null)
        {
            final int redemptionCount =
                    is32AccountQuotaService.getRedemptionCount(promotion.getUid(), customer);

            if (redemptionCount >= promotion.getMaxRedemptionPerUser())
            {
                return false;
            }
        }

        return !rewards.isEmpty();
    }

    @Required
    public void setIs32PromotionService(final IS32PromotionService is32PromotionService)
    {
        this.is32PromotionService = is32PromotionService;
    }

    @Required
    public void setIs32AccountQuotaService(final IS32AccountQuotaService is32AccountQuotaService)
    {
        this.is32AccountQuotaService = is32AccountQuotaService;
    }

    @Required
    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }

    @Required
    public void setConfigurationService(final ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }
}
