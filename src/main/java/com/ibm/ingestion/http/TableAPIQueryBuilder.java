package com.ibm.ingestion.http;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TableAPIQueryBuilder {

    private static class ServiceNowQuerySyntax {
        public static final String AND = "^";
        public static final String OR = "^OR";
        public static final String NEW_QUERY = "^NQ";
        public static final String IS = "=";
        public static final String IS_NOT = "!=";
        public static final String STARTS_WITH = "STARTSWITH";
        public static final String LIKE = "LIKE";
        public static final String NOT_LIKE = "NOTLIKE";
        public static final String IS_ANYTHING = "ANYTHING";
        public static final String IS_EMPTY_STRING = "EMPTYSTRING";
        public static final String IS_EMPTY = "ISEMPTY";
        public static final String IS_NOT_EMPTY = "ISNOTEMPTY";
        public static final String GREATER_THAN_OR_EQUAL = ">=";
        public static final String LESS_THAN_OR_EQUAL = "<=";
        public static final String LESS_THAN = "<";
        public static final String GREATER_THAN = ">";
        public static final String ORDERBY_ASC = "ORDERBY";
        public static final String ORDERBY_DESC = "ORDERBYDESC";
    }

    private StringBuilder _query = new StringBuilder();
    private TableAPIQueryBuilder() {
    }

    public static TableAPIQueryBuilder Builder() {
        return new TableAPIQueryBuilder();
    }

    private String SERVICENOW_QUERY_SEPARATOR_PATTERN = String.format("\\%s", ServiceNowQuerySyntax.AND);
    private String ESCAPED_QUERY_SEPARATOR = String.format("%s%s", ServiceNowQuerySyntax.AND, ServiceNowQuerySyntax.AND);
    private String sanitizeValue(String value) {

        if(value == null || value.trim().length() == 0) {
            return value;
        }

        return value.replaceAll(SERVICENOW_QUERY_SEPARATOR_PATTERN, ESCAPED_QUERY_SEPARATOR);
    }

    /**
     * EXAMPLE: "company=1245^someotherkey=1111"
     * @return
     */
    public String build() {
        return this._query.toString().replaceAll("^\\^", "");
    }

    public TableAPIQueryBuilder whereEquals(String field, String value) {
        value = this.sanitizeValue(value);
        this._query.append(String.format("%s%s%s%s",ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.IS, value));
        return this;
    }

    public TableAPIQueryBuilder orWhereEquals(String field, String value) {
        value = this.sanitizeValue(value);
        this._query.append(String.format("%s%s%s%s",ServiceNowQuerySyntax.OR, field, ServiceNowQuerySyntax.IS, value));
        return this;
    }

    public TableAPIQueryBuilder whereNotEquals(String field, String value) {
        value = this.sanitizeValue(value);
        this._query.append(String.format("%s%s%s%s",ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.IS_NOT, value));
        return this;
    }

    public TableAPIQueryBuilder orWhereNotEquals(String field, String value) {
        value = this.sanitizeValue(value);
        this._query.append(String.format("%s%s%s%s",ServiceNowQuerySyntax.OR, field, ServiceNowQuerySyntax.IS_NOT, value));
        return this;
    }

    public TableAPIQueryBuilder whereGreaterThan(String field, String value) {
        value = this.sanitizeValue(value);
        this._query.append(String.format("%s%s%s%s",ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.GREATER_THAN, value));
        return this;
    }

    public TableAPIQueryBuilder whereGreaterThanOrEqual(String field, String value) {
        value = this.sanitizeValue(value);
        this._query.append(String.format("%s%s%s%s",ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.GREATER_THAN_OR_EQUAL, value));
        return this;
    }

    public TableAPIQueryBuilder whereLessThan(String field, String value) {
        value = this.sanitizeValue(value);
        this._query.append(String.format("%s%s%s%s",ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.LESS_THAN, value));
        return this;
    }

    public TableAPIQueryBuilder whereLessThanOrEqual(String field, String value) {
        value = this.sanitizeValue(value);
        this._query.append(String.format("%s%s%s%s",ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.LESS_THAN_OR_EQUAL, value));
        return this;
    }

    public TableAPIQueryBuilder whereIsAnything(String field) {
        field = this.sanitizeValue(field);
        this._query.append(String.format("%s%s%s", ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.IS_ANYTHING));
        return this;
    }

    public TableAPIQueryBuilder whereIsEmptyString(String field) {
        field = this.sanitizeValue(field);
        this._query.append(String.format("%s%s%s", ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.IS_EMPTY_STRING));
        return this;
    }

    public TableAPIQueryBuilder whereIsEmpty(String field) {
        field = this.sanitizeValue(field);
        this._query.append(String.format("%s%s%s", ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.IS_EMPTY));
        return this;
    }

    public TableAPIQueryBuilder whereIsNotEmpty(String field) {
        field = this.sanitizeValue(field);
        this._query.append(String.format("%s%s%s", ServiceNowQuerySyntax.AND, field, ServiceNowQuerySyntax.IS_NOT_EMPTY));
        return this;
    }

    public TableAPIQueryBuilder orderByAsc(String field) {
        field = this.sanitizeValue(field);
        this._query.append(String.format("%s%s%s", ServiceNowQuerySyntax.AND, ServiceNowQuerySyntax.ORDERBY_ASC, field));
        return this;
    }

    public TableAPIQueryBuilder orderByDesc(String field) {
        field = this.sanitizeValue(field);
        this._query.append(String.format("%s%s%s", ServiceNowQuerySyntax.AND, ServiceNowQuerySyntax.ORDERBY_DESC, field));
        return this;
    }

    public TableAPIQueryBuilder whereTimestampEquals(String field, LocalDateTime timestamp) {
        this.whereEquals(field, toServiceNowDateTime(timestamp));
        return this;
    }

    public TableAPIQueryBuilder whereBetweenInclusive(String field, LocalDateTime from, LocalDateTime through) {
        this.whereGreaterThanOrEqual(field, toServiceNowDateTime(from));
        this.whereLessThanOrEqual(field, toServiceNowDateTime(through));
        return this;
    }

    public TableAPIQueryBuilder whereBetweenExclusive(String field, LocalDateTime from, LocalDateTime through) {
        this.whereGreaterThan(field, toServiceNowDateTime(from));
        this.whereLessThan(field, toServiceNowDateTime(through));
        return this;
    }

    public TableAPIQueryBuilder union(TableAPIQueryBuilder builder) {
        this._query.append(ServiceNowQuerySyntax.NEW_QUERY);
        this._query.append(builder.build());
        return this;
    }

    private static String toServiceNowDateTime(LocalDateTime timestamp) {
        final String fromDate = DateTimeFormatter.ofPattern("yyyy-LL-dd").format(timestamp);
        final String fromTime = DateTimeFormatter.ofPattern("HH:mm:ss").format(timestamp);
        return String.format("javascript:gs.dateGenerate('%s','%s')", fromDate, fromTime);
    }
}
