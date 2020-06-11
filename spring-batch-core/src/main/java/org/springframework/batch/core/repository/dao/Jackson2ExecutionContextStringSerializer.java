/*
 * Copyright 2008-2020 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * Implementation that uses Jackson2 to provide (de)serialization. 
 * 
 * By default, this implementation trusts a limited set of classes to be
 * deserialized from the execution context. If a class is not trusted by default
 * and is safe to deserialize, you can provide an explicit mapping using Jackson
 * annotations, as shown in the following example:
 * 
 * <pre class="code">
 *     &#064;JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
 *     public class MyTrustedType implements Serializable {
 *        
 *     }
 * </pre>
 * 
 * It is also possible to provide a custom {@link ObjectMapper} with a mixin for
 * the trusted type:
 *
 * <pre class="code">
 *     ObjectMapper objectMapper = new ObjectMapper();
 *     objectMapper.addMixIn(MyTrustedType.class, Object.class);
 *     Jackson2ExecutionContextStringSerializer serializer = new Jackson2ExecutionContextStringSerializer();
 *     serializer.setObjectMapper(objectMapper);
 *     // register serializer in JobRepositoryFactoryBean
 * </pre>
 * 
 * If the (de)serialization is only done by a trusted source, you can also enable
 * default typing:
 *
 * <pre class="code">
 *     PolymorphicTypeValidator polymorphicTypeValidator = .. // configure your trusted PolymorphicTypeValidator
 *     ObjectMapper objectMapper = new ObjectMapper();
 *     objectMapper.activateDefaultTyping(polymorphicTypeValidator); 
 *     Jackson2ExecutionContextStringSerializer serializer = new Jackson2ExecutionContextStringSerializer();
 *     serializer.setObjectMapper(objectMapper);
 *     // register serializer in JobRepositoryFactoryBean
 * </pre>
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
        this.objectMapper.setDefaultTyping(createTrustedDefaultTyping());
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

    /**
     * Creates a TypeResolverBuilder that checks if a type is trusted.
     * @return a TypeResolverBuilder that checks if a type is trusted.
     */
    private static TypeResolverBuilder<? extends TypeResolverBuilder> createTrustedDefaultTyping() {
        TypeResolverBuilder<? extends TypeResolverBuilder>  result = new TrustedTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL);
        result = result.init(JsonTypeInfo.Id.CLASS, null);
        result = result.inclusion(JsonTypeInfo.As.PROPERTY);
        return result;
    }

    /**
     * An implementation of {@link ObjectMapper.DefaultTypeResolverBuilder}
     * that inserts an {@code allow all} {@link PolymorphicTypeValidator}
     * and overrides the {@code TypeIdResolver}
     * @author Rob Winch
     */
    static class TrustedTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {

        TrustedTypeResolverBuilder(ObjectMapper.DefaultTyping defaultTyping) {
            super(
                    defaultTyping,
                    //we do explicit validation in the TypeIdResolver
                    BasicPolymorphicTypeValidator.builder()
                            .allowIfSubType(Object.class)
                            .build()
            );
        }

        @Override
        protected TypeIdResolver idResolver(MapperConfig<?> config,
                                            JavaType baseType,
                                            PolymorphicTypeValidator subtypeValidator,
                                            Collection<NamedType> subtypes, boolean forSer, boolean forDeser) {
            TypeIdResolver result = super.idResolver(config, baseType, subtypeValidator, subtypes, forSer, forDeser);
            return new TrustedTypeIdResolver(result);
        }
    }

    /**
     * A {@link TypeIdResolver} that delegates to an existing implementation and throws an IllegalStateException if the
     * class being looked up is not trusted, does not provide an explicit mixin, and is not annotated with Jackson
     * mappings.
     */
    static class TrustedTypeIdResolver implements TypeIdResolver {
        private static final Set<String> TRUSTED_CLASS_NAMES = Collections.unmodifiableSet(new HashSet(Arrays.asList(
                "java.util.ArrayList",
                "java.util.LinkedList",
                "java.util.Collections$EmptyList",
                "java.util.Collections$EmptyMap",
                "java.util.Collections$EmptySet",
                "java.util.Collections$UnmodifiableRandomAccessList",
                "java.util.Collections$UnmodifiableList",
                "java.util.Collections$UnmodifiableMap",
                "java.util.Collections$UnmodifiableSet",
                "java.util.Collections$SingletonList",
                "java.util.Collections$SingletonMap",
                "java.util.Collections$SingletonSet",
                "java.util.Date",
                "java.time.Instant",
                "java.time.Duration",
                "java.time.LocalDate",
                "java.time.LocalTime",
                "java.time.LocalDateTime",
                "java.net.URL",
                "java.util.TreeMap",
                "java.util.HashMap",
                "java.util.LinkedHashMap",
                "java.util.TreeSet",
                "java.util.HashSet",
                "java.util.LinkedHashSet",
                "java.lang.Boolean",
                "java.lang.Byte",
                "java.lang.Short",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Double",
                "java.lang.Float",
                "java.math.BigDecimal",
                "java.math.BigInteger",
                "java.lang.String",
                "java.lang.Character",
                "java.lang.CharSequence",
                "java.util.Properties",
                "[Ljava.util.Properties;",
                "org.springframework.batch.core.JobParameter",
                "org.springframework.batch.core.JobParameters",
                "org.springframework.batch.core.jsr.partition.JsrPartitionHandler$PartitionPlanState"
        )));

        private final TypeIdResolver delegate;

        TrustedTypeIdResolver(TypeIdResolver delegate) {
            this.delegate = delegate;
        }

        @Override
        public void init(JavaType baseType) {
            delegate.init(baseType);
        }

        @Override
        public String idFromValue(Object value) {
            return delegate.idFromValue(value);
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            return delegate.idFromValueAndType(value, suggestedType);
        }

        @Override
        public String idFromBaseType() {
            return delegate.idFromBaseType();
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) throws IOException {
            DeserializationConfig config = (DeserializationConfig) context.getConfig();
            JavaType result = delegate.typeFromId(context, id);
            String className = result.getRawClass().getName();
            if (isTrusted(className)) {
                return result;
            }
            boolean isExplicitMixin = config.findMixInClassFor(result.getRawClass()) != null;
            if (isExplicitMixin) {
                return result;
            }
            Class<?> rawClass = result.getRawClass();
            JacksonAnnotation jacksonAnnotation = AnnotationUtils.findAnnotation(rawClass, JacksonAnnotation.class);
            if (jacksonAnnotation != null) {
                return result;
            }
            throw new IllegalArgumentException("The class with " + id + " and name of " + className + " is not trusted. " +
                    "If you believe this class is safe to deserialize, please provide an explicit mapping using Jackson annotations or a custom ObjectMapper. " +
                    "If the serialization is only done by a trusted source, you can also enable default typing.");
        }

        private boolean isTrusted(String id) {
            return TRUSTED_CLASS_NAMES.contains(id);
        }

        @Override
        public String getDescForKnownTypeIds() {
            return delegate.getDescForKnownTypeIds();
        }

        @Override
        public JsonTypeInfo.Id getMechanism() {
            return delegate.getMechanism();
        }

    }

}
