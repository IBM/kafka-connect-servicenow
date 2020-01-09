package com.ibm.ingestion.connect.servicenow.source;

import org.apache.kafka.connect.source.SourceRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class SourceRecordsPage {

    private List<SourceRecord> _records;
    private LocalDateTime _nextTimeToPollUtc;

    public SourceRecordsPage(List<SourceRecord> records, LocalDateTime nextTimeToPollUtc) {
        this._records = records;
        this._nextTimeToPollUtc = nextTimeToPollUtc;
    }

    public List<SourceRecord> getRecords() {
        return this._records;
    }

    public LocalDateTime getNextTimeToPollUtc(){
        return this._nextTimeToPollUtc;
    }
}
