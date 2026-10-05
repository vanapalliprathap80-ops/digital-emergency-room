package com.emergency.domain;

public enum RemediationActionType {
    // Database
    INCREASE_DB_POOL,
    CLEAR_CONNECTION_POOL,
    RESTORE_DATABASE_AVAILABILITY,
    REDUCE_QUERY_LATENCY,

    // Payment
    RESTORE_PAYMENT_SERVICE,
    RESET_PAYMENT_TIMEOUT,

    // Deployment
    ROLLBACK_DEPLOYMENT,

    // Service-level
    RESTART_SERVICE,
    SCALE_SERVICE,

    // Unsafe / blocked
    UNSUPPORTED
}
