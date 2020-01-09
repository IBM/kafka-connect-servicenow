package com.ibm.ingestion.connect.servicenow.source.partitioner;

import org.apache.kafka.connect.data.Schema;
import org.json.JSONObject;

import java.util.List;

import static com.ibm.ingestion.connect.servicenow.util.Helpers.buildKeyStruct;
import static com.ibm.ingestion.connect.servicenow.util.Helpers.buildSchemaForKey;

public class FieldBasedServiceNowTablePartitioner implements IServiceNowTablePartitioner {

    private Schema _keySchema;
    private List<String> _keyFields;

    public FieldBasedServiceNowTablePartitioner(List<String> keyFields) {
        this._keyFields = keyFields;
        this._keySchema = buildSchemaForKey(this._keyFields);
    }

    public String getPartitionerId() {
        return Partitioners.FieldBased;
    }

    public Integer getTargetPartition() {
        return null; // NOTE(Millies): letting kafka connect framework determine partition based on getKeyValue.
    }

    public Schema getKeySchema() {
        return this._keySchema;
    }

    public Object getKeyValue(JSONObject record) {
        return buildKeyStruct(this._keySchema, this._keyFields, record);
    }
}
