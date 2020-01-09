package com.ibm.ingestion.connect.servicenow.source;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.*;

public class ServiceNowSourceConnectorConfig extends AbstractConfig {

    public static final ConfigDef CONFIGURATION = baseConfigDef();
    private Map<String, Object> WHITELIST = new HashMap<>();
    public ServiceNowSourceConnectorConfig(Map<String, ?> props) {
        super(CONFIGURATION, props);
    }

    protected ServiceNowSourceConnectorConfig(ConfigDef subclassConfig, Map<String, String> props) {
        super(subclassConfig, props);
    }

    public static ConfigDef baseConfigDef() {
        ConfigDef config = new ConfigDef();
        addServiceNowClientOptions(config);
        addConnectorOptions(config);
        return config;
    }

    public static final String TABLE_WHITELIST_PREFIX = "table.whitelist.";
    public static final String TABLE_WHITELIST_NAME_POSTFIX = "name";
    public static final String TABLE_WHITELIST_TIMESTAMP_FIELD_POSTFIX = "timestamp.field.name";
    public static final String TABLE_WHITELIST_IDENTIFIER_FIELD_POSTFIX = "identifier.field.name";
    public static final String TABLE_WHITELIST_FIELDS_POSTFIX = "fields";

    public static final Map<String, String> WHITELIST_TABLE_PROPERTY_DISPLAY = new HashMap<>();
    public static final Map<String, String> WHITELIST_TABLE_PROPERTY_DOCS = new HashMap<>();
    static {

        WHITELIST_TABLE_PROPERTY_DOCS.put(TABLE_WHITELIST_NAME_POSTFIX, "The name of the table to query as is required by the ServiceNow TableAPI.");
        WHITELIST_TABLE_PROPERTY_DISPLAY.put(TABLE_WHITELIST_NAME_POSTFIX, "Table Name");

        WHITELIST_TABLE_PROPERTY_DOCS.put(TABLE_WHITELIST_TIMESTAMP_FIELD_POSTFIX, "The timestamp field to use when detecting new or modified records.");
        WHITELIST_TABLE_PROPERTY_DISPLAY.put(TABLE_WHITELIST_TIMESTAMP_FIELD_POSTFIX, "Timestamp Field");

        WHITELIST_TABLE_PROPERTY_DOCS.put(TABLE_WHITELIST_IDENTIFIER_FIELD_POSTFIX, "The name of a sortable column uniquely identifying a particular record.");
        WHITELIST_TABLE_PROPERTY_DISPLAY.put(TABLE_WHITELIST_IDENTIFIER_FIELD_POSTFIX, "Identifier Field");

        WHITELIST_TABLE_PROPERTY_DOCS.put(TABLE_WHITELIST_FIELDS_POSTFIX, "The list of fields to retrieve from the table.");
        WHITELIST_TABLE_PROPERTY_DISPLAY.put(TABLE_WHITELIST_FIELDS_POSTFIX, "Fields");
    }

    private static boolean isValidWhitelistTableProperty(String candidate) {
        // NOTE(Millies): Assuming that all valid postfixes will have an entry in this map.
        String exists = getWhitelistTablePropertyDisplay(candidate);
        return exists != null && exists.trim().length() > 0;
    }

    private static String getWhitelistTablePropertyDoc(String key) {
        // NOTE(Millies): Assuming that all valid postfixes will have an entry in this map.
        for(String token : WHITELIST_TABLE_PROPERTY_DOCS.keySet()) {
            if(key.endsWith(token)) {
                return WHITELIST_TABLE_PROPERTY_DOCS.get(token);
            }
        }
        return null;
    }

