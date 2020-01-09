package com.ibm.ingestion.connect.servicenow.source;

import com.ibm.ingestion.http.ServiceNowTableApiClient;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SubTaskManager {

    private static final Logger LOG = LoggerFactory.getLogger(SubTaskManager.class);

    private Queue<TableAPISubTask> _subTasks = new LinkedList<>();
    private ServiceNowTableApiClient _client;
    private ServiceNowTableAPISourceTaskConfig _config;

    private LocalDateTime _nextPollUtc;

    public SubTaskManager(ServiceNowTableAPISourceTaskConfig config, ServiceNowTableApiClient client) {
        this._config = config;
        this._client = client;
    }

    public void addSubTask(TableQueryPartition sourcePartition, TimestampSourceOffset offset) {
        this._subTasks.add(new TableAPISubTask(sourcePartition, offset, this._config, this._client));
    }

    public SourceRecordsPage poll() throws IOException, InterruptedException {

        List<SourceRecord> records = new ArrayList<>();
        LocalDateTime nextPollUtc = null;
        int numChecks = this._subTasks.size();
        while(numChecks-- > 0) {

            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            TableAPISubTask currentSubTask = null;
            try {
                // NOTE(millies): dequeue.
                currentSubTask = this._subTasks.remove();
                LocalDateTime candidate = currentSubTask.getNextPollUtc();
                if(candidate != null && candidate.isAfter(nowUtc)) {

                    // NOTE(millies): If a task wants to be polled before the current next poll cycle, then use that one.
                    if(nextPollUtc == null || candidate.isBefore(nextPollUtc)) {
                        nextPollUtc = candidate;
                    }

                    if(LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Skipping Candidate: %s, Now: %s, Candidate %s s > Now %s s = %s", candidate, nowUtc, candidate.toEpochSecond(ZoneOffset.UTC), nowUtc.toEpochSecond(ZoneOffset.UTC), candidate.isAfter(nowUtc)));
                    }
                    // NOTE(millies): skipping tasks that are not ready to be executed yet.
                    continue;
                }

                SourceRecordsPage subTaskPage = currentSubTask.poll();

                // NOTE(millies): Getting the earliest next poll time.
                candidate = subTaskPage.getNextTimeToPollUtc();
                if(nextPollUtc == null || candidate.isBefore(nextPollUtc)) {
                    nextPollUtc = candidate;
                } else if(LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Not using Candidate: %s, Next Poll: %s", candidate, nextPollUtc));
                }

                // NOTE(millies): add records from subtask to unified page.
                records.addAll(subTaskPage.getRecords());
            } finally {
                // NOTE(millies): requeue. back of the queue.
                if(currentSubTask != null) {
                    this._subTasks.add(currentSubTask);
                }
            }
        }

        // NOTE(millies): Delaying the polling loop at least a minimum amount of time.
        // This kicks in if for whatever reason the connector gets started without any tasks. And
        // would cpu peg the source task in the polling loop without us forcing at least some delay.
        if(nextPollUtc == null) {
            final int EMPTY_TASK_POLL_INTERVAL_SECONDS = 120;
            LOG.warn(String.format("No tasks have been defined in configuration, defaulting polling interval to %s. At least one table should be configured to be watched.", EMPTY_TASK_POLL_INTERVAL_SECONDS));
            nextPollUtc = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(EMPTY_TASK_POLL_INTERVAL_SECONDS);
        }

        return new SourceRecordsPage(records, nextPollUtc);
    }

    public void close() {
        try {
            this._client.close();
        }
        catch(Exception ex) {
            LOG.debug("Error occurred while closing the subtask manager. {}", ex);
        }
    }
}
