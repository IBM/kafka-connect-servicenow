package com.ibm.ingestion.connect.servicenow.source.partitioner;

import org.apache.kafka.connect.data.Schema;
import org.json.JSONObject;

/**
 * Sets destination kafka topic partitioning related fields in such a way that kafka-connect uses
 * a Round Robin pattern for message => Partition assignment.
 */
public class RoundRobinServiceNowTablePartitioner implements IServiceNowTablePartitioner {

    public String getPartitionerId() {
        return Partitioners.RoundRobin;
    }

    public Integer getTargetPartition() {
        return null;
    }

    public Schema getKeySchema() {
        return null;
    }

    public Object getKeyValue(JSONObject record) {
        return null;
    }
}
