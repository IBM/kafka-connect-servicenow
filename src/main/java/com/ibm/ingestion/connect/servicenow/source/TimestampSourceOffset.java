package com.ibm.ingestion.connect.servicenow.source;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TimestampSourceOffset {

    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String LAST_READ_IDENTIFIER = "last_identifier";
    private Instant _lastSeenTimestamp;
    private String _lastReadIdentifier;

    public TimestampSourceOffset(Instant lastSeenTimestamp, String lastReadIdentifier) {
        this._lastSeenTimestamp = lastSeenTimestamp;
        this._lastReadIdentifier = lastReadIdentifier;
    }

    public static TimestampSourceOffset fromMap(Map<String, Object> offsetMap) {
        if (offsetMap == null || offsetMap.isEmpty()) {
            return new TimestampSourceOffset(null, null);
        }

        long lastSeenTimestampSeconds = (long)offsetMap.get(TIMESTAMP_KEY);
        String lastReadIdentifier = (String)offsetMap.get(LAST_READ_IDENTIFIER);
        return new TimestampSourceOffset(Instant.ofEpochSecond(lastSeenTimestampSeconds), lastReadIdentifier);
    }

    public Map<String, Object> toMap() {
        return toMap(this._lastSeenTimestamp, this._lastReadIdentifier);
    }

    public static Map<String, Object> toMap(Instant lastSeenTimestamp, String lastReadIdentifier) {
        Map<String, Object> offsetMap = new HashMap<String, Object>();
        offsetMap.put(TIMESTAMP_KEY, lastSeenTimestamp.getEpochSecond());
        offsetMap.put(LAST_READ_IDENTIFIER, lastReadIdentifier);
        return offsetMap;
    }

    public Instant getLastSeenTimestamp() {
        return this._lastSeenTimestamp;
    }

    public String getLastReadIdentifier() {

        return this._lastReadIdentifier;
    }

    public void updateOffset(Instant lastSeenTimestamp, String lastReadIdentifier) {
        this._lastSeenTimestamp = lastSeenTimestamp;
        this._lastReadIdentifier = lastReadIdentifier;
    }

    public void updateLastReadIdentifier(String lastReadIdentifier) {
        this._lastReadIdentifier = lastReadIdentifier;
    }
}
