package com.teams_tracking_system.integrations.dtos;

public enum ExternalCheckInType {
    CHECKIN,
    CHECKOUT,
    VISIT_COMPLETED,
    STOP_DETECTED,
    STOP_ENDED,
    SIGNAL_LOST,
    SIGNAL_RESTORED,
    LOW_BATTERY,
    GEOFENCE_ENTER,
    GEOFENCE_EXIT
}
