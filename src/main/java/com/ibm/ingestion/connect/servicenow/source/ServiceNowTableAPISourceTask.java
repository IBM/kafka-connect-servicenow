package com.ibm.ingestion.connect.servicenow.source;

import com.ibm.ingestion.http.ServiceNowTableApiClient;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class ServiceNowTableAPISourceTask extends SourceTask {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceNowTableAPISourceTask.class);

    private ServiceNowTableAPISourceTaskConfig _config;
    private ServiceNowTableApiClient _client;
    private SubTaskManager taskManager;

    public ServiceNowTableAPISourceTask() {
    }

    @Override
    public void initialize(SourceTaskContext context) {
        this.context = context;
    }

    @Override
    public String version() {
        return "0.0.0.0";
    }

    @Override
    public void start(Map<String, String> props) {

        this._config = new ServiceNowTableAPISourceTaskConfig(props);
        this._client = new ServiceNowTableApiClient(this._config);

        String rawTables = this._config.getString(ServiceNowTableAPISourceTaskConfig.TABLE_LIST);
        if(rawTables == null || rawTables.trim().isEmpty()) {
            throw new ConnectException(String.format("Must provide one or more tables to poll. Configuration [%s] Comma separated.", ServiceNowTableAPISourceTaskConfig.TABLE_LIST));
        }

        String[] tables = rawTables.split(",");
        if(tables.length == 0) {
            throw new ConnectException(String.format("Must provide one or more tables to poll. Configuration [%s] Comma separated.", ServiceNowTableAPISourceTaskConfig.TABLE_LIST));
        }

        LOG.info("Starting SourceTask for [{}] tables, {}.", tables.length, tables);
        try {
            this._client.init();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectException("An error occurred while initializing the ServiceNow Client.", e);
        }

        this.taskManager = new SubTaskManager(this._config, this._client);

        // TODO(millies): clean up this logic with some refactoring. This felt like fighting against the object model.
        List<TableQueryPartition> partitions = new ArrayList<>(tables.length);
        List<Map<String, Object>> offsetKeys = new ArrayList<>(tables.length);
        for(String table : tables) {
            TableQueryPartition partition = new TableQueryPartition(table);
            partitions.add(partition);
            offsetKeys.add(partition.getPartition());

            LOG.info("Added source partition for table [{}].", partition.getTableName());
        }

        this._offsets = this.context.offsetStorageReader().offsets(offsetKeys);
        for(TableQueryPartition partition : partitions) {
            this.taskManager.addSubTask(partition, TimestampSourceOffset.fromMap(this._offsets.get(partition.getPartition())));
        }
    }

    private LocalDateTime _nextPollUtc;

    private void setNextPollUtc(LocalDateTime nextPollUtc) {
        this._nextPollUtc = nextPollUtc;
    }

    private LocalDateTime getNextPollUtc() {
        if(this._nextPollUtc == null) {
            return LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1); // we should do an initial query.
        }
        return this._nextPollUtc;
    }

    private boolean _hasNextPollTimeBeenLogged = false;

    @Override
    public List<SourceRecord> poll() throws InterruptedException {

        // NOTE(Millies): Should we check for new records?
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime nextPollUtc = this.getNextPollUtc();
        long delayMilliseconds = nextPollUtc.toInstant(ZoneOffset.UTC).toEpochMilli() - nowUtc.toInstant(ZoneOffset.UTC).toEpochMilli();
        if(delayMilliseconds > 0) {
            if(!_hasNextPollTimeBeenLogged) {
                LOG.info("Next poll in [{}] ms.", delayMilliseconds);
                _hasNextPollTimeBeenLogged = true;
            }

            // NOTE(millies): Periodically giving control back to the calling class so we can gracefully exit. If we don't
            // do this for an interval less than 10 seconds, we get a nasty "Graceful stop of task <blah> failed.".
            Thread.sleep(Math.min(5000, delayMilliseconds));
            return new ArrayList<>();
        }
        _hasNextPollTimeBeenLogged = false;

        SourceRecordsPage page = null;
        try {
            LOG.info("Polling tables...");
            page = this.taskManager.poll();
            this.setNextPollUtc(page.getNextTimeToPollUtc());
            LOG.info("Publishing [{}] records, and set to poll again at [{}].", page.getRecords().size(), page.getNextTimeToPollUtc());
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectException("An error occurred while trying to poll.", e);
        }

        return page.getRecords();
    }

    @Override
    public synchronized void stop() {
        try {
            LOG.info("Stopping.");
            this.taskManager.close();
        } catch(Exception ex) {
            LOG.error(String.format("Exception while stopping: %s", ex));
        }
    }

    private Map<Map<String, Object>, Map<String, Object>> _offsets;
}
