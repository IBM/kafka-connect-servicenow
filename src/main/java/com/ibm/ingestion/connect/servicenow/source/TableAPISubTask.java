package com.ibm.ingestion.connect.servicenow.source;

import com.ibm.ingestion.connect.servicenow.source.partitioner.IServiceNowTablePartitioner;
import com.ibm.ingestion.connect.servicenow.source.partitioner.PartitionerFactory;
import com.ibm.ingestion.connect.servicenow.util.Helpers;
import com.ibm.ingestion.http.ServiceNowTableApiClient;
import com.ibm.ingestion.http.TableAPIQueryBuilder;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.ibm.ingestion.connect.servicenow.util.Helpers.buildSchemaFromSimpleJsonRecord;
import static com.ibm.ingestion.connect.servicenow.util.Helpers.commaDelimitedToList;

public class TableAPISubTask {

    private static final Logger LOG = LoggerFactory.getLogger(TableAPISubTask.class);

    private final int UNBOUNDED_FROM_QUERY_KEY = -1;

    private TimestampSourceOffset _offset;
    private ServiceNowTableApiClient _client;
    private LocalDateTime _nextPollUtc;

    private long FAST_INTERVAL_NS;
    private long SLOW_INTERVAL_NS;
    private long INITIAL_QUERY_HOURS_AGO;
    private int TIMESTAMP_DELAY_INTERVAL_SECONDS;
    private int MAX_BATCH_SIZE;

    private String TABLE_NAME;
    private String TIMESTAMP_COLUMN_FIELD;
    private String IDENTIFIER_COLUMN_FIELD;
    private String TARGET_TOPIC;
    private List<String> FIELDS = null;

    private Schema _cachedValueSchema;
    private TableQueryPartition SOURCE_PARTITION;
    private IServiceNowTablePartitioner DESTINATION_PARTITIONER;

    private static String tryGetConfig(ServiceNowTableAPISourceTaskConfig config, String key, String defaultValue) {
        try {
            return config.getString(key);
        } catch(ConfigException ex) {
            return defaultValue;
        }
    }

    public TableAPISubTask(TableQueryPartition sourcePartition, TimestampSourceOffset offset, ServiceNowTableAPISourceTaskConfig config, ServiceNowTableApiClient client) {

        this._client = client;
        this._offset = offset;
        this.FAST_INTERVAL_NS = config.getLong(ServiceNowSourceConnectorConfig.TASK_POLL_FAST_INTERVAL_MS) * 1000000;
        this.SLOW_INTERVAL_NS = config.getLong(ServiceNowSourceConnectorConfig.TASK_POLL_SLOW_INTERVAL_MS) * 1000000;
        this.INITIAL_QUERY_HOURS_AGO = config.getLong(ServiceNowSourceConnectorConfig.TIMESTAMP_INITIAL_QUERY_HOURS_AGO);
        this.TIMESTAMP_DELAY_INTERVAL_SECONDS = config.getInt(ServiceNowSourceConnectorConfig.TIMESTAMP_DELAY_INTERVAL_SECONDS);
        this.MAX_BATCH_SIZE = config.getInt(ServiceNowSourceConnectorConfig.TASK_POLL_BATCH_MAX_SIZE);

        this.SOURCE_PARTITION = sourcePartition;
        final String tableKey = this.SOURCE_PARTITION.getTableName();
        // Required connector configurations with no defaults
        final String TABLE_NAME_KEY = String.format("table.whitelist.%s.name", tableKey);
        this.TABLE_NAME = config.getString(TABLE_NAME_KEY);
        if(this.TABLE_NAME == null || this.TABLE_NAME.trim().isEmpty()) {
            // NOTE(millies): Might consider supporting falling back to use the "tableKey" as the name of the table.
            throw new ConnectException(String.format("Must specify the name of the source table. Configuration [%s]", TABLE_NAME_KEY));
        }

        final String TIMESTAMP_FIELD_KEY = String.format("table.whitelist.%s.timestamp.field.name", tableKey);
        this.TIMESTAMP_COLUMN_FIELD = config.getString(TIMESTAMP_FIELD_KEY);
        if(this.TIMESTAMP_COLUMN_FIELD == null || this.TIMESTAMP_COLUMN_FIELD.trim().isEmpty()) {
            throw new ConnectException(String.format("Must specify what the name of the column to use for the timestamp. Configuration [%s]", TIMESTAMP_FIELD_KEY));
        }

        final String IDENTIFIER_FIELD_KEY = String.format("table.whitelist.%s.identifier.field.name", tableKey);
        this.IDENTIFIER_COLUMN_FIELD = config.getString(IDENTIFIER_FIELD_KEY);
        if(this.IDENTIFIER_COLUMN_FIELD == null || this.IDENTIFIER_COLUMN_FIELD.trim().isEmpty()) {
            throw new ConnectException(String.format("Must specify what the name of the column to use as a sortable identifier. Configuration [%s]", IDENTIFIER_FIELD_KEY));
        }

        this.TARGET_TOPIC = config.getString(ServiceNowSourceConnectorConfig.STREAM_PREFIX);
        if(this.TARGET_TOPIC == null || this.TARGET_TOPIC.trim().isEmpty()) {
            throw new ConnectException(String.format("Must specify the topic/stream prefix to which records should be published. Configuration [%s]", ServiceNowSourceConnectorConfig.STREAM_PREFIX));
        } else {
            this.TARGET_TOPIC = this.TARGET_TOPIC.replaceAll("\\.+$", "") + "." + tableKey;
        }

        final String FIELDS_KEY = String.format("table.whitelist.%s.fields", tableKey);
        final String rawFields = tryGetConfig(config, FIELDS_KEY, "");
        if(rawFields != null && !rawFields.trim().isEmpty()) {
            this.FIELDS = commaDelimitedToList(rawFields);
            if(this.FIELDS.isEmpty()) {
                throw new ConnectException(String.format("If specifying [%s], then it must include at least one field name.", FIELDS_KEY));
            }
        }

        this.DESTINATION_PARTITIONER = PartitionerFactory.build(tableKey, config);
    }

