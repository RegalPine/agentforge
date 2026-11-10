package io.github.regalpine.agentforge.observability.alert;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Alert manager.
 * AF-ENHANCE-06 §11: collects and
 * dispatches alerts.
 */
public final class AlertManager {

    private final List<AlertEvent> alerts =
        new CopyOnWriteArrayList<>();

    private final List<AlertHandler> handlers =
        new CopyOnWriteArrayList<>();

    /**
     * Register an alert handler.
     */
    public AlertManager addHandler(
        AlertHandler handler
    ) {
        handlers.add(
            Objects.requireNonNull(handler)
        );
        return this;
    }

    /**
     * Fire an alert.
     */
    public void fire(AlertEvent alert) {
        alerts.add(alert);
        for (AlertHandler h : handlers) {
            try {
                h.handle(alert);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Get all alerts.
     */
    public List<AlertEvent> alerts() {
        return List.copyOf(alerts);
    }

    /**
     * Get alerts by severity.
     */
    public List<AlertEvent> alertsBySeverity(
        AlertSeverity severity
    ) {
        return alerts.stream()
            .filter(a -> a.severity() == severity)
            .toList();
    }

    /**
     * Get critical alerts.
     */
    public List<AlertEvent> criticalAlerts() {
        return alertsBySeverity(
            AlertSeverity.CRITICAL
        );
    }

    /**
     * Get alert count.
     */
    public int count() {
        return alerts.size();
    }

    /**
     * Clear all alerts.
     */
    public void clear() {
        alerts.clear();
    }

    /**
     * Alert handler SPI.
     */
    public interface AlertHandler {
        void handle(AlertEvent alert);
    }
}
