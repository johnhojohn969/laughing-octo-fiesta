package com.is32.core.constants;

public final class IS32CoreConstants
{
    public static final String EXTENSIONNAME = "is32core";

    public static final String PLATFORM_LOGO_CODE = "is32corePlatformLogo";

    public static final class Promotion
    {
        public static final String DEFAULT_STATUS = "DRAFT";
        public static final int DEFAULT_PRIORITY = 0;
        public static final int DEFAULT_MAX_RESULTS = 100;

        private Promotion() {}
    }

    public static final class FlexibleSearch
    {
        public static final String PARAM_STATUS = "status";
        public static final String PARAM_CURRENT_DATE = "currentDate";
        public static final String PARAM_CATALOG_VERSION = "catalogVersion";
        public static final String PARAM_USER_PK = "userPk";
        public static final String PARAM_DISPLAY_TYPE = "displayType";
        public static final String PARAM_REWARD_TYPE = "rewardType";
        public static final String PARAM_SUSPENDED = "suspended";

        private FlexibleSearch() {}
    }

    public static final class Config
    {
        public static final String CLEANUP_DAYS = "is32core.promotion.cleanup.days";
        public static final String CLEANUP_BATCH_SIZE = "is32core.promotion.cleanup.batchsize";
        public static final String EVALUATION_CACHE_ENABLED = "is32core.promotion.evaluation.cache.enabled";
        public static final String ACCOUNT_QUOTA_QUERY_TIMEOUT = "is32core.accountquota.query.timeout";
        public static final String ACCOUNT_QUOTA_MAX_RESULTS = "is32core.accountquota.max.results";

        private Config() {}
    }

    private IS32CoreConstants()
    {
        // private constructor to prevent instantiation
    }
}
