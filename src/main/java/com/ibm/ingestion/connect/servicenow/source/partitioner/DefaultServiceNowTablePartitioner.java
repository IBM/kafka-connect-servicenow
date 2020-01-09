package com.ibm.ingestion.connect.servicenow.source.partitioner;

import org.apache.kafka.connect.data.Schema;
import org.json.JSONObject;

/**
 * Sets destination kafka topic partitioning related fields in such a way that kafka-connect uses
 * a single partition '0' for all messages.
 *
 * Keeping the default as this since I want the person configuring the connector to make a conscious
 * decision to choose RoundRobin.  (Processing order of messages main concern here.)
 */
public class DefaultServiceNowTablePartitioner implements IServiceNowTablePartitioner {

    public String getPartitionerId() {
        return Partitioners.Default;
    }
    public Integer getTargetPartition() {
        // Default partitioner just uses a single partition.
        return 0;
    }

    public Schema getKeySchema() {
        return null;
    }

    public Object getKeyValue(JSONObject record) {
        return null;
    }
}
