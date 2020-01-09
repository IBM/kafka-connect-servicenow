package com.ibm.ingestion.connect.servicenow.source.partitioner;

import org.apache.kafka.connect.data.Schema;
import org.json.JSONObject;

public interface IServiceNowTablePartitioner {
    String getPartitionerId();
    Integer getTargetPartition();
    Schema getKeySchema();
    Object getKeyValue(JSONObject record);
}
