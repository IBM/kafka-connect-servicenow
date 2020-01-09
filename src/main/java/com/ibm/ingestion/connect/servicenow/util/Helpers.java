package com.ibm.ingestion.connect.servicenow.util;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.json.JSONObject;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Helpers {

    private static DateTimeFormatter ServiceNowDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX");
    public static LocalDateTime parseServiceNowDateTimeUtc(String raw) {
        if(!raw.endsWith("Z")) {
            raw = raw + "Z"; // should consider this as UTC time.
        }

        return LocalDateTime.parse(raw, ServiceNowDateTimeFormat);
    }

    public static List<String> commaDelimitedToList(String commaDelimited) {
        List<String> result = new ArrayList<>();
        if(commaDelimited == null) {
            return result; // empty list.
        }

        for(String field : commaDelimited.trim().split(",")) {
            final String candidate = field.trim();
            if(candidate.length() > 0) {
                result.add(candidate);
            }
        }

        return result;
    }

    public static String underscoresForPeriods(String periods) {
        return periods.replaceAll("\\.", "__");
    }

    public static Struct buildKeyStruct(Schema keySchema, List<String> keyFields, JSONObject record) {
        Struct value = new Struct(keySchema);
        for(String field : keyFields) {
            if(record.isNull(field)) {
                value.put(underscoresForPeriods(field), null);
            } else {
                value.put(underscoresForPeriods(field), record.get(field).toString());
            }
        }

        return value;
    }

    /**
     * Naive mapping of a JSONObject to a Kafka Connect Schema.
     *
     * Assumes the JSONObject is simple in that it is only a set of key-value pairs, no complex fields. Also, assumes
     * all fields are set as strings.
     * @param record
     * @return
     */
    public static Schema buildSchemaFromSimpleJsonRecord(JSONObject record) {

        Iterator<String> keys = record.keys();
        SchemaBuilder builder = SchemaBuilder.struct();
        while(keys.hasNext()) {
            String key = keys.next();
            if(key != null && !key.trim().isEmpty()) {
                // TODO(Millies): Potentially enhance to interrogate the values to determine datatype mapping?
                builder
                        .field(underscoresForPeriods(key), Schema.OPTIONAL_STRING_SCHEMA);
            }
        }
        return builder.build();
    }

    public static Struct buildStruct(Schema schema, JSONObject record) {

        Struct value = new Struct(schema);
        Iterator<String> keys = record.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            if(key != null && !key.trim().isEmpty()) {
                if(record.isNull(key)) {
                    value.put(underscoresForPeriods(key), null);
                } else {
                    // TODO(millies): potentially enhance to interrogate the values to determine data types?
                    value.put(underscoresForPeriods(key), record.get(key).toString());
                }
            }
        }

        return value;
    }

    public static Schema buildSchemaForKey(List<String> fields) {
        SchemaBuilder builder = SchemaBuilder.struct();
        for(String field : fields) {
            if(field != null && !field.trim().isEmpty()) {
                builder.field(underscoresForPeriods(field), Schema.OPTIONAL_STRING_SCHEMA);
            }
        }
        return builder.build();
    }
}
