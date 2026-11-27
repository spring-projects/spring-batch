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
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.ExecutionContextSerializer;

/**
 * An {@link ExecutionContextSerializer} that uses Jackson 3 to serialize/deserialize the
 * execution context as JSON. By default, this serializer enables default typing with a
 * {@link BasicPolymorphicTypeValidator} that allows only classes from certain packages to
 * be deserialized, for security reasons. If you need a different configuration, you can
 * provide your own {@link JsonMapper} instance through the constructor.
 *
 * @author Mahmoud Ben Hassine
 * @author Soonjae Jung
 * @author Yanming Zhou
 * @since 6.0.0
 */
public class JacksonExecutionContextStringSerializer implements ExecutionContextSerializer {

	private static final List<String> TRUSTED_TYPE_PREFIXES = List.of("java.util.", "java.sql.", "java.lang.",
			"java.math.", "java.time.", "java.net.", "javax.xml.namespace.QName", "org.springframework.batch.");

	private static final PolymorphicTypeValidator polymorphicTypeValidator = createPolymorphicTypeValidator();

	private final JsonMapper jsonMapper;

	/**
	 * Create a new {@link JacksonExecutionContextStringSerializer} with default
	 * configuration (only classes from certain packages will be allowed to be
	 * deserialized).
	 */
	public JacksonExecutionContextStringSerializer() {
		this(JsonMapper.builder());
	}

	/**
	 * Create a new {@link JacksonExecutionContextStringSerializer} with a custom
	 * {@link JsonMapper.Builder}.
	 * @param jsonMapperBuilder the {@link JsonMapper.Builder} to use for
	 * serialization/deserialization
	 */
	public JacksonExecutionContextStringSerializer(JsonMapper.Builder jsonMapperBuilder) {
		this(jsonMapperBuilder.activateDefaultTyping(polymorphicTypeValidator)
			.addMixIns(Map.of(JobParameters.class, JobParametersMixIn.class))
			.build());
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

	private static PolymorphicTypeValidator createPolymorphicTypeValidator() {
		BasicPolymorphicTypeValidator.Builder builder = BasicPolymorphicTypeValidator.builder();
		TRUSTED_TYPE_PREFIXES.forEach(builder::allowIfSubType);
		// the type id of an array starts with '[', so arrays are not matched by the
		// prefixes above: allow them only when their component type is trusted
		builder.allowIfSubType((context, type) -> isTrustedArrayType(type));
		return builder.build();
	}

	private static boolean isTrustedArrayType(Class<?> type) {
		if (!type.isArray()) {
			return false;
		}
		Class<?> componentType = type.getComponentType();
		while (componentType.isArray()) {
			componentType = componentType.getComponentType();
		}
		String componentTypeName = componentType.getName();
		return componentType.isPrimitive() || TRUSTED_TYPE_PREFIXES.stream().anyMatch(componentTypeName::startsWith);
	}

	@SuppressWarnings("unused")
	private abstract static class JobParametersMixIn {

		@JsonIgnore
		abstract boolean isEmpty();

		@JsonIgnore
		abstract Map<String, JobParameter<?>> getIdentifyingParameters();

	}

}
