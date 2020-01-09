package com.ibm.ingestion.connect.servicenow.source.partitioner;

import com.ibm.ingestion.connect.servicenow.source.ServiceNowTableAPISourceTaskConfig;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.ibm.ingestion.connect.servicenow.util.Helpers.commaDelimitedToList;


public final class PartitionerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PartitionerFactory.class);

    private static String tryGetConfig(ServiceNowTableAPISourceTaskConfig config, String key, String defaultValue) {
        try {
            return config.getString(key);
        } catch(ConfigException ex) {
            return defaultValue;
        }
    }

    public static IServiceNowTablePartitioner build(String tableKey, ServiceNowTableAPISourceTaskConfig config) {
        final String PARTITION_TYPE_FIELD_KEY = String.format("table.whitelist.%s.partition.type", tableKey);
        final String partitionType = tryGetConfig(config, PARTITION_TYPE_FIELD_KEY, Partitioners.Default);

        LOG.info("Table [{}] destination partitioning type set to [{}]", tableKey, partitionType);
        switch(partitionType) {
            case Partitioners.FieldBased:
                return buildFieldBasedPartitioner(tableKey, config);
            case Partitioners.RoundRobin:
                return new RoundRobinServiceNowTablePartitioner();
            case Partitioners.Default:
                return new DefaultServiceNowTablePartitioner();
            default:
                throw new ConnectException(String.format("The specified partitioning type [%s] is not supported.", partitionType));
        }
    }

    private static IServiceNowTablePartitioner buildFieldBasedPartitioner(String tableKey, ServiceNowTableAPISourceTaskConfig config) {
        final String PARTITION_FIELDS_KEY = String.format("table.whitelist.%s.partition.fields", tableKey);
        final String rawPartitionFields = tryGetConfig(config, PARTITION_FIELDS_KEY, "");
        LOG.info("Table [{}] Partitioning Fields [{}]", tableKey, rawPartitionFields);
        if(rawPartitionFields != null && !rawPartitionFields.trim().isEmpty()) {
            List<String> keyFields = commaDelimitedToList(rawPartitionFields);
            if(!keyFields.isEmpty()) {
                return new FieldBasedServiceNowTablePartitioner(keyFields);
            }
        }

        throw new ConnectException(String.format("If specifying [%s], then it must include at least one field name.", PARTITION_FIELDS_KEY));
    }
}
