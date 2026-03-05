package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32MemberProfileDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32MemberProfileDao.class);

    private static final String FIND_MEMBERS_BY_PK_LIST =
            "SELECT * FROM {IS32MemberProfile} WHERE {pk} IN (?memberPks)";

    private static final String FIND_MEMBERS_BY_LOYALTY_CARD_IDS =
            "SELECT {pk}, {firstName}, {lastName}, {email}, {phone}, {loyaltyCardId}, {tier}, " +
            "{pointBalance}, {registrationDate}, {lastActivityDate}, {preferredStore}, {status} " +
            "FROM {IS32MemberProfile} WHERE {loyaltyCardId} IN (?cardIds)";

    private static final String FIND_ACTIVE_MEMBER_BY_EMAIL =
            "SELECT {pk} FROM {IS32MemberProfile} " +
            "WHERE {email} = ?email " +
            "AND {status} = ?activeStatus";

    private static final String FIND_MEMBERS_BY_TIER_AND_STORE =
            "SELECT {mp.pk} FROM {IS32MemberProfile AS mp " +
            "JOIN IS32LoyaltyCard AS lc ON {lc.memberId} = {mp.memberId}} " +
            "WHERE {mp.tier} = ?tier " +
            "AND {mp.preferredStore} = ?store " +
            "AND {lc.status} = ?cardStatus " +
            "ORDER BY {mp.lastActivityDate} DESC";

    private FlexibleSearchService flexibleSearchService;

    public List<Object> findMembersByPKs(final List<Object> memberPks)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("memberPks", memberPks);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_MEMBERS_BY_PK_LIST, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public List<Object> findMembersByLoyaltyCardIds(final List<String> cardIds)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("cardIds", cardIds);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_MEMBERS_BY_LOYALTY_CARD_IDS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public Object findActiveMemberByEmail(final String email)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("activeStatus", "ACTIVE");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ACTIVE_MEMBER_BY_EMAIL, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    public List<Object> findMembersByTierAndStore(final String tier, final Object store)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("tier", tier);
        params.put("store", store);
        params.put("cardStatus", "ACTIVE");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_MEMBERS_BY_TIER_AND_STORE, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
