package com.ibm.ingestion.connect.servicenow.source;

import java.util.HashMap;
import java.util.Map;

public class TableQueryPartition {


    private static final String PARTITION_KEY = "partition";
    private String _tableName;

    public TableQueryPartition(String tableName) {
        this._tableName = tableName;
    }

    public String getTableName() {
        return this._tableName;
    }

    public Map<String, Object> getPartition() {
        HashMap<String, Object> partitionInfo = new HashMap<>(1);
        partitionInfo.put(PARTITION_KEY, this._tableName);
        return partitionInfo;
    }
}
