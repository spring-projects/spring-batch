/*
 * Copyright 2022 the original author or authors.
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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.util.Assert;

/**
 * This class is an implementation of {@link ExecutionContextSerializer}
 * based on <a href="https://github.com/google/gson">Google Gson</a> library.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class GsonExecutionContextStringSerializer implements ExecutionContextSerializer {

    private Gson gson;

    /**
     * Create a new {@link GsonExecutionContextStringSerializer}.
     *
     * @param gson the Gson instance to use
     */
    public GsonExecutionContextStringSerializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Map<String, Object> deserialize(InputStream inputStream) throws IOException {
        Assert.notNull(inputStream, "An InputStream is required");
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        try (JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream))) {
            return this.gson.fromJson(jsonReader, mapType);
        }
    }

    @Override
    public void serialize(Map<String, Object> context, OutputStream outputStream) throws IOException {
        Assert.notNull(context, "A context is required");
        Assert.notNull(outputStream, "An OutputStream is required");
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        try (JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream))) {
            this.gson.toJson(context, mapType, jsonWriter);
        }
    }

}
