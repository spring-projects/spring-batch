/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.chunk.listener.ItemProcessListener;
import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.api.chunk.listener.ItemWriteListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

/**
 * <p>
 * {@link BeanPostProcessor} implementation used to inject JSR-352 String properties into batch artifact fields
 * that are marked with the {@link BatchProperty} annotation.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class BatchPropertyBeanPostProcessor implements BeanPostProcessor {
    @Autowired
    private BatchPropertyContext batchPropertyContext;
    private Log logger = LogFactory.getLog(getClass());
    private Set<Class<? extends Annotation>> requiredAnnotations = new HashSet<Class<? extends Annotation>>();

    public BatchPropertyBeanPostProcessor() {
        setRequiredAnnotations();
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
        if (!isBatchArtifact(bean)) {
            return bean;
        }

        final Properties artifactProperties = batchPropertyContext.getBatchProperties(beanName);

        if (artifactProperties.isEmpty()) {
            return bean;
        }

        injectBatchProperties(bean, artifactProperties);

        return bean;
    }

    private void setRequiredAnnotations() {
        ClassLoader cl = BatchPropertyBeanPostProcessor.class.getClassLoader();

        try {
            this.requiredAnnotations.add((Class<? extends Annotation>) cl.loadClass("javax.inject.Inject"));
        } catch (ClassNotFoundException ex) {
            logger.warn("javax.inject.Inject not found - @BatchProperty marked fields will not be processed.");
        }

        this.requiredAnnotations.add(BatchProperty.class);
    }

    private boolean isBatchArtifact(Object bean) {
        return (bean instanceof ItemReader) ||
                (bean instanceof ItemProcessor) ||
                (bean instanceof ItemWriter) ||
                (bean instanceof CompletionPolicy) ||
                (bean instanceof Batchlet) ||
                (bean instanceof ItemReadListener) ||
                (bean instanceof ItemProcessListener) ||
                (bean instanceof ItemWriteListener) ||
                (bean instanceof JobExecutionDecider) ||
                (bean instanceof Step) ||
                (bean instanceof Job);
    }

    private void injectBatchProperties(final Object bean, final Properties artifactProperties) {
        ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                if (isValidFieldModifier(field) && isAnnotated(field)) {
                    boolean isAccessible = field.isAccessible();
                    field.setAccessible(true);

                    String batchProperty = getBatchPropertyFieldValue(field, artifactProperties);

                    if (batchProperty != null) {
                        field.set(bean, batchProperty);
                    }

                    field.setAccessible(isAccessible);
                }
            }
        });
    }

    private String getBatchPropertyFieldValue(Field field, Properties batchArtifactProperties) {
        BatchProperty batchProperty = field.getAnnotation(BatchProperty.class);

        if (!"".equals(batchProperty.name())) {
            return getBatchProperty(batchProperty.name(), batchArtifactProperties);
        }

        return getBatchProperty(field.getName(), batchArtifactProperties);
    }

    private String getBatchProperty(String propertyKey, Properties batchArtifactProperties) {
        if (batchArtifactProperties.containsKey(propertyKey)) {
            return (String) batchArtifactProperties.get(propertyKey);
        }

        return null;
    }

    private boolean isAnnotated(Field field) {
        for (Class<? extends Annotation> annotation : requiredAnnotations) {
            if(!field.isAnnotationPresent(annotation)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidFieldModifier(Field field) {
        return !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
