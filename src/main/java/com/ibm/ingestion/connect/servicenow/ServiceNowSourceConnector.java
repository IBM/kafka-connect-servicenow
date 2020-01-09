package com.ibm.ingestion.connect.servicenow;

import com.ibm.ingestion.connect.servicenow.source.ServiceNowSourceConnectorConfig;
import com.ibm.ingestion.connect.servicenow.source.ServiceNowTableAPISourceTask;
import com.ibm.ingestion.connect.servicenow.source.ServiceNowTableAPISourceTaskConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.connect.util.ConnectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceNowSourceConnector extends SourceConnector {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceNowSourceConnector.class);

    private Map<String, String> _configProperties;
    private ServiceNowSourceConnectorConfig _config;

    @Override
    public void start(Map<String, String> props) {

        LOG.info("Starting [{}].", ServiceNowSourceConnector.class.getName());
        this._configProperties = props;
        this._config = new ServiceNowSourceConnectorConfig(props);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return ServiceNowTableAPISourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {

        String[] rawTableWhitelist = this._config.getString(ServiceNowSourceConnectorConfig.TABLE_WHITELIST).split(",");
        if(rawTableWhitelist == null || rawTableWhitelist.length == 0) {
            throw new ConnectException(String.format("[%s] must have at least one table defined.", ServiceNowSourceConnectorConfig.TABLE_WHITELIST));
        }

        List<Map<String, String>> taskConfigs = new ArrayList<Map<String, String>>();
        int groups = Math.min(rawTableWhitelist.length, maxTasks);

        List<String> tableWhiteList = new ArrayList<String>(rawTableWhitelist.length);
        for(String table : rawTableWhitelist) {
            tableWhiteList.add(table);
        }
        List<List<String>> groupedTables = ConnectorUtils.groupPartitions(tableWhiteList, groups);
        for (List<String> group : groupedTables) {
            Map<String, String> taskProps = new HashMap<String, String>(this._configProperties);
            taskProps.put(ServiceNowTableAPISourceTaskConfig.TABLE_LIST, String.join(",", group));
            taskConfigs.add(taskProps);
        }

        LOG.debug("Tasks with configs: {}", taskConfigs);
        LOG.info("Grouped tables into [{}] buckets, [{}].", groupedTables.size(), groupedTables);
        return taskConfigs;
    }

    @Override
    public void stop() {
        LOG.info("Stopping [{}].", ServiceNowSourceConnector.class.getName());

    }

    @Override
    public ConfigDef config() {
        return ServiceNowSourceConnectorConfig.CONFIGURATION;
    }

    @Override
    public String version() {
        return "0.5.0.0";
    }

    public static void main(String[] args) {

    }
}