    private static String getWhitelistTablePropertyDisplay(String key) {
        // NOTE(Millies): Assuming that all valid postfixes will have an entry in this map.
        for(String token : WHITELIST_TABLE_PROPERTY_DISPLAY.keySet()) {
            if(key.endsWith(token)) {
                return WHITELIST_TABLE_PROPERTY_DISPLAY.get(token);
            }
        }
        return null;
    }

//    @Override
//    protected Map<String,Object> postProcessParsedConfig(Map<String,Object> parsedValues) {
//
//        final String WHITELIST_CONFIGURATION_GROUP = "Table Whitelist";
//
//        // NOTE(millies): Default value is specified so that these fields can be validated as "optional" and only
//        // throw an error during runtime if the configuration requires one of them to be set.
//        final String DEFAULT_VALUE = "";
//        int orderInGroup = 0;
//
//        HashMap<String, Object> additionalProperties = new HashMap<>();
//        Map<String, Object> originalProperties = this.originals();
//        for(String key : originalProperties.keySet()) {
//            final String candidateKey = key.toLowerCase(Locale.US);
//            if(!candidateKey.startsWith(TABLE_WHITELIST_PREFIX) || !isValidWhitelistTableProperty(candidateKey) || CONFIGURATION.configKeys().containsKey(candidateKey)) {
//                continue;
//            }
//
//
//            CONFIGURATION.define(
//                    candidateKey,
//                    ConfigDef.Type.STRING,
//                    DEFAULT_VALUE, // Providing a default value so they are not considered required at config time.
//                    ConfigDef.Importance.HIGH,
//                    getWhitelistTablePropertyDoc(candidateKey),
//                    WHITELIST_CONFIGURATION_GROUP,
//                    ++orderInGroup,
//                    ConfigDef.Width.LONG,
//                    getWhitelistTablePropertyDisplay(candidateKey)
//            );
//
//            additionalProperties.put(candidateKey, originalProperties.getOrDefault(key, null));
//        }
//
//        return additionalProperties;
//    }

    @Override
    protected Map<String,Object> postProcessParsedConfig(Map<String,Object> parsedValues) {
        HashMap<String, Object> properties = new HashMap<>();


        this.originals().forEach( (key, value) -> {
            if (key.startsWith("table.whitelist.") && !CONFIGURATION.configKeys().containsKey(key)) {
                CONFIGURATION.define(
                        key,
                        ConfigDef.Type.STRING,
                        "",
                        ConfigDef.Importance.HIGH,
                        "",
                        "Table Whitelist",
                        0,
                        ConfigDef.Width.LONG,
                        "");
            }

            if (key.startsWith("table.whitelist.")) {
                properties.put (key, value.toString());
            }
        });

        return properties;
    }


    private static final String SERVICENOW_CLIENT_GROUP = "ServiceNow Client";
    public static final String SERVICENOW_CLIENT_BASEURI = "servicenow.client.base.uri";
    private static final String SERVICENOW_CLIENT_BASEURI_DOC
            = "The base url to use for the target ServiceNow Instance. Example: 'https://ibmmhasdev2.service-now.com'";
    private static final String SERVICENOW_CLIENT_BASEURI_DISPLAY
            = "ServiceNow Instance Base Url";


    public static final String SERVICENOW_CLIENT_BASETABLEAPIPATH = "servicenow.client.base.tableapi.path";
    private static final String SERVICENOW_CLIENT_BASETABLEAPIPATH_DOC
            = "The base path to use when calling ServiceNow's TableAPI. Example: '/api/now/table'";
    private static final String SERVICENOW_CLIENT_BASETABLEAPIPATH_DISPLAY
            = "ServiceNow Instance TableAPI Base Path";
    public static final String SERVICENOW_CLIENT_BASETABLEAPIPATH_DEFAULT = "/api/now/table";


    public static final String SERVICENOW_CLIENT_OAUTH_PATH = "servicenow.client.oauth.path";
    private static final String SERVICENOW_CLIENT_OAUTH_PATH_DOC
            = "The path to use when logging into the ServiceNow instance using OAuth. Example: '/oauth_token.do'";
    private static final String SERVICENOW_CLIENT_OAUTH_PATH_DISPLAY
            = "ServiceNow Instance OAuth Path";
    public static final String SERVICENOW_CLIENT_OAUTH_PATH_DEFAULT = "/oauth_token.do";


    public static final String SERVICENOW_CLIENT_OAUTH_CLIENTID = "servicenow.client.oauth.clientid";
    private static final String SERVICENOW_CLIENT_OAUTH_CLIENTID_DOC
            = "The OAuth Client ID to use when authenticating.";
    private static final String SERVICENOW_CLIENT_OAUTH_CLIENTID_DISPLAY
            = "ServiceNow OAuth Client ID";


