/*
 * Copyright 2008-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.util.Assert;

/**
 * Implementation that uses Jackson2 to provide (de)serialization.
 *
 * @author Marten Deinum
 * @author Mahmoud Ben Hassine
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
        this.objectMapper.registerModule(new JobParametersModule());
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        this.objectMapper = objectMapper.copy();
        this.objectMapper.registerModule(new JobParametersModule());
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

    // BATCH-2680
    /**
     * Custom Jackson module to support {@link JobParameter} and {@link JobParameters}
     * deserialization.
     */
    private class JobParametersModule extends SimpleModule {

        private static final long serialVersionUID = 1L;

        private JobParametersModule() {
            super("Job parameters module");
            setMixInAnnotation(JobParameters.class, JobParametersMixIn.class);
            addDeserializer(JobParameter.class, new JobParameterDeserializer());
        }

        private abstract class JobParametersMixIn {
            @JsonIgnore
            abstract boolean isEmpty();
        }

        private class JobParameterDeserializer extends StdDeserializer<JobParameter> {

            private static final long serialVersionUID = 1L;
            private static final String IDENTIFYING_KEY_NAME = "identifying";
            private static final String TYPE_KEY_NAME = "type";
            private static final String VALUE_KEY_NAME = "value";

            JobParameterDeserializer() {
                super(JobParameter.class);
            }

            @Override
            public JobParameter deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                JsonNode node = parser.readValueAsTree();
                boolean identifying = node.get(IDENTIFYING_KEY_NAME).asBoolean();
                String type = node.get(TYPE_KEY_NAME).asText();
                JsonNode value = node.get(VALUE_KEY_NAME);
                Object parameterValue;
                switch (JobParameter.ParameterType.valueOf(type)) {
                    case STRING: {
                        parameterValue = value.asText();
                        return new JobParameter((String) parameterValue, identifying);
                    }
                    case DATE: {
                        parameterValue = new Date(value.get(1).asLong());
                        return new JobParameter((Date) parameterValue, identifying);
                    }
                    case LONG: {
                        parameterValue = value.get(1).asLong();
                        return new JobParameter((Long) parameterValue, identifying);
                    }
                    case DOUBLE: {
                        parameterValue = value.asDouble();
                        return new JobParameter((Double) parameterValue, identifying);
                    }
                }
                return null;
            }
        }

    }

}
