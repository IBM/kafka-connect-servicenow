# Kafka Connect ServiceNow Source Connector

This module is a [Kafka Connect](https://docs.confluent.io/current/connect/index.html)
Source Connector for the [ServiceNow Table API](https://developer.servicenow.com/app.do#!/rest_api_doc?v=madrid&id=c_TableAPI).
It provides facilities for polling arbitrary ServiceNow tables via its Table API and publishing
detected changes to a [Kafka topic](https://kafka.apache.org/documentation/#intro_topics).

This module is agnostic to the ServiceNow model being used as all the table names, and fields used
are provided via configuration.

Going forward in this readme `source_table` will be used to refer to a table accessible via the
ServiceNow TableAPI and `table_config_key` will be used to refer to an identifier representing a
particular `source_table` within the Kafka Connect ServiceNow Source Connector's configuration.

### Specifying Source Tables

The source table configuration is provided by first specifying a comma delimited list of
`table_config_key(s)`. Each `table_config_key` is used to specify `source table` specific
configuration properties.

**Example:**
```json
{
  "config": {
    "table.whitelist": "case,changerequest"
  }
}
```

In the above example, the Source Connector is going to look for `source table` specific configurations
using the `table_config_keys` of `case` and `changerequest`.

#### Source Table Constraints

The current implementation requires the following constraints to be satisfied in order to poll it.

- There must exist a `sortable last updated at timestamp` column that the polling algorithm can use to manage
its window. This column is specified with the `table.whitelist.<table config id>.timestamp.field.name` configuration
key.
- There must exist a `sortable unique identifier` column that the polling algorithm can use to distinguish
between records in the source table. This column is specified with the `table.whitelist.<table config id>.identifier.field.name`
configuration key.

Entry point for this connector is `com.ibm.ingestion.connect.servicenow.ServiceNowSourceConnector`.

The `ServiceNowSourceConnector` class evaluates the connector configuration and determines how
many `ServiceNowTableAPISourceTask` instances it needs to configure. It returns the initialized
`ServiceNowTableAPISourceTaskConfig` objects to `kafka connect`.

#### Destination Kafka Topic Partitioning
This Source Connector supports several destination partitioning types.

`table.whitelist.<table config id>.partition.type`

###### Values:

- `default` - This type forces all messages to be on partition `0`. Useful for testing.
- `round-robin` - This type assigns messages to partitions in a round-robin fashion.
- `field-based` - This type requires a set of `fields` (`table.whitelist.<table config id>.partition.fields`) to use as the `partitioning key`
which determines the destination partition. This is useful if you need to guarantee
messages about a particular entity go to the same partition.

#### Connector Configuration

Configuration | Default | Notes
:------- | :------- | :-------
table.whitelist | none | A list of source table keys to use in subsequent, table specific, configurations. This keys are also used when calculating the `target kafka topic` for a particular `source table`.
topic.prefix | none | The prefix to use when publishing messages for source tables. For example if the prefix is `ibm.test.servicenow`, and the `source table key` is `changerequest`, then the calculated topic will be `ibm.test.servicenow.changerequest`.This is a required setting and has no default value provided.

#### ServiceNow Table API Authentication Configuration
Configuration | Default | Notes
:------- | :------- | :-------
servicenow.client.oauth.path | `/oauth_token.do` | The path to use when logging into the ServiceNow instance using OAuth.
servicenow.client.oauth.clientid | none | The OAuth Client ID to use when authenticating. This is a required field and no default is provided.
servicenow.client.oauth.clientsecret | none | The OAuth Client Secret to use when authenticating.
servicenow.client.oauth.username | none | The OAuth User Name to use when authenticating.
servicenow.client.oauth.userpassword | none | the OAuth User Password to use when Authenticating.

#### Connector to ServiceNow HTTP Client Configuration

ServiceNow Client Implementation: `com.ibm.ingestion.http.ServiceNowTableApiClient`

Configuration  | Default | Notes
:------------- | :------------ | :------------
servicenow.client.connection.timeout.seconds | 30 | The amount of time in seconds to wait for establishing a connection.
servicenow.client.request.timeout.seconds | 30 | The overall timeout for any call. This configuration is independent of the read, and write timeouts.
servicenow.client.read.timeout.seconds | 30 | The amount of time in seconds to wait for a read operation to complete.
servicenow.client.request.retries.max | none | The number of times a http call will be retried for an IO exception. If this setting is excluded, then the task will continuously retry http calls.
servicenow.client.request.retries.backoff.seconds | 30 | The amount of time delayed between retries.
servicenow.client.connection.pool.max.idle.connections | 2 | The maximum number of idle connections to hold in the connection pool.
servicenow.client.connection.pool.keep.alive.duration.seconds | 60 | The amount of time to hold onto idle connections in the connection pool.


#### Connector Subtask Configuration

Subtask Implementation: `com.ibm.ingestion.connect.servicenow.source.TableAPISubTask`

Configuration  | Default | Notes
:------------- | :------------ | :------------
task.poll.batch.max.size | 20 | The maximum number of records to retrieve from ServiceNow per call. This setting applies to each SubTask individually. For instance, if you have `two tables` being watched, and a `max batch of 20`, then for each polling cycle the connector task will have at most `20 * 2 = 40` records in memory before publishing to the target stream.
task.poll.fast.interval.ms | 500 | The amount of time between http requests to ServiceNow when the most recent call returned data. This setting combined with the max batch size setting directly translates to the maximum possible throughput for this connector.
task.poll.slow.interval.ms | 30000 | The amount of time between http requests to ServiceNow when the most recent call returned no data. This setting directly translates to the how long it takes for a change in ServiceNow to be surfaced in target stream.
timestamp.initial.query.hours.ago | none | When a subtask fires up and does not have an existing offset to start from, this setting determines the date from which it will start pulling records. When this setting is excluded, the task starts with the earliest timestamp available in the `source table`.
through.timestamp.delay.interval.seconds | 0 | The amount of time between when a record in the source table is updated, and when it will be picked up by the connector query. For example, if this setting is 10 seconds, then an updated record will "cooldown" for at least 10 seconds before the connector will pick it up. This is useful if one is consuming from multiple tables that have relationships and wants to provide time for any source transactions to complete.
table.whitelist.`<table config id>`.name | none | The name of the source table in ServiceNow TableAPI.
table.whitelist.`<table config id>`.timestamp.field.name | none | The name of the column in the source table pertaining to the last updated time for each record.
table.whitelist.`<table config id>`.identifier.field.name | none | The name of the column in the source table uniquely identifying the record.
table.whitelist.`<table config id>`.fields | none | A comma delimited list of fields or columns to pull from the source table. By default all available fields or columns are retrieved.
table.whitelist.`<table config id>`.partition.type | none | The partitioning type to use when selecting destination kafka topic partitions for records. See the readme section about partitioning types. When this setting is excluded, the `default` partition type is used.
table.whitelist.`<table config id>`.partition.fields | none | Only valid for partitioning-type of `field-based`. This setting determines the fields on the `source table` to use as the partitioning key for selecting destination kafka topic partitions for records.


```$json
{
    "name": "milz-servicenow-connector",
    "config": {
        "servicenow.client.base.uri": "https://ibmmhasdev4.service-now.com",
        "servicenow.client.oauth.clientid": "clientid",
        "servicenow.client.oauth.clientsecret": "clientsecret",
        "servicenow.client.oauth.username": "someusername",
        "servicenow.client.oauth.userpassword": "someuserpassword",
        "table.whitelist": "case",
        "table.whitelist.case.name": "sn_customerservice_case",
        "table.whitelist.case.timestamp.field.name": "sys_updated_on",
        "table.whitelist.case.identifier.field.name": "number",
        "table.whitelist.case.fields": "number,sys_updated_on",
        "table.whitelist.case.partition.type": "default",
        "table.whitelist.changerequest.name": "change_request",
        "table.whitelist.changerequest.timestamp.field.name": "sys_updated_on",
        "table.whitelist.changerequest.identifier.field.name": "number",
        "table.whitelist.changerequest.partition.type": "field-based",
        "table.whitelist.changerequest.partition.fields": "number",
        "topic.prefix": "test.milz.servicenowconnector1",
        "timestamp.initial.query.hours.ago": 720,
        "tasks.max": 1,
        "connector.class": "com.ibm.ingestion.connect.servicenow.ServiceNowSourceConnector"
    }
}
```

#### How to Build

This connector has a gradle configuration file. You can create a bundled JAR with the following gradle command:
```
[millies:~/Documents/ibm-github/cmas-portals/servicenow-connector-repos/servicenow-connector]$ ./gradlew shadowJar
[millies:~/Documents/ibm-github/cmas-portals/servicenow-connector-repos/servicenow-connector]$ ls ./build/libs
servicenow-connector-1.0-SNAPSHOT-all.jar
[millies:~/Documents/ibm-github/cmas-portals/servicenow-connector-repos/servicenow-connector]$

```
