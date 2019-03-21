/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * Base {@code Configuration} class providing common structure for enabling and using Spring Batch. Customization is
 * available by implementing the {@link BatchConfigurer} interface.
 * 
 * @author Dave Syer
 * @since 2.2
 * @see EnableBatchProcessing
 */
public class BatchConfigurationSelector implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		Class<?> annotationType = EnableBatchProcessing.class;
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(
				annotationType.getName(), false));
		Assert.notNull(attributes, String.format("@%s is not present on importing class '%s' as expected",
				annotationType.getSimpleName(), importingClassMetadata.getClassName()));

		String[] imports;
		if (attributes.containsKey("modular") && attributes.getBoolean("modular")) {
			imports = new String[] { ModularBatchConfiguration.class.getName() };
		}
		else {
			imports = new String[] { SimpleBatchConfiguration.class.getName() };
		}

		return imports;
	}

}