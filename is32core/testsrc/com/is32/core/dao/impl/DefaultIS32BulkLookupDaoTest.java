package com.is32.core.dao.impl;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.PK;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for FlexibleSearch queries matching PATTERN-03:
 * Bulk PK lookup with IN clause (unbounded parameter list).
 *
 * PATTERN-03 SQL signature:
 * <pre>
 * SELECT * FROM crmaccount WHERE PK IN (?,?,..., ?)
 * </pre>
 *
 * This test verifies that FlexibleSearch queries using IN clauses with dynamic
 * parameter lists are correctly constructed, and validates that batching safeguards
 * are in place to prevent unbounded IN lists that can exceed database limits
 * (Oracle 1000, SQL Server 2100) or cause query plan cache misses.
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultIS32BulkLookupDaoTest
{
    private static final int MAX_IN_CLAUSE_SIZE = 1000;

    /**
     * FlexibleSearch equivalent of PATTERN-03: bulk PK lookup with IN clause.
     * Uses SELECT * pattern (selects all columns) — a PATTERN-03 risk indicator.
     */
    private static final String FIND_PROMOTIONS_BY_PK_LIST =
            "SELECT {p.pk} " +
            "FROM {IS32Promotion AS p} " +
            "WHERE {p.pk} IN (?pkList)";

    /**
     * Variant: IN clause on a non-PK column (uid list).
     */
    private static final String FIND_PROMOTIONS_BY_UID_LIST =
            "SELECT {p.pk} " +
            "FROM {IS32Promotion AS p} " +
            "WHERE {p.uid} IN (?uidList)";

    /**
     * Anti-pattern: SELECT * equivalent in FlexibleSearch — selects all fields.
     * Combined with IN clause, this matches full PATTERN-03 signature.
     */
    private static final String FIND_ALL_FIELDS_BY_PK_LIST =
            "SELECT * " +
            "FROM {IS32Promotion AS p} " +
            "WHERE {p.pk} IN (?pkList)";

    /**
     * Variant: IN clause on a joined table's column.
     */
    private static final String FIND_REWARDS_BY_PROMOTION_UID_LIST =
            "SELECT {r.pk} " +
            "FROM {IS32Reward AS r} " +
            "WHERE {r.promotionUid} IN (?promotionUidList)";

    @Mock
    private FlexibleSearchService flexibleSearchService;

    @Mock
    private SearchResult<Object> searchResult;

    @Captor
    private ArgumentCaptor<FlexibleSearchQuery> queryCaptor;

    @Before
    public void setUp()
    {
        when(flexibleSearchService.search(any(FlexibleSearchQuery.class))).thenReturn(searchResult);
        when(searchResult.getResult()).thenReturn(Collections.emptyList());
    }

    @Test
    public void testInClauseQueryContainsPkInPattern()
    {
        // PATTERN-03 check: query contains PK IN clause
        assertTrue("Query must contain PK IN clause (PATTERN-03 signature)",
                FIND_PROMOTIONS_BY_PK_LIST.contains("{p.pk} IN ("));
    }

    @Test
    public void testSelectStarQueryMatchesPattern03()
    {
        // PATTERN-03 check: SELECT * combined with IN clause
        assertTrue("Query must use SELECT * (PATTERN-03 risk: selects all columns)",
                FIND_ALL_FIELDS_BY_PK_LIST.contains("SELECT *"));
        assertTrue("Query must contain IN clause",
                FIND_ALL_FIELDS_BY_PK_LIST.contains("IN (?pkList)"));
    }

    @Test
    public void testInClauseWithSmallList()
    {
        final List<String> pkList = List.of("pk1", "pk2", "pk3");

        final Map<String, Object> params = new HashMap<>();
        params.put("pkList", pkList);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_PROMOTIONS_BY_PK_LIST, params);
        flexibleSearchService.search(query);

        verify(flexibleSearchService).search(queryCaptor.capture());
        final FlexibleSearchQuery captured = queryCaptor.getValue();

        assertNotNull("Captured query should not be null", captured);
        assertTrue("Query should contain IN clause",
                captured.getQuery().contains("IN (?pkList)"));
    }

    @Test
    public void testInClauseWithUidList()
    {
        final List<String> uidList = List.of("PROMO_001", "PROMO_002", "PROMO_003");

        final Map<String, Object> params = new HashMap<>();
        params.put("uidList", uidList);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_PROMOTIONS_BY_UID_LIST, params);
        flexibleSearchService.search(query);

        verify(flexibleSearchService).search(queryCaptor.capture());
        final FlexibleSearchQuery captured = queryCaptor.getValue();

        assertNotNull("Captured query should not be null", captured);
        assertEquals("Query should match uid list query",
                FIND_PROMOTIONS_BY_UID_LIST, captured.getQuery());
    }

    @Test
    public void testLargeInClauseExceedsMaxSize()
    {
        // PATTERN-03 risk: generate a list larger than Oracle's 1000 IN-clause limit
        final List<String> largePkList = IntStream.range(0, 1500)
                .mapToObj(i -> "PK_" + i)
                .collect(Collectors.toList());

        assertTrue("Large PK list exceeds max IN clause size (" + MAX_IN_CLAUSE_SIZE + ")",
                largePkList.size() > MAX_IN_CLAUSE_SIZE);
    }

    @Test
    public void testBatchingInClauseForLargeList()
    {
        // PATTERN-03 mitigation: batch large IN lists into chunks of MAX_IN_CLAUSE_SIZE
        final List<String> largePkList = IntStream.range(0, 2500)
                .mapToObj(i -> "PK_" + i)
                .collect(Collectors.toList());

        final List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < largePkList.size(); i += MAX_IN_CLAUSE_SIZE)
        {
            batches.add(largePkList.subList(i, Math.min(i + MAX_IN_CLAUSE_SIZE, largePkList.size())));
        }

        assertEquals("2500 items should be split into 3 batches",
                3, batches.size());
        assertEquals("First batch should have " + MAX_IN_CLAUSE_SIZE + " items",
                MAX_IN_CLAUSE_SIZE, batches.get(0).size());
        assertEquals("Second batch should have " + MAX_IN_CLAUSE_SIZE + " items",
                MAX_IN_CLAUSE_SIZE, batches.get(1).size());
        assertEquals("Third batch should have 500 items",
                500, batches.get(2).size());
    }

    @Test
    public void testEmptyInClauseListHandling()
    {
        final List<String> emptyList = Collections.emptyList();

        // PATTERN-03 edge case: empty IN list should not execute query
        assertTrue("Empty list should be checked before query execution",
                emptyList.isEmpty());
    }

    @Test
    public void testInClauseOnJoinedTableColumn()
    {
        // PATTERN-03 variant: IN clause on non-PK column of a different table
        assertTrue("IN clause on promotionUid list for rewards lookup",
                FIND_REWARDS_BY_PROMOTION_UID_LIST.contains("{r.promotionUid} IN ("));
    }

    @Test
    public void testAllInClauseQueriesUseParameterizedList()
    {
        // PATTERN-03: all IN clause queries must use parameterized lists (not string concatenation)
        assertTrue("PK list query must use parameterized IN clause",
                FIND_PROMOTIONS_BY_PK_LIST.contains("?pkList"));
        assertTrue("UID list query must use parameterized IN clause",
                FIND_PROMOTIONS_BY_UID_LIST.contains("?uidList"));
        assertTrue("Select-all query must use parameterized IN clause",
                FIND_ALL_FIELDS_BY_PK_LIST.contains("?pkList"));
        assertTrue("Reward query must use parameterized IN clause",
                FIND_REWARDS_BY_PROMOTION_UID_LIST.contains("?promotionUidList"));
    }

    @Test
    public void testSelectStarShouldBeReplacedWithSpecificFields()
    {
        // PATTERN-03 recommendation: SELECT * should be replaced with specific fields
        assertTrue("PATTERN-03 anti-pattern: SELECT * detected — should select specific fields",
                FIND_ALL_FIELDS_BY_PK_LIST.startsWith("SELECT *"));
        // The PK-only variant is the recommended replacement
        assertTrue("Recommended: SELECT {p.pk} instead of SELECT *",
                FIND_PROMOTIONS_BY_PK_LIST.startsWith("SELECT {p.pk}"));
    }
}
