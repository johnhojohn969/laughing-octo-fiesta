package com.is32.core.dao.impl;

import com.is32.core.dao.IS32RewardDao;
import com.is32.core.model.IS32RewardModel;
import com.is32.core.enums.IS32RewardType;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32RewardDao implements IS32RewardDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32RewardDao.class);

    private static final String FIND_BY_PROMOTION_UID =
            "SELECT {r." + IS32RewardModel.PK + "} " +
            "FROM {" + IS32RewardModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32RewardModel.PROMOTIONUID + "} = ?promotionUid";

    private static final String FIND_BY_REWARD_TYPE =
            "SELECT {r." + IS32RewardModel.PK + "} " +
            "FROM {" + IS32RewardModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32RewardModel.REWARDTYPE + "} = ?rewardType";

    private static final String FIND_BY_ACCOUNT_ID =
            "SELECT {r." + IS32RewardModel.PK + "} " +
            "FROM {" + IS32RewardModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32RewardModel.INCREASEMEMBERACCOUNTID + "} = ?accountId";

    private static final String FIND_BY_UID_AND_TYPE =
            "SELECT {r." + IS32RewardModel.PK + "} " +
            "FROM {" + IS32RewardModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32RewardModel.PROMOTIONUID + "} = ?promotionUid " +
            "AND {r." + IS32RewardModel.REWARDTYPE + "} = ?rewardType";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<IS32RewardModel> findByPromotionUid(final String promotionUid)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("promotionUid", promotionUid);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_PROMOTION_UID, params);
        final SearchResult<IS32RewardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32RewardModel> findByRewardType(final IS32RewardType rewardType)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("rewardType", rewardType);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_REWARD_TYPE, params);
        final SearchResult<IS32RewardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32RewardModel> findByAccountId(final String accountId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_ACCOUNT_ID, params);
        final SearchResult<IS32RewardModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public IS32RewardModel findByPromotionUidAndRewardType(final String promotionUid, final IS32RewardType rewardType)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("promotionUid", promotionUid);
        params.put("rewardType", rewardType);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_UID_AND_TYPE, params);
        query.setResultClassList(Collections.singletonList(IS32RewardModel.class));
        final SearchResult<IS32RewardModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
