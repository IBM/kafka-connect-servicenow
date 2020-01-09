package com.ibm.ingestion.connect.servicenow.source;

import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

public class ServiceNowTableAPISourceTaskConfig extends ServiceNowSourceConnectorConfig {

    public static final String TABLE_LIST = "task.tablekeys";
    private static final String TABLES_DOC = "A comma separated list of table keys for this task to watch for changes.";

    static ConfigDef TASK_CONFIG = baseConfigDef()
            .define(TABLE_LIST, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, TABLES_DOC);

    public ServiceNowTableAPISourceTaskConfig(Map<String, String> props) {
        super(TASK_CONFIG, props);
    }
}
