package org.springframework.batch.item.file.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.item.file.LineMapper;

public class JsonObjectLineMapper<T> implements LineMapper<T> {
    private final Class<T> type;
    private final ObjectMapper om = new ObjectMapper();

    public JsonObjectLineMapper(Class<T> type) {
        this.type = type;
    }

    @Override
    public T mapLine(String line, int lineNumber) throws Exception {
        return om.readValue(line, type);
    }
}
