/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import org.springframework.batch.core.configuration.xml.SimpleFlowFactoryBean;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.jsr.job.flow.support.state.JsrStepState;

/**
 * Extension to the {@link SimpleFlowFactoryBean} that provides {@link org.springframework.batch.core.jsr.job.flow.support.state.JsrStepState}
 * implementations for JSR-352 based jobs.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrFlowFactoryBean extends SimpleFlowFactoryBean {

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.configuration.xml.SimpleFlowFactoryBean#createNewStepState(org.springframework.batch.core.job.flow.State, java.lang.String, java.lang.String)
	 */
	@Override
	protected State createNewStepState(State state, String oldName,
			String stateName) {
		return new JsrStepState(stateName, ((JsrStepState) state).getStep(oldName));
	}
}
