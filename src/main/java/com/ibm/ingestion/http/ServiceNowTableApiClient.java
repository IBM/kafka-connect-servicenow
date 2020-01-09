package com.ibm.ingestion.http;

import com.ibm.ingestion.connect.servicenow.source.ServiceNowSourceConnectorConfig;
import okhttp3.*;

import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ServiceNowTableApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceNowTableApiClient.class);

    private static class Urls {
        public static final String DEFAULT_TABLEAPI_PATH = "/api/now/table";
        public static final String LOGIN = "/oauth_token.do";
    }

    private ServiceNowSourceConnectorConfig _config;
    private OkHttpClient _okHttpClient;

    private final int UNBOUNDED_NUMBER_OF_RETRIES = -1;
    private int MAX_RETRIES;
    private int RETRY_BACKOFF_MS;

    /**
     * EXAMPLE: https://ibmmhasdev2.service-now.com
     */

    private static class ServiceNowParams {
        /**
         * Limits the number of results returned. Used in conjunction with OFFSET for paging.
         */
        public static final String LIMIT = "sysparm_limit";

        /**
         * The offset to start at when retrieving records. Used in conjunction with LIMIT for paging.
         */
        public static final String OFFSET = "sysparm_offset";

        /**
         * The query to perform in service now query syntax. Use TableAPIQueryBuilder to help make queries.
         */
        public static final String QUERY = "sysparm_query";
        public static final String EXCLUDE_REFERENCES = "sysparm_exclude_reference_link";
        /**
         * Comma separated list of fields to return.
         */
        public static final String FIELDS = "sysparm_fields";
    }

    private JSONObject _currentJwtToken;

    public void init() throws IOException {

        final int CONNECTION_TIMEOUT_SECONDS = this.getConfigOrDefault(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_CONNECTION_TIMEOUT_SECONDS, 10);
        final int CALL_TIMEOUT_SECONDS = this.getConfigOrDefault(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_TIMEOUT_SECONDS, 30); // Disabled by default. Read/Write timeouts will be respected.
        final int READ_TIMEOUT_SECONDS = this.getConfigOrDefault(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_READ_TIMEOUT_SECONDS, 25);
        final int MAX_IDLE_CONNECTIONS = this.getConfigOrDefault(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_MAX_IDLE_CONNECTIONS, 2);
        final int KEEP_ALIVE_DURATION_SECONDS = this.getConfigOrDefault(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_KEEP_ALIVE_DURATION_SECONDS, 30);

        this._okHttpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION_SECONDS, TimeUnit.SECONDS))
                .connectTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        this.refreshAuthenticationToken();

        this.MAX_RETRIES = this.getRequiredConfigInt(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_MAX_RETRIES);
        this.RETRY_BACKOFF_MS = this.getRequiredConfigInt(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_RETRY_BACKOFF_SECONDS) * 1000;
    }

    public void close() {
        try {
            this._okHttpClient.connectionPool().evictAll();
        } catch(Exception ex) {
            LOG.debug("Error while cleaning up connection pool. [{}]", ex);
        }
    }

    public List<JSONObject> getRecords(String table, TableAPIQueryBuilder query) throws InterruptedException, IOException {
        return this.getRecords(table, query, 0, 1);
    }

    public List<JSONObject> getRecords(String table, TableAPIQueryBuilder query, long offset, int limit) throws InterruptedException, IOException {
        return this.getRecords(table, query, offset, limit, true, null);
    }

    public List<JSONObject> getRecords(String table, TableAPIQueryBuilder query, long offset, int limit, List<String> fields) throws InterruptedException, IOException {
        return this.getRecords(table, query, offset, limit, true, fields);
    }

    public List<JSONObject> getRecords(String table, TableAPIQueryBuilder query, long offset, int limit, boolean excludeReferenceLink, List<String> fields) throws InterruptedException, IOException {

        final StringBuilder requestUrl = this.getBaseUri();
        requestUrl.append(Urls.DEFAULT_TABLEAPI_PATH);
        requestUrl.append("/" + table.replaceAll("^/+", ""));

        List<String> queryArgs = new ArrayList<String>();
        queryArgs.add(String.format("%s=%s", ServiceNowParams.EXCLUDE_REFERENCES, excludeReferenceLink));

        if(offset != Long.MIN_VALUE) {
            queryArgs.add(String.format("%s=%s", ServiceNowParams.OFFSET, offset));
        }

        if(limit != Integer.MIN_VALUE) {
            queryArgs.add(String.format("%s=%s", ServiceNowParams.LIMIT, limit));
        }

        if(query == null) {
            throw new RuntimeException("A query is required even if it is empty.");
        }

        try {
            queryArgs.add(String.format("%s=%s", ServiceNowParams.QUERY, URLEncoder.encode(query.build(), "UTF-8")));
        } catch(UnsupportedEncodingException ex) {
            throw new ConnectException(String.format("The following error occurred while trying to encode the following query string. [%s]", query.build()), ex);
        }


        if(fields != null) {
            queryArgs.add(String.format("%s=%s", ServiceNowParams.FIELDS, String.join(",", fields)));
        }

        requestUrl.append("?");
        requestUrl.append(String.join("&", queryArgs));

        Request.Builder req = new Request.Builder()
                .url(requestUrl.toString())
                .addHeader("Accept", "application/json")
                .get();

        return this.sendWithRetry(req);
    }

    /**
     * Guaranteed to throw exception if it cannot get a successful response.
     * @param requestBuilder
     * @return
     * @throws InterruptedException
     */
    private List<JSONObject> sendWithRetry(Request.Builder requestBuilder) throws InterruptedException {

        List<JSONObject> result = null;
        final String AUTHORIZATION_HEADER = "Authorization";
        int remainingRetries = this.MAX_RETRIES;
        while(this.MAX_RETRIES == this.UNBOUNDED_NUMBER_OF_RETRIES || remainingRetries-- > 0) {

            Response candidate = null;
            Request request = requestBuilder
                    .removeHeader(AUTHORIZATION_HEADER)
                    .addHeader(AUTHORIZATION_HEADER, String.format("Bearer %s", this.getAccessToken()))
                    .build();

            try {
                candidate = this._okHttpClient.newCall(request).execute();
            }
            catch(IOException ex) {
                LOG.error("The following error occurred while sending request {}. {}", request, ex);
            }

            // NOTE(millies): handling a failed response and trying to log as much info as we can.
            if(candidate == null || !candidate.isSuccessful()) {
                if(candidate != null) {
                    ResponseBody body = candidate.body();
                    String bodyContents = "";
                    try {
                        if(body != null) {
                            bodyContents = body.string();
                            body.close();
                        }
                    } catch(Exception ignored) {
                        // ignored.
                    }

                    LOG.error("Request {} failed with code {} and the following body. {}", request, candidate.code(), bodyContents);
                    if(candidate.code() == 401) {
                        LOG.info("Received 401 from server, attempting to refresh authentication token.");
                        try {
                            this.refreshAuthenticationToken();
                        } catch(Exception ex) {
                            LOG.error("Failed to refresh authentication token: {}", ex);
                        }
                    }
                }
            } else {
                // NOTE(millies): Received a successful response attempt parsing.
                try {
                    result = parseJSONBody(candidate);
                    // NOTE(millies): Successful parsing.
                    break;
                } catch(IOException ex) {
                    LOG.error("Failed parsing response with error [{}].", ex);
                }
            }

            LOG.info("Retrying in [{}] ms. [{}] retries remaining.", this.RETRY_BACKOFF_MS, remainingRetries);

            // NOTE(millies): as a result of a failure on the response, we're going to wait a little bit before
            // retrying.
            Thread.sleep(this.RETRY_BACKOFF_MS);
        }

        if(result == null) {
            throw new ConnectException("Failed the maximum number of retries while making request. Bailing.");
        }

        return result;
    }

    private List<JSONObject>  parseJSONBody(Response response) throws IOException {
        JSONObject page = null;
        ResponseBody body = null;
        try {
            body = response.body();
            if(body != null) {
                page = new JSONObject(body.string());
            }
        } finally {
            if(body != null) {
                body.close();
            }
        }

        List<JSONObject> results = new ArrayList<JSONObject>();
        if(page != null) {
            JSONArray records = page.optJSONArray("result");
            if(records != null) {
                for(int i = 0; i < records.length() ;i++) {
                    results.add(records.getJSONObject(i));
                }
            } else {
                // NOTE(millies): put this here so we have visibility of this situation.
                LOG.error("Received the following body that could not be parsed. {}", page);
            }
        } else {
            // NOTE(millies): put this here so we have visibility of this situation.
            LOG.error("Page ended up being null during query of table.");
        }

        return results;
    }

    private String getAccessToken() {
        if(this._currentJwtToken != null) {
            return this._currentJwtToken.getString("access_token");
        }

        return null;
    }

    private String getRefreshToken() {
        if(this._currentJwtToken != null) {
            return this._currentJwtToken.getString("refresh_token");
        }

        return null;
    }

    public ServiceNowTableApiClient(ServiceNowSourceConnectorConfig config) {
        this._config = config;
    }

    private String getRequiredConfig(String key) {
        final String candidate = this._config.getString(key);
        if(candidate == null || candidate.trim().length() == 0) {
            throw new RuntimeException(String.format("Missing [%s] configuration.", key));
        }

        return candidate;
    }

    private int getRequiredConfigInt(String key) {
        return this._config.getInt(key);
    }

    private String getConfigOrDefault(String key, String defaultValue) {
        final String candidate = this._config.getString(key);
        if(candidate == null || candidate.trim().length() == 0) {
            return defaultValue;
        }

        return candidate;
    }

    private Integer getConfigOrDefault(String key, Integer defaultValue) {
        try {
            return this._config.getInt(key);
        } catch(ConfigException ex) {
            return defaultValue;
        }
    }

    private StringBuilder getBaseUri() {
        final String baseUrl = this.getRequiredConfig(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_BASEURI);
        // NOTE(millies): starting the url with ensuring the baseurl doesn't have a trailing slash.
        return new StringBuilder(baseUrl.replaceAll("/+$", ""));
    }

    private JSONObject getAuthenticationToken() throws IOException {

        final String GRANT_TYPE_KEY = "grant_type";
        final String CLIENTID_KEY = "client_id";
        final String CLIENTSECRET_KEY = "client_secret";
        final String USERNAME_KEY = "username";
        final String USERPASSWORD_KEY = "password";

        final String clientId = this.getRequiredConfig(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_OAUTH_CLIENTID);
        final String clientSecret = this.getRequiredConfig(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_OAUTH_CLIENTSECRET);
        final String userName = this.getRequiredConfig(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_OAUTH_USERNAME);
        final String userPassword = this.getRequiredConfig(ServiceNowSourceConnectorConfig.SERVICENOW_CLIENT_OAUTH_USERPASSWORD);

        final StringBuilder requestUrl = this.getBaseUri();
        requestUrl.append(Urls.LOGIN);

        RequestBody form = new FormBody.Builder()
                .add(GRANT_TYPE_KEY, "password")
                .add(CLIENTID_KEY, clientId)
                .add(CLIENTSECRET_KEY, clientSecret)
                .add(USERNAME_KEY, userName)
                .add(USERPASSWORD_KEY, userPassword)
                .build();

        Request req = new Request.Builder()
                .url(requestUrl.toString())
                .post(form)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        Response response = this._okHttpClient.newCall(req).execute();
        JSONObject authenticationInfo = null;
        try {
            if(!response.isSuccessful()) {
                throw new ConnectException(String.format("Received [%s] [%s]", response.code(), response.body().string()));
            }

            authenticationInfo = new JSONObject(response.body().string());
        } finally {
            if(response != null) {
                response.close();
            }
        }

        return authenticationInfo;
    }

    public static String toLogFormat(Exception ex) {
        StringBuilder sb = new StringBuilder();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String message = ex.getMessage();
        if(message == null) {
            // NOTE(millies): sometimes exceptions don't have a message.
            message = ex.toString();
        }
        sb.append(message);
        sb.append(":");
        sb.append(sw.toString());
        return sb.toString();
    }

    private void refreshAuthenticationToken() throws JSONException, IOException {
        this._currentJwtToken = this.getAuthenticationToken();
    }
}