    public static final String SERVICENOW_CLIENT_OAUTH_CLIENTSECRET = "servicenow.client.oauth.clientsecret";
    private static final String SERVICENOW_CLIENT_OAUTH_CLIENTSECRET_DOC
            = "The OAuth Client Secret to use when authenticating.";
    private static final String SERVICENOW_CLIENT_OAUTH_CLIENTSECRET_DISPLAY
            = "ServiceNow OAuth Client Secret";


    public static final String SERVICENOW_CLIENT_OAUTH_USERNAME = "servicenow.client.oauth.username";
    private static final String SERVICENOW_CLIENT_OAUTH_USERNAME_DOC
            = "The OAuth User Name to use when authenticating.";
    private static final String SERVICENOW_CLIENT_OAUTH_USERNAME_DISPLAY
            = "ServiceNow OAuth User Name";


    public static final String SERVICENOW_CLIENT_OAUTH_USERPASSWORD = "servicenow.client.oauth.userpassword";
    private static final String SERVICENOW_CLIENT_OAUTH_USERPASSWORD_DOC
            = "The OAuth User Password to use when authenticating.";
    private static final String SERVICENOW_CLIENT_OAUTH_USERPASSWORD_DISPLAY
            = "ServiceNow OAuth User Password";

    public static final String SERVICENOW_CLIENT_TIMEOUT_SECONDS = "servicenow.client.request.timeout.seconds";
    private static final String SERVICENOW_CLIENT_TIMEOUT_SECONDS_DOC
            = "The amount of time in seconds that the client will wait for a response from ServiceNow.";
    private static final String SERVICENOW_CLIENT_TIMEOUT_SECONDS_DISPLAY
            = "ServiceNow Request Timeout (seconds)";
    public static final int SERVICENOW_CLIENT_TIMEOUT_SECONDS_DEFAULT = 30;


    public static final String SERVICENOW_CLIENT_MAX_RETRIES = "servicenow.client.request.retries.max";
    private static final String SERVICENOW_CLIENT_MAX_RETRIES_DOC
            = "The number of times the connector task will retry a request to ServiceNow before entering a failed state. If not specified, or a '-1' is used, then there is no retry limit.";
    private static final String SERVICENOW_CLIENT_MAX_RETRIES_DISPLAY
            = "ServiceNow Request Max Retries";
    public static final int SERVICENOW_CLIENT_MAX_RETRIES_DEFAULT = -1;


    public static final String SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS = "servicenow.client.request.retries.backoff.seconds";
    private static final String SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS_DOC
            = "The amount of time in seconds that the connector task will wait before retrying after a ServiceNow request failure.";
    private static final String SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS_DISPLAY
            = "ServiceNow Retry Backoff (seconds)";
    public static final int SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS_DEFAULT = 30;

    public static final String SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS = "servicenow.client.read.timeout.seconds";
    private static final String SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS_DOC
            = "The amount of time in seconds that the client will wait for data.";
    private static final String SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS_DISPLAY
            = "ServiceNow Client Request Read Timeout (seconds)";
    public static final int SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS_DEFAULT = 30;

    public static final String SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS = "servicenow.client.connection.timeout.seconds";
    private static final String SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS_DOC
            = "The amount of time in seconds that the client will wait while establishing a connection.";
    private static final String SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS_DISPLAY
            = "ServiceNow Client Connection Timeout (seconds)";
    public static final int SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT = 30;



    public static final String SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS = "servicenow.client.connection.pool.max.idle.connections";
    private static final String SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS_DOC
            = "The maximum number of idle connections to hold in the connection pool.";
    private static final String SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS_DISPLAY
            = "ServiceNow Client Max Idle Connections";
    public static final int SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS_DEFAULT = 2;

    public static final String SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS = "servicenow.client.connection.pool.keep.alive.duration.seconds";
    private static final String SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS_DOC
            = "The amount of time to hold onto idle connections.";
    private static final String SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS_DISPLAY
            = "ServiceNow Client Connection Pool Keep Alive Duration (seconds)";
    public static final int SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS_DEFAULT = 60;


