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
package org.springframework.batch.execution.step;

import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.execution.step.simple.SimpleStepExecutor;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link StepExecutorFactory} that uses a prototype bean in the application
 * context to satisfy the factory contract. If the prototype bean and
 * {@link StepConfiguration} are of known (simple) type, they can be combined to
 * add the commit interval information from the configuration.<br/>
 * 
 * The nominated bean has to be a prototype because its state may be changed
 * before it is used, applying values for things like commit interval from the
 * {@link StepConfiguration}.
 * 
 * @author Dave Syer
 * 
 */
public class PrototypeBeanStepExecutorFactory implements StepExecutorFactory,
		BeanFactoryAware, InitializingBean {

	private String stepExecutorName = null;

	private BeanFactory beanFactory;

	/**
	 * Setter for injected {@link BeanFactory}.
	 * 
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Assert that if the step executor name is provided, then it is valid and
	 * is of prototype scope.
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		// Make an assertion that the bean exists and is of the correct type
		Assert
				.notNull(beanFactory.getBean(stepExecutorName,
						StepExecutor.class),
						"Step executor name must correspond to a StepExecutor instance.");
		Assert.state(beanFactory.isPrototype(stepExecutorName),
				"StepExecutor must be a prototype.  Change the scope of the bean named '"
						+ stepExecutorName + "' to prototype.");
	}

	/**
	 * Locate a {@link StepExecutor} for this configuration, allowing different
	 * strategies for configuring the inner loop (chunk operations). Try the
	 * following in this order, until one succeeds. In each case first obtain
	 * the {@link StepExecutor} referred to by the {@link #stepExecutorName},
	 * then:
	 * <ul>
	 * 
	 * <li>If the {@link StepExecutor} refers to a {@link SimpleStepExecutor},
	 * and {@link StepConfiguration} is an instance of
	 * {@link RepeatOperationsHolder}, then the {@link RepeatOperations} for
	 * the chunk will be pulled from there directly. This gives maximum
	 * flexibility for clients to control the properties of the iteration. For
	 * simple use cases where clients only need to control a few aspects of the
	 * execution, like the commit interval, this is not necessary.</li>
	 * 
	 * <li>If the {@link StepExecutor} is a {@link SimpleStepExecutor} and the
	 * configuration is a {@link SimpleStepConfiguration} then this
	 * implementation modifies the state of the {@link StepExecutor} to set the
	 * completion policy of the chunk operations. In this case the chunk
	 * operations cannot be set by the client of this factory.</li>
	 * 
	 * <li> Use the {@link StepExecutor} directly. </li>
	 * 
	 * </ul>
	 * <br/>
	 * 
	 * @throws IllegalStateException
	 *             if no {@link StepExecutor} can be located.
	 * 
	 * @see StepExecutorFactory#getExecutor(StepConfiguration)
	 */
	public StepExecutor getExecutor(StepConfiguration configuration) {

		StepExecutor executor = getStepExecutor();

		if (executor instanceof SimpleStepExecutor) {
			RepeatTemplate template = new RepeatTemplate();
			RepeatOperations repeatOperations = template;
			if (configuration instanceof RepeatOperationsHolder) {
				repeatOperations = ((RepeatOperationsHolder) configuration)
						.getChunkOperations();
				Assert
						.state(repeatOperations != null,
								"Chunk operations obtained from step configuration must be non-null.");
			} else if (configuration instanceof SimpleStepConfiguration) {
				template.setCompletionPolicy(new SimpleCompletionPolicy(
						((SimpleStepConfiguration) configuration)
								.getCommitInterval()));
			}
			((SimpleStepExecutor) executor)
					.setChunkOperations(repeatOperations);
		}

		return executor;

	}

	/**
	 * Setter for the bean name of the {@link StepExecutor} to use. The
	 * corresponding bean must be prototype scoped, so that its properties can
	 * be overridden per execution by the {@link StepConfiguration}.
	 * 
	 * @param stepExecutor
	 *            the stepExecutor to set
	 */
	public void setStepExecutorName(String stepExecutorName) {
		this.stepExecutorName = stepExecutorName;
	}

	/**
	 * Internal convenience method to get a step executor instance.
	 * 
	 * @return the step executor instance to use.
	 */
	private StepExecutor getStepExecutor() {
		return (StepExecutor) beanFactory.getBean(stepExecutorName,
				StepExecutor.class);
	}

}
