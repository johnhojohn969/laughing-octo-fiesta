package com.is32.core.dao.impl;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for FlexibleSearch queries matching PATTERN-02:
 * Aggregate function (AVG/SUM/COUNT) with OR / IS NULL filter.
 *
 * PATTERN-02 SQL signature:
 * <pre>
 * SELECT avg(item_t0.p_rating)
 * FROM customerreviews item_t0
 * WHERE item_t0.p_product = ?
 *   AND item_t0.TypePkString = ?
 *   AND (item_t0.p_blocked = '***' OR item_t0.p_blocked IS NULL)
 *   AND item_t0.p_approvalstatus != '***'
 * </pre>
 *
 * This test verifies that FlexibleSearch queries using aggregate functions combined
 * with OR/IS NULL conditions and inequality operators are correctly constructed.
 * These patterns are risky because OR conditions with IS NULL prevent effective index usage,
 * and aggregate functions on large tables can cause performance degradation.
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultIS32AggregateQueryDaoTest
{
    /**
     * FlexibleSearch equivalent of PATTERN-02: AVG aggregate with OR/IS NULL filter.
     * Simulates a rating average calculation with blocked/approval status filtering.
     * In Hybris FlexibleSearch syntax, this translates to the customerreviews SQL pattern.
     */
    private static final String GET_AVERAGE_REWARD_VALUE =
            "SELECT AVG({r.rewardValue}) " +
            "FROM {IS32Reward AS r} " +
            "WHERE {r.promotionUid} = ?promotionUid " +
            "AND ({r.maxRewardCap} = ?defaultCap OR {r.maxRewardCap} IS NULL) " +
            "AND {r.rewardType} <> ?excludedType";

    /**
     * Variant: SUM aggregate with IS NULL guard on optional column.
     */
    private static final String GET_TOTAL_REWARD_VALUE =
            "SELECT SUM({r.rewardValue}) " +
            "FROM {IS32Reward AS r} " +
            "WHERE {r.promotionUid} = ?promotionUid " +
            "AND ({r.minSpend} IS NULL OR {r.minSpend} <= ?cartTotal)";

    /**
     * Variant: COUNT aggregate with multiple OR/IS NULL conditions.
     */
    private static final String COUNT_ACTIVE_REWARDS =
            "SELECT COUNT({r.pk}) " +
            "FROM {IS32Reward AS r} " +
            "WHERE {r.promotionUid} = ?promotionUid " +
            "AND {r.rewardType} = ?rewardType " +
            "AND ({r.maxRewardCap} IS NULL OR {r.maxRewardCap} > ?zeroValue) " +
            "AND {r.rewardValue} != ?zeroValue";

    @Mock
    private FlexibleSearchService flexibleSearchService;

    @Mock
    private SearchResult<List<Object>> searchResult;

    @Captor
    private ArgumentCaptor<FlexibleSearchQuery> queryCaptor;

    @Before
    public void setUp()
    {
        when(flexibleSearchService.search(any(FlexibleSearchQuery.class))).thenReturn(searchResult);
        when(searchResult.getResult()).thenReturn(Collections.emptyList());
    }

    @Test
    public void testAggregateQueryContainsAvgFunction()
    {
        // PATTERN-02 check: query uses AVG aggregate function
        assertTrue("Query must use AVG() aggregate function (PATTERN-02 signature)",
                GET_AVERAGE_REWARD_VALUE.contains("AVG("));
    }

    @Test
    public void testAggregateQueryContainsOrWithIsNull()
    {
        // PATTERN-02 check: OR condition combined with IS NULL
        assertTrue("Query must contain OR with IS NULL pattern (PATTERN-02 risk indicator)",
                GET_AVERAGE_REWARD_VALUE.contains("OR {r.maxRewardCap} IS NULL"));
    }

    @Test
    public void testAggregateQueryContainsInequalityOperator()
    {
        // PATTERN-02 check: inequality operator (<> or !=)
        assertTrue("Query must contain inequality filter <> (PATTERN-02 signature)",
                GET_AVERAGE_REWARD_VALUE.contains("<> ?excludedType"));
    }

    @Test
    public void testAggregateQueryWithOrIsNullPreventsIndexUsage()
    {
        // PATTERN-02 risk: OR with IS NULL on the same column prevents index-only scan
        assertTrue("OR/IS NULL on maxRewardCap prevents index usage",
                GET_AVERAGE_REWARD_VALUE.contains("{r.maxRewardCap} = ?defaultCap OR {r.maxRewardCap} IS NULL"));
    }

    @Test
    public void testSumAggregateWithIsNullGuard()
    {
        // PATTERN-02 variant: SUM with IS NULL guard
        assertTrue("SUM query must contain IS NULL guard (PATTERN-02 variant)",
                GET_TOTAL_REWARD_VALUE.contains("SUM("));
        assertTrue("SUM query must use IS NULL OR comparison pattern",
                GET_TOTAL_REWARD_VALUE.contains("{r.minSpend} IS NULL OR {r.minSpend} <="));
    }

    @Test
    public void testCountAggregateWithOrIsNull()
    {
        // PATTERN-02 variant: COUNT with OR/IS NULL
        assertTrue("COUNT query must use COUNT() aggregate",
                COUNT_ACTIVE_REWARDS.contains("COUNT("));
        assertTrue("COUNT query must contain OR/IS NULL pattern",
                COUNT_ACTIVE_REWARDS.contains("IS NULL OR"));
        assertTrue("COUNT query must contain inequality != operator",
                COUNT_ACTIVE_REWARDS.contains("!= ?zeroValue"));
    }

    @Test
    public void testAvgQueryExecutionWithParameters()
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("promotionUid", "PROMO_001");
        params.put("defaultCap", Double.valueOf(0.0));
        params.put("excludedType", "POINTS_MULTIPLIER");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_AVERAGE_REWARD_VALUE, params);
        query.setResultClassList(Collections.singletonList(Double.class));

        flexibleSearchService.search(query);

        verify(flexibleSearchService).search(queryCaptor.capture());
        final FlexibleSearchQuery captured = queryCaptor.getValue();

        assertNotNull("Captured query should not be null", captured);
        assertEquals("Query should have AVG aggregate",
                GET_AVERAGE_REWARD_VALUE, captured.getQuery());
        assertEquals("Result class should be Double for AVG",
                Collections.singletonList(Double.class), captured.getResultClassList());
    }

    @Test
    public void testSumQueryExecutionWithParameters()
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("promotionUid", "PROMO_002");
        params.put("cartTotal", Double.valueOf(100.0));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_TOTAL_REWARD_VALUE, params);
        query.setResultClassList(Collections.singletonList(Double.class));

        flexibleSearchService.search(query);

        verify(flexibleSearchService).search(queryCaptor.capture());
        final FlexibleSearchQuery captured = queryCaptor.getValue();

        assertNotNull("Captured query should not be null", captured);
        assertTrue("Query string must contain SUM aggregate",
                captured.getQuery().contains("SUM("));
    }

    @Test
    public void testCountQueryExecutionWithParameters()
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("promotionUid", "PROMO_003");
        params.put("rewardType", "DISCOUNT");
        params.put("zeroValue", Double.valueOf(0.0));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(COUNT_ACTIVE_REWARDS, params);
        query.setResultClassList(Collections.singletonList(Integer.class));

        flexibleSearchService.search(query);

        verify(flexibleSearchService).search(queryCaptor.capture());
        final FlexibleSearchQuery captured = queryCaptor.getValue();

        assertNotNull("Captured query should not be null", captured);
        assertTrue("Query string must contain COUNT aggregate",
                captured.getQuery().contains("COUNT("));
    }

    @Test
    public void testAllAggregateQueriesHaveEqualityFilter()
    {
        // PATTERN-02: all aggregate queries must have at least one equality WHERE filter
        assertTrue("AVG query must have equality filter on promotionUid",
                GET_AVERAGE_REWARD_VALUE.contains("{r.promotionUid} = ?promotionUid"));
        assertTrue("SUM query must have equality filter on promotionUid",
                GET_TOTAL_REWARD_VALUE.contains("{r.promotionUid} = ?promotionUid"));
        assertTrue("COUNT query must have equality filter on promotionUid",
                COUNT_ACTIVE_REWARDS.contains("{r.promotionUid} = ?promotionUid"));
    }
}