    private static void addServiceNowClientOptions(ConfigDef config) {
        int orderInGroup = 0;
        config.define(
                SERVICENOW_CLIENT_BASEURI,
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                SERVICENOW_CLIENT_BASEURI_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_BASEURI_DISPLAY
        ).define(
                SERVICENOW_CLIENT_BASETABLEAPIPATH,
                ConfigDef.Type.STRING,
                SERVICENOW_CLIENT_BASETABLEAPIPATH_DEFAULT,
                ConfigDef.Importance.HIGH,
                SERVICENOW_CLIENT_BASETABLEAPIPATH_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_BASETABLEAPIPATH_DISPLAY
        ).define(
                SERVICENOW_CLIENT_OAUTH_PATH,
                ConfigDef.Type.STRING,
                SERVICENOW_CLIENT_OAUTH_PATH_DEFAULT,
                ConfigDef.Importance.HIGH,
                SERVICENOW_CLIENT_OAUTH_PATH_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_OAUTH_PATH_DISPLAY
        ).define(
                SERVICENOW_CLIENT_OAUTH_CLIENTID,
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                SERVICENOW_CLIENT_OAUTH_CLIENTID_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_OAUTH_CLIENTID_DISPLAY
        ).define(
                SERVICENOW_CLIENT_OAUTH_CLIENTSECRET,
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                SERVICENOW_CLIENT_OAUTH_CLIENTSECRET_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_OAUTH_CLIENTSECRET_DISPLAY
        ).define(
                SERVICENOW_CLIENT_OAUTH_USERNAME,
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                SERVICENOW_CLIENT_OAUTH_USERNAME_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_OAUTH_USERNAME_DISPLAY
        ).define(
                SERVICENOW_CLIENT_OAUTH_USERPASSWORD,
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                SERVICENOW_CLIENT_OAUTH_USERPASSWORD_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_OAUTH_USERPASSWORD_DISPLAY
        ).define(
                SERVICENOW_CLIENT_TIMEOUT_SECONDS,
                ConfigDef.Type.INT,
                SERVICENOW_CLIENT_TIMEOUT_SECONDS_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                SERVICENOW_CLIENT_TIMEOUT_SECONDS_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_TIMEOUT_SECONDS_DISPLAY
        ).define(
                SERVICENOW_CLIENT_MAX_RETRIES,
                ConfigDef.Type.INT,
                SERVICENOW_CLIENT_MAX_RETRIES_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                SERVICENOW_CLIENT_MAX_RETRIES_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_MAX_RETRIES_DISPLAY
        ).define(
                SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS,
                ConfigDef.Type.INT,
                SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS_DISPLAY
        ).define(
                SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS,
                ConfigDef.Type.INT,
                SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS_DISPLAY
        ).define(
                SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS,
                ConfigDef.Type.INT,
                SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS_DISPLAY
        ).define(
                SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS,
                ConfigDef.Type.INT,
                SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS_DISPLAY
        ).define(
                SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS,
                ConfigDef.Type.INT,
                SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS_DOC,
                SERVICENOW_CLIENT_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS_DISPLAY
        );
    }

    private static final String CONNECTOR_GROUP = "Connector";
    public static final String TIMESTAMP_DELAY_INTERVAL_SECONDS = "through.timestamp.delay.interval.seconds";
    private static final String TIMESTAMP_DELAY_INTERVAL_SECONDS_DOC
            = "How long to wait before a particular record with a valid timestamp is published. Effectively a padding to allow data for that particular timestamp to commit.";
    private static final String TIMESTAMP_DELAY_INTERVAL_SECONDS_DISPLAY
            = "Timestamp Delay (Seconds)";
    private static final int TIMESTAMP_DELAY_INTERVAL_SECONDS_DEFAULT = 0;


    public static final String TABLE_WHITELIST = "table.whitelist";
    private static final String TABLE_WHITELIST_DOC
            = "A comma separated list of table identifiers used subsequently in this config.";
    private static final String TABLE_WHITELIST_DISPLAY
            = "Table Whitelist";

    public static final String STREAM_PREFIX = "topic.prefix";
    private static final String STREAM_PREFIX_DOC
            = "Prefix to prepend to table names to generate the names of topics to publish records.";
    private static final String STREAM_PREFIX_DISPLAY
            = "Topic Prefix";

    public static final String TASK_POLL_FAST_INTERVAL_MS = "task.poll.fast.interval.ms";
    private static final String TASK_POLL_FAST_INTERVAL_MS_DOC
            = "The amount of time a task waits before querying for additional table data. This interval is used when the last request for the table returned data.";
    private static final String TASK_POLL_FAST_INTERVAL_MS_DISPLAY
            = "Task Polling Fast Interval (ms)";
    private static final long TASK_POLL_FAST_INTERVAL_MS_DEFAULT = 500;


