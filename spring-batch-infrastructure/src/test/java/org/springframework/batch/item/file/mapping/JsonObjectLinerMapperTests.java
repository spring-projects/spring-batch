package org.springframework.batch.item.file.mapping;

import com.fasterxml.jackson.core.JsonParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonObjectLinerMapperTests {
    private JsonObjectLineMapper<JsonTestType> mapper = new JsonObjectLineMapper<>(JsonTestType.class);

    @Test
    public void testMapLine() throws Exception {
        JsonTestType map = mapper.mapLine("{\"age\": 1, \"name\": \"name\"}", 1);
        assertEquals(map.getAge(), 1);
        assertEquals(map.getName(), "name");
    }

    @Test(expected = JsonParseException.class)
    public void testMappingError() throws Exception {
        JsonTestType map = mapper.mapLine("{\"age\": 1, \"name\": \"name\"", 1);
        assertEquals(map.getAge(), 1);
    }
}
