package com.emergency.service;

/**
 * The six logical services present in the production topology.
 * Phase 2 will allow changing these states via chaos injection.
 */
public enum LogicalService {
    API_GATEWAY,
    AUTH,
    ORDERS,
    PAYMENT,
    DATABASE,
    NOTIFICATION
}
