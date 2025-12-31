/*
 * Copyright 2025-present the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import org.springframework.batch.core.repository.ExecutionContextSerializer;

/**
 * An {@link ExecutionContextSerializer} that uses Jackson 3 to serialize/deserialize the
 * execution context as JSON. By default, this serializer enables default typing with a
 * {@link BasicPolymorphicTypeValidator} that allows only classes from certain packages to
 * be deserialized, for security reasons. If you need a different configuration, you can
 * provide your own {@link JsonMapper} instance through the constructor.
 *
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 * @since 6.0.0
 */
public class JacksonExecutionContextStringSerializer implements ExecutionContextSerializer {

	private final JsonMapper jsonMapper;

	/**
	 * Create a new {@link JacksonExecutionContextStringSerializer} with default
	 * configuration (only classes from certain packages will be allowed to be
	 * deserialized).
	 */
	public JacksonExecutionContextStringSerializer() {
		PolymorphicTypeValidator polymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
			.allowIfSubType("java.util.")
			.allowIfSubType("java.sql.")
			.allowIfSubType("java.lang.")
			.allowIfSubType("java.math.")
			.allowIfSubType("java.time.")
			.allowIfSubType("java.net.")
			.allowIfSubType("java.xml.")
			.allowIfSubType("org.springframework.batch.")
			.build();
		this.jsonMapper = JsonMapper.builder()
			.activateDefaultTyping(polymorphicTypeValidator)
			.addMixIns(Map.of(JobParameters.class, JobParametersMixIn.class))
			.build();
	}

	/**
	 * Create a new {@link JacksonExecutionContextStringSerializer} with a custom
	 * {@link JsonMapper}.
	 * @param jsonMapper the {@link JsonMapper} to use for serialization/deserialization
	 */
	public JacksonExecutionContextStringSerializer(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public Map<String, Object> deserialize(InputStream inputStream) throws IOException {
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
		};
		return this.jsonMapper.readValue(inputStream, typeRef);
	}

	@Override
	public void serialize(Map<String, Object> object, OutputStream outputStream) throws IOException {
		this.jsonMapper.writeValue(outputStream, object);
	}

	@SuppressWarnings("unused")
	private abstract static class JobParametersMixIn {

		@JsonIgnore
		abstract boolean isEmpty();

		@JsonIgnore
		abstract Map<String, JobParameter<?>> getIdentifyingParameters();

	}

}