    public LocalDateTime getNextPollUtc() {
        return this._nextPollUtc;
    }

    private void setNextPollUtc(LocalDateTime nextPollUtc) {
        this._nextPollUtc = nextPollUtc;
    }

    public SourceRecordsPage poll() throws IOException, InterruptedException {

        List<SourceRecord> records = getBatch();
        long nextPollIntervalNs = SLOW_INTERVAL_NS;
        if(records != null && records.size() > 0) {
            nextPollIntervalNs = FAST_INTERVAL_NS;
        }

        final LocalDateTime nextPollUtc = LocalDateTime.now(ZoneOffset.UTC).plusNanos(nextPollIntervalNs);
        this.setNextPollUtc(nextPollUtc);

        LOG.info("Received [{}] records for table [{}]. Suggested next poll time for table is [{}].", records.size(), TABLE_NAME, nextPollUtc);
        return new SourceRecordsPage(records, nextPollUtc);
    }

    private List<SourceRecord> getBatch() throws IOException, InterruptedException {
        TableAPIQueryBuilder builder = buildQuery();
        LOG.info("Query [{}].", builder.build());

        List<JSONObject> rawRecords = this._client.getRecords(TABLE_NAME, builder, 0, this.MAX_BATCH_SIZE, this.FIELDS);
        return processBatch(rawRecords);
    }

    private List<SourceRecord> processBatch(List<JSONObject> batch) {

        List<SourceRecord> records = new ArrayList<>(batch.size());
        LocalDateTime lastProcessedTimestamp = null;
        String lastProcessedIdentifier = null;
        for(JSONObject result : batch) {

            // NOTE(Millies): dynamic building of the value schema. Not sure if this is going to cause an issue.
            if(this._cachedValueSchema == null && result != null) {
                this._cachedValueSchema = buildSchemaFromSimpleJsonRecord(result);
            }

            String rawTimestamp = result.getString(this.TIMESTAMP_COLUMN_FIELD);
            lastProcessedTimestamp = Helpers.parseServiceNowDateTimeUtc(rawTimestamp);
            lastProcessedIdentifier = result.getString(this.IDENTIFIER_COLUMN_FIELD);

            SourceRecord record = SourceRecordBuilder.Builder(DESTINATION_PARTITIONER)
                    .withSourcePartition(this.SOURCE_PARTITION.getPartition())
                    .withOffset(TimestampSourceOffset.toMap(lastProcessedTimestamp.toInstant(ZoneOffset.UTC), lastProcessedIdentifier))
                    .withTopic(this.TARGET_TOPIC)
                    .withValueSchema(this._cachedValueSchema)
                    .withRecord(result)
                    .build();

            records.add(record);

            this._offset.updateOffset(lastProcessedTimestamp.toInstant(ZoneOffset.UTC), lastProcessedIdentifier);
        }

        return records;
    }

