package org.springframework.batch.item.file;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class JsonBufferedReaderFactory implements BufferedReaderFactory {
    @Override
    public BufferedReader create(Resource resource, String encoding) throws IOException {
        return new JsonBufferedReader(new InputStreamReader(resource.getInputStream(), encoding));
    }

    private final class JsonBufferedReader extends BufferedReader {
        private final ObjectMapper mapper = new ObjectMapper();
        private final JsonFactory factory = mapper.getFactory();
        private final JsonParser parser;
        private ObjectNode node;

        JsonBufferedReader(Reader in) throws IOException {
            super(in);
            parser = factory.createParser(in);
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected an array");
            }
        }

        @Override
        public String readLine() throws IOException {
            JsonToken nextToken = parser.nextToken();
            if (nextToken == JsonToken.START_OBJECT) {
                node = mapper.readTree(parser);
                return node.toString();
            }
            if (nextToken == JsonToken.END_ARRAY) {
                return null;
            }
            throw new IllegalStateException("Expected start of object or end of array of objects");
        }

        @Override
        public void close() throws IOException {
            super.close();
            parser.close();
        }
    }
}