package org.springframework.batch.item.file;

import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;

import java.io.BufferedReader;

import static org.junit.Assert.assertEquals;

public class JsonBufferedReaderFactoryTests {
    @Test
    public void testCreate() throws Exception {
        JsonBufferedReaderFactory factory = new JsonBufferedReaderFactory();
        @SuppressWarnings("resource")
        BufferedReader reader = factory.create(new ByteArrayResource("[{\"age\":1,\"name\":\"name\"},{\"age\":1,\"name\":\"name\"}]".getBytes()), "UTF-8");
        assertEquals("{\"age\":1,\"name\":\"name\"}", reader.readLine());
    }
}