    public static final String TASK_POLL_SLOW_INTERVAL_MS = "task.poll.slow.interval.ms";
    private static final String TASK_POLL_SLOW_INTERVAL_MS_DOC
            = "The amount of time a task waits before querying for additional table data. This interval is used when the last request for the table returned no data.";
    private static final String TASK_POLL_SLOW_INTERVAL_MS_DISPLAY
            = "Task Polling Slow Interval (ms)";
    private static final long TASK_POLL_SLOW_INTERVAL_MS_DEFAULT = 30 * 1000;


    public static final String TASK_POLL_BATCH_MAX_SIZE = "task.poll.batch.max.size";
    private static final String TASK_POLL_BATCH_MAX_SIZE_DOC
            = "The maximum number of records to request from ServiceNow at a time. Controls the request page size.";
    private static final String TASK_POLL_BATCH_MAX_SIZE_DISPLAY
            = "Task Max Batch Size";
    private static final int TASK_POLL_BATCH_MAX_SIZE_DEFAULT = 20;


    public static final String TIMESTAMP_INITIAL_QUERY_HOURS_AGO = "timestamp.initial.query.hours.ago";
    private static final String TIMESTAMP_INITIAL_QUERY_HOURS_AGO_DOC
            = "Tasks will use this to calculate the initial timestamp used to query tables. If not specified, or '-1', then the initial query from clause is unbounded.";
    private static final String TIMESTAMP_INITIAL_QUERY_HOURS_AGO_DISPLAY
            = "Timestamp Initial Query (Hours Ago)";
    private static final long TIMESTAMP_INITIAL_QUERY_HOURS_AGO_DEFAULT = -1;


    private static void addConnectorOptions(ConfigDef config) {
        int orderInGroup = 0;
        config.define(
                TIMESTAMP_DELAY_INTERVAL_SECONDS,
                ConfigDef.Type.INT,
                TIMESTAMP_DELAY_INTERVAL_SECONDS_DEFAULT,
                ConfigDef.Importance.LOW,
                TIMESTAMP_DELAY_INTERVAL_SECONDS_DOC,
                CONNECTOR_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                TIMESTAMP_DELAY_INTERVAL_SECONDS_DISPLAY
        ).define(
                TABLE_WHITELIST,
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                TABLE_WHITELIST_DOC,
                CONNECTOR_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                TABLE_WHITELIST_DISPLAY
        ).define(
                STREAM_PREFIX,
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                STREAM_PREFIX_DOC,
                CONNECTOR_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                STREAM_PREFIX_DISPLAY
        ).define(
                TASK_POLL_FAST_INTERVAL_MS,
                ConfigDef.Type.LONG,
                TASK_POLL_FAST_INTERVAL_MS_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                TASK_POLL_FAST_INTERVAL_MS_DOC,
                CONNECTOR_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                TASK_POLL_FAST_INTERVAL_MS_DISPLAY
        ).define(
                TASK_POLL_SLOW_INTERVAL_MS,
                ConfigDef.Type.LONG,
                TASK_POLL_SLOW_INTERVAL_MS_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                TASK_POLL_SLOW_INTERVAL_MS_DOC,
                CONNECTOR_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                TASK_POLL_SLOW_INTERVAL_MS_DISPLAY
        ).define(
                TASK_POLL_BATCH_MAX_SIZE,
                ConfigDef.Type.INT,
                TASK_POLL_BATCH_MAX_SIZE_DEFAULT,
                ConfigDef.Importance.MEDIUM,
                TASK_POLL_BATCH_MAX_SIZE_DOC,
                CONNECTOR_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                TASK_POLL_BATCH_MAX_SIZE_DISPLAY
        ).define(
                TIMESTAMP_INITIAL_QUERY_HOURS_AGO,
                ConfigDef.Type.LONG,
                TIMESTAMP_INITIAL_QUERY_HOURS_AGO_DEFAULT,
                ConfigDef.Importance.HIGH,
                TIMESTAMP_INITIAL_QUERY_HOURS_AGO_DOC,
                CONNECTOR_GROUP,
                ++orderInGroup,
                ConfigDef.Width.LONG,
                TIMESTAMP_INITIAL_QUERY_HOURS_AGO_DISPLAY
        );
    }
}
