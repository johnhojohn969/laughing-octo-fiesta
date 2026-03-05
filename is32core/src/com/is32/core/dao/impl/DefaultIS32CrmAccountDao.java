package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultIS32CrmAccountDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32CrmAccountDao.class);

    // FSQL-03 violation: SELECT * with dynamic IN clause
    private static final String FIND_ACCOUNTS_BY_PKS =
            "SELECT * FROM {CrmAccount} WHERE {pk} IN (?pkList)";

    // FSQL-03 violation: SELECT all fields with large IN clause
    private static final String FIND_ACCOUNTS_BY_CODES =
            "SELECT {pk}, {code}, {name}, {email}, {phone}, {address}, {city}, {state}, {country}, {postalCode}, " +
            "{createdDate}, {modifiedDate}, {status}, {accountType}, {parentAccount} " +
            "FROM {CrmAccount} WHERE {code} IN (?codeList)";

    // FSQL-01, FSQL-05 violation: JOIN + EXISTS subquery combined
    private static final String FIND_ACCOUNTS_WITH_ORDERS =
            "SELECT {a.pk} FROM {CrmAccount AS a JOIN Order AS o ON {o.account} = {a.pk}} " +
            "WHERE {a.status} = ?activeStatus " +
            "AND EXISTS ({{ SELECT {oe.pk} FROM {OrderEntry AS oe} WHERE {oe.order} = {o.pk} AND {oe.quantity} > ?minQuantity }})";

    private FlexibleSearchService flexibleSearchService;

    public List<Object> findAccountsByPKs(final List<Object> pkList)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("pkList", pkList);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ACCOUNTS_BY_PKS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public List<Object> findAccountsByCodes(final List<String> codes)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("codeList", codes);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ACCOUNTS_BY_CODES, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public List<Object> findAccountsWithOrders(final String activeStatus, final int minQuantity)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("activeStatus", activeStatus);
        params.put("minQuantity", minQuantity);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ACCOUNTS_WITH_ORDERS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
