package com.ibm.ingestion.restapi;

import com.ibm.ingestion.connect.servicenow.source.*;
import com.ibm.ingestion.http.ServiceNowTableApiClient;
import com.ibm.ingestion.http.TableAPIQueryBuilder;
import org.apache.kafka.connect.source.SourceRecord;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestApiSourceTests {

    public RestApiSourceTests() {

    }

//    @Test
//    public void testGetIncidents() {
//
//        Map<String, String> config = new HashMap<String, String>();
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_BASEURI, "https://ibmmhasdev2.service-now.com");
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_OAUTH_CLIENTID, "");
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_OAUTH_CLIENTSECRET, "");
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_OAUTH_USERNAME, "");
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_OAUTH_USERPASSWORD, "");
//
//        ServiceNowTableApiClient client = new ServiceNowTableApiClient(config);
//        try {
//            client.init();
//
//            TableAPIQueryBuilder builder = new TableAPIQueryBuilder();
//            builder
//                    .whereEquals("company", "8ef6a495dbec0b00366ffa5aaf961912");
//            final List<JSONObject> results = client.getRecords(ServiceNowTableApiClient.Tables.INCIDENT, builder);
//            Assert.assertEquals(1, 1);
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    @Test
//    public void testTableCrawler() throws IOException {
//        Map<String, String> config = new HashMap<String, String>();
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_BASEURI, "https://ibmmhasdev2.service-now.com");
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_OAUTH_CLIENTID, "");
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_OAUTH_CLIENTSECRET, "");
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_OAUTH_USERNAME, "");
//        config.put(ServiceNowTableApiClient.OPTIONS_SERVICENOW_OAUTH_USERPASSWORD, "");
//
//        TableCrawler crawler = new TableCrawler(config);
//        crawler.init();
//
//        Instant since = Instant.ofEpochSecond(1556902730L);
//
//        long offset = 0;
//        int limit = 10;
//
//        List<JSONObject> records = crawler.getRecordsSince(since, offset, limit);
//
//        Assert.assertTrue(!records.isEmpty());
//    }


    public void testTableQueryTimestampStrategy() throws IOException {
//        Map<String, String> props = new HashMap<String, String>();
//        props.put(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_BASEURI, "https://ibmmhasdev2.service-now.com");
//        props.put(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_OAUTH_CLIENTID, "");
//        props.put(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_OAUTH_CLIENTSECRET, "");
//        props.put(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_OAUTH_USERNAME, "");
//        props.put(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_OAUTH_USERPASSWORD, "");
//        props.put(ServiceNowSourceConnectorConfig.TABLE_WHITELIST, "incident");
//        props.put(ServiceNowSourceConnectorConfig.TIMESTAMP_FIELD_NAME, "sys_updated_on");
//        props.put(ServiceNowSourceConnectorConfig.STREAM_PREFIX, "milz.connect.servicenow.test");
//
//        // Task level config
//        props.put(ServiceNowTableAPISourceTaskConfig.TABLE_LIST, "incident");
//
//        ServiceNowSourceConnectorConfig config = new ServiceNowSourceConnectorConfig(props);
//        ServiceNowTableApiClient client = new ServiceNowTableApiClient(props);
//        client.init();
//
//        LocalDateTime lastSeenDateTime = LocalDateTime.now(ZoneOffset.UTC).minusDays(30);
//        Map<String, Object> offsetMap = TimestampSourceOffset.toMap(lastSeenDateTime.toInstant(ZoneOffset.UTC), 0);
//
//        TableQueryTimestampStrategy strat = new TableQueryTimestampStrategy(
//                new ServiceNowTableAPISourceTaskConfig(props),
//                new TableQueryPartition("incident"),
//                "sys_updated_on",
//                offsetMap,
//                client);
//
//        List<SourceRecord> results = strat.poll();
//        Assert.assertTrue(!results.isEmpty());
//
//
//        results = strat.poll();
//        Assert.assertTrue(!results.isEmpty());
    }


    public void testQueryBuilder() {
        TableAPIQueryBuilder builder = TableAPIQueryBuilder.Builder();
        builder
                .whereEquals("company", "12345")
                .orWhereEquals("company", "54^321")
                .whereGreaterThan("time", "123")
                .whereGreaterThanOrEqual("time2", "1234")
                .whereNotEquals("notthis", "somevalue");

        System.out.print(builder.build());
    }


}
