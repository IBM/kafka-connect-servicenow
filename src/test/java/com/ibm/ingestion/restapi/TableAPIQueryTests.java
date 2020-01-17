package com.ibm.ingestion.restapi;
import com.ibm.ingestion.http.TableAPIQueryBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class TableAPIQueryTests {

    public TableAPIQueryTests() {
    }

    @Test
    public void testTrivialEqualsQuery() {

        final String FIELD_ID = "company";
        final String FIELD_VALUE = "1111111111";
        final String EXPECTED_QUERY = String.format("%s=%s", FIELD_ID, FIELD_VALUE);

        TableAPIQueryBuilder builder = TableAPIQueryBuilder.Builder();
        builder
                .whereEquals(FIELD_ID, FIELD_VALUE);

        assertEquals(EXPECTED_QUERY, builder.build());
    }
}