    private LocalDateTime getFromDateTimeUtc() {
        final LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        Instant lastSeenTimestamp = this._offset.getLastSeenTimestamp();
        if(lastSeenTimestamp == null) {

            if(this.INITIAL_QUERY_HOURS_AGO == UNBOUNDED_FROM_QUERY_KEY) {
                return null;
            }

            final long initialDelayHours = this.INITIAL_QUERY_HOURS_AGO;
            lastSeenTimestamp = nowUtc.minusHours(initialDelayHours).toInstant(ZoneOffset.UTC);
        }

        // NOTE(millies): As a result of us shifting the through date back, I don't see why we would need to
        // substract from our last observed timestamp.
        // ASSUMPTION: If a updated timestamp "A" has been generated by the source system, all subsequent updated
        // timestamps "B" are guaranteed to be greater than or equal to "A".
        // "B" >= "A"
//        final long fromGracePeriodSeconds = this.TIMESTAMP_DELAY_INTERVAL_SECONDS;
//        lastSeenTimestamp = lastSeenTimestamp.minusSeconds(fromGracePeriodSeconds);

        //System.out.println(String.format("Last Seen Timestamp: %s", lastSeenTimestamp.atOffset(ZoneOffset.UTC)));
        return LocalDateTime.ofInstant(lastSeenTimestamp, ZoneOffset.UTC);
    }

    private LocalDateTime getThroughDateTimeUtc() {
        final LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        final int timestampDelaySeconds = this.TIMESTAMP_DELAY_INTERVAL_SECONDS;
        return nowUtc.minusSeconds(timestampDelaySeconds);
    }

    private TableAPIQueryBuilder buildQuery() {
        LocalDateTime fromDateTimeUtc = this.getFromDateTimeUtc();
        LocalDateTime throughDateTimeUtc = this.getThroughDateTimeUtc();
        if(fromDateTimeUtc == null) {
            return buildQueryUnboundedQuery(throughDateTimeUtc);
        } else {
            return buildQueryBoundedQuery(fromDateTimeUtc, throughDateTimeUtc);
        }
    }

    private TableAPIQueryBuilder buildQueryUnboundedQuery(LocalDateTime throughDateTimeUtc) {

        TableAPIQueryBuilder unboundedQuery = TableAPIQueryBuilder.Builder();

        // NOTE(millies): filter out any records with no value for the identifier field.
        unboundedQuery.whereIsNotEmpty(this.IDENTIFIER_COLUMN_FIELD);

        unboundedQuery
                .orderByAsc(this.TIMESTAMP_COLUMN_FIELD)
                .orderByAsc(this.IDENTIFIER_COLUMN_FIELD);

        return unboundedQuery;
    }

    /**
     * Query:
     *  SELECT
     *      *
     *  FROM
     *      "TABLE"
     *  WHERE
     *      (@lastSeenTimestamp != null
     *          AND "TIMESTAMP_FIELD" = @lastSeenTimestamp
     *          AND (@lastSeenIdentifier = null OR "IDENTIFIER_FIELD" > @lastSeenIdentifier))
     *      OR (
     *          "TIMESTAMP_FIELD" > @lastSeenTimestamp
     *          AND "TIMESTAMP_FIELD" <= @throughTimestamp
     *      )
     *  ORDER BY
     *      "TIMESTAMP_FIELD" ASC
     *      "IDENTIFIER_FIELD" ASC
     * @return
     */
    private TableAPIQueryBuilder buildQueryBoundedQuery(LocalDateTime fromDateTimeUtc, LocalDateTime throughDateTimeUtc) {

        TableAPIQueryBuilder lastSeenTimestampEqualsQuery = TableAPIQueryBuilder.Builder();

        lastSeenTimestampEqualsQuery.whereTimestampEquals(this.TIMESTAMP_COLUMN_FIELD, fromDateTimeUtc);
        String lastSeenIdentifier = this._offset.getLastReadIdentifier();
        if(lastSeenIdentifier != null) {
            lastSeenTimestampEqualsQuery.whereGreaterThan(this.IDENTIFIER_COLUMN_FIELD, lastSeenIdentifier);
        }

        // NOTE(millies): filter out any records with no value for the identifier field.
        lastSeenTimestampEqualsQuery.whereIsNotEmpty(this.IDENTIFIER_COLUMN_FIELD);

        TableAPIQueryBuilder timestampWindowQuery = TableAPIQueryBuilder.Builder();
        timestampWindowQuery
                .whereBetweenExclusive(this.TIMESTAMP_COLUMN_FIELD, fromDateTimeUtc, throughDateTimeUtc);

        // NOTE(millies): filter out any records with no value for the identifier field.
        timestampWindowQuery.whereIsNotEmpty(this.IDENTIFIER_COLUMN_FIELD);

        timestampWindowQuery
                .orderByAsc(this.TIMESTAMP_COLUMN_FIELD)
                .orderByAsc(this.IDENTIFIER_COLUMN_FIELD);

        // NOTE(Millies): join the queries together with an "or" type of thing.
        lastSeenTimestampEqualsQuery.union(timestampWindowQuery);

        return lastSeenTimestampEqualsQuery;
    }
}
