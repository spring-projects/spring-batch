/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;

import javax.batch.api.BatchProperty;

import org.springframework.beans.factory.annotation.InjectionMetadata;

/**
 * <p>This class overrides methods in the copied {@link SpringAutowiredAnnotationBeanPostProcessor} class
 * to check for the {@link BatchProperty} annotation before processing injection annotations. If the annotation
 * is found, further injection processing for the field is skipped.</p>
 */
public class JsrAutowiredAnnotationBeanPostProcessor extends SpringAutowiredAnnotationBeanPostProcessor {
	@Override
	protected InjectionMetadata findAutowiringMetadata(Class<?> clazz) {
		return super.buildAutowiringMetadata(clazz);
	}

	@Override
	protected Annotation findAutowiredAnnotation(AccessibleObject ao) {
		if (ao.getAnnotation(BatchProperty.class) != null) {
			return null;
		}

		return super.findAutowiredAnnotation(ao);
	}
}
