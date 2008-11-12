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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.listener.JobExecutionListenerAdapter;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} for {@link JobExecutionListener}s
 * 
 * @author Lucas Ward
 *
 */
public class JobExecutionListenerParser {

	
	public ManagedList parse(Element element,
			ParserContext parserContext) {
		List<BeanReference> listeners = new ArrayList<BeanReference>();
		
		@SuppressWarnings("unchecked")
		List<Element> listenerElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "listener");
		for(Element listenerElement : listenerElements){
			BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.genericBeanDefinition(JobExecutionListenerAdapter.class);
			String delegateName = listenerElement.getAttribute("ref");
			listenerBuilder.addConstructorArgReference(delegateName);
			
			String beforeMethod = listenerElement.getAttribute("before-method");
			if(StringUtils.hasText(beforeMethod)){
				listenerBuilder.addPropertyValue("beforeMethod", beforeMethod);
			}
			
			String afterMethod = listenerElement.getAttribute("after-method");
			if(StringUtils.hasText(beforeMethod)){
				listenerBuilder.addPropertyValue("afterMethod", afterMethod);
			}
			AbstractBeanDefinition beanDef = listenerBuilder.getBeanDefinition();
			String id = listenerElement.getAttribute("id");
			if (!StringUtils.hasText(id)) {
				id = parserContext.getReaderContext().generateBeanName(beanDef);
			}
			parserContext.getRegistry().registerBeanDefinition(id, beanDef);
	        BeanReference bean = new RuntimeBeanReference(id);
			listeners.add(bean);
		}
		
		ManagedList managedList = new ManagedList();
		@SuppressWarnings( { "unchecked", "unused" })
		boolean dummy = managedList.addAll(listeners);
		
		return managedList;
	}

}
