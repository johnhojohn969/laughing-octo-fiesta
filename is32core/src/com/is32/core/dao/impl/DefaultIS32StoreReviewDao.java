package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo DAO for Gemini SQL-05 pattern detection.
 * All queries in this class structurally match the SQL-05 pattern from styleguide.md Section 5.1:
 * multi-JOIN queries on CustomerReview joined with Product, Customer, and BaseStore.
 */
public class DefaultIS32StoreReviewDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32StoreReviewDao.class);

    // SQL-05 match: 3-table JOIN — CustomerReview → Product, Customer, BaseStore
    private static final String FIND_REVIEWS_BY_STORE =
            "SELECT {cr.pk}, {cr.rating}, {p.code}, {c.uid}, {bs.uid} " +
            "FROM {CustomerReview AS cr " +
            "JOIN Product AS p ON {cr.product} = {p.pk} " +
            "JOIN Customer AS c ON {cr.user} = {c.pk} " +
            "JOIN BaseStore AS bs ON {cr.baseStore} = {bs.pk}} " +
            "WHERE {cr.approvalStatus} = ?approvalStatus " +
            "AND ({cr.blocked} = ?blocked OR {cr.blocked} IS NULL) " +
            "AND {bs.uid} = ?storeUid " +
            "AND {cr.creationtime} >= ?sinceDate " +
            "ORDER BY {cr.creationtime} DESC";

    // SQL-05 match: same 3-table JOIN structure with AVG aggregate
    private static final String AVG_RATING_BY_STORE_AND_PRODUCT =
            "SELECT {p.code}, avg({cr.rating}) " +
            "FROM {CustomerReview AS cr " +
            "JOIN Product AS p ON {cr.product} = {p.pk} " +
            "JOIN Customer AS c ON {cr.user} = {c.pk} " +
            "JOIN BaseStore AS bs ON {cr.baseStore} = {bs.pk}} " +
            "WHERE {cr.approvalStatus} = ?approvalStatus " +
            "AND ({cr.blocked} = ?blocked OR {cr.blocked} IS NULL) " +
            "AND {bs.uid} = ?storeUid " +
            "GROUP BY {p.code}";

    // SQL-05 match: same 3-table JOIN with COUNT and date range filter
    private static final String COUNT_REVIEWS_BY_STORE_CUSTOMER =
            "SELECT {c.uid}, COUNT({cr.pk}) " +
            "FROM {CustomerReview AS cr " +
            "JOIN Product AS p ON {cr.product} = {p.pk} " +
            "JOIN Customer AS c ON {cr.user} = {c.pk} " +
            "JOIN BaseStore AS bs ON {cr.baseStore} = {bs.pk}} " +
            "WHERE {bs.uid} = ?storeUid " +
            "AND {cr.approvalStatus} = ?approvalStatus " +
            "AND {cr.creationtime} >= ?startDate " +
            "AND {cr.creationtime} <= ?endDate " +
            "GROUP BY {c.uid} " +
            "ORDER BY COUNT({cr.pk}) DESC";

    private FlexibleSearchService flexibleSearchService;

    public List<Object> findReviewsByStore(final String storeUid, final Date sinceDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("approvalStatus", "APPROVED");
        params.put("blocked", Boolean.FALSE);
        params.put("storeUid", storeUid);
        params.put("sinceDate", sinceDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_REVIEWS_BY_STORE, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public List<Object> getAverageRatingByStoreAndProduct(final String storeUid)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("approvalStatus", "APPROVED");
        params.put("blocked", Boolean.FALSE);
        params.put("storeUid", storeUid);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(AVG_RATING_BY_STORE_AND_PRODUCT, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public List<Object> countReviewsByStoreCustomer(final String storeUid, final Date startDate, final Date endDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("storeUid", storeUid);
        params.put("approvalStatus", "APPROVED");
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(COUNT_REVIEWS_BY_STORE_CUSTOMER, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
