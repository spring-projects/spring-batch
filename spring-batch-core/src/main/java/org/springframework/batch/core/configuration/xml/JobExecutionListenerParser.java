/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.listener.AbstractListenerFactoryBean;
import org.springframework.batch.core.listener.JobListenerFactoryBean;
import org.springframework.batch.core.listener.JobListenerMetaData;
import org.springframework.batch.core.listener.ListenerMetaData;

/**
 * Parser for a step listener element. Builds a {@link JobListenerFactoryBean}
 * using attributes from the configuration.
 * 
 * @author Dan Garrette
 * @since 2.0
 * @see AbstractListenerParser
 */
public class JobExecutionListenerParser extends AbstractListenerParser {

	protected Class<? extends AbstractListenerFactoryBean> getBeanClass() {
		return JobListenerFactoryBean.class;
	}

	protected ListenerMetaData[] getMetaDataValues() {
		return JobListenerMetaData.values();
	}

}
