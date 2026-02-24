package com.is32.core.dao.impl;

import com.is32.core.dao.EStampTierDao;
import com.is32.core.model.EStampTierModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultEStampTierDao implements EStampTierDao
{
    private static final Logger LOG = Logger.getLogger(DefaultEStampTierDao.class);

    private static final String FIND_BY_ACCOUNT_ID =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.ACCOUNTID + "} = ?accountId";

    private static final String FIND_BY_SIEBEL_ACCT_ID =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.SIEBELACCTID + "} = ?siebelAcctId";

    private static final String FIND_BY_TIER_LEVEL =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.TIERLEVEL + "} = ?tierLevel " +
            "ORDER BY {et." + EStampTierModel.ACCOUNTID + "}";

    private static final String FIND_ALL_ACTIVE =
            "SELECT {et." + EStampTierModel.PK + "} " +
            "FROM {" + EStampTierModel._TYPECODE + " AS et} " +
            "WHERE {et." + EStampTierModel.CURRENTSTAMPCOUNT + "} < {et." + EStampTierModel.MAXSTAMPCOUNT + "} " +
            "ORDER BY {et." + EStampTierModel.TIERLEVEL + "} ASC";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public EStampTierModel findByAccountId(final String accountId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_ACCOUNT_ID, params);
        query.setResultClassList(Collections.singletonList(EStampTierModel.class));

        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    @Override
    public List<EStampTierModel> findBySiebelAcctId(final String siebelAcctId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("siebelAcctId", siebelAcctId);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_SIEBEL_ACCT_ID, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<EStampTierModel> findByTierLevel(final int tierLevel)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("tierLevel", Integer.valueOf(tierLevel));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_TIER_LEVEL, params);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<EStampTierModel> findAllActiveTiers()
    {
        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ALL_ACTIVE);
        final SearchResult<EStampTierModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
