package com.ibm.ingestion.connect.servicenow.source;

import com.ibm.ingestion.connect.servicenow.source.partitioner.DefaultServiceNowTablePartitioner;
import com.ibm.ingestion.connect.servicenow.source.partitioner.IServiceNowTablePartitioner;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.json.JSONObject;

import java.util.Map;

import static com.ibm.ingestion.connect.servicenow.util.Helpers.buildStruct;

public class SourceRecordBuilder {

    private Schema _valueSchema;
    private JSONObject _record;
    private Map<String, Object> _offset;
    private Map<String, Object> _sourcePartition;
    private String _topic;

    private IServiceNowTablePartitioner _partitioner;

    private SourceRecordBuilder(IServiceNowTablePartitioner partitioner) {
        this._partitioner = partitioner;
    }

    public static SourceRecordBuilder Builder(IServiceNowTablePartitioner partitioner) {
        return new SourceRecordBuilder(partitioner);
    }

    public SourceRecordBuilder withValueSchema(Schema valueSchema) {
        this._valueSchema = valueSchema;
        return this;
    }

    public SourceRecordBuilder withRecord(JSONObject record) {
        this._record = record;
        return this;
    }

    public SourceRecordBuilder withOffset(Map<String, Object> offset) {
        this._offset = offset;
        return this;
    }

    public SourceRecordBuilder withSourcePartition(Map<String, Object> sourcePartition) {
        this._sourcePartition = sourcePartition;
        return this;
    }

    public SourceRecordBuilder withTopic(String topic) {
        this._topic = topic;
        return this;
    }

    public SourceRecord build() {
        return new SourceRecord(
                this._sourcePartition,
                this._offset,
                this._topic,
                this._partitioner.getTargetPartition(),
                this._partitioner.getKeySchema(),
                this._partitioner.getKeyValue(this._record),
                this._valueSchema,
                buildStruct(this._valueSchema, this._record)
        );
    }
}
