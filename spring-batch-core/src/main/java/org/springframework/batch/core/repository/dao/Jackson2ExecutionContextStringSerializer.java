/*
 * Copyright 2008-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.repository.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.util.Assert;

/**
 * Implementation that uses Jackson2 to provide (de)serialization.
 *
 * @author Marten Deinum
 * @since 3.0.7
 *
 * @see ExecutionContextSerializer
 */
public class Jackson2ExecutionContextStringSerializer implements ExecutionContextSerializer {

    private ObjectMapper objectMapper;

    public Jackson2ExecutionContextStringSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        this.objectMapper.enableDefaultTyping();
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> deserialize(InputStream in) throws IOException {

        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
        return objectMapper.readValue(in, typeRef);
    }

    public void serialize(Map<String, Object> context, OutputStream out) throws IOException {

        Assert.notNull(context, "A context is required");
        Assert.notNull(out, "An OutputStream is required");

        objectMapper.writeValue(out, context);
    }
}
