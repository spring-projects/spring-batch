/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.step.item;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.retry.RetryPolicy;

/**
 * Factory bean for step that executes its item processing with a stateful
 * retry. Failed items where the exception is classified as retryable always
 * cause a rollback. Before a rollback, the {@link Step} makes a record of the
 * failed item, caching it under a key given by the {@link ItemKeyGenerator}.
 * Then when it is re-presented by the {@link ItemReader} it is recognised and
 * retried up to a limit given by the {@link RetryPolicy}. When the retry is
 * exhausted the item is skipped and handled by a {@link SkipListener} if one is
 * present.<br/>
 * 
 * The skipLimit property is still used to control the overall exception
 * handling policy. Only exhausted retries count against the exception handler,
 * instead of counting all exceptions.<br/>
 * 
 * This class is not designed for extension. Do not subclass it.
 * 
 * @author Dave Syer
 * 
 * @deprecated All the features of this factory bean were moved to
 * {@link SkipLimitStepFactoryBean} in version 1.1.0.
 * 
 */
public class StatefulRetryStepFactoryBean extends SkipLimitStepFactoryBean {

}
