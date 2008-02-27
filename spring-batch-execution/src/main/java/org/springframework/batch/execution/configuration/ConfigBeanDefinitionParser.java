/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.execution.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.execution.job.SimpleJob;
import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.execution.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.execution.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.execution.step.TaskletStep;
import org.springframework.batch.execution.step.support.LimitCheckingItemSkipPolicy;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DB2SequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DerbyMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgreSQLSequenceMaxValueIncrementer;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Ben Hale
 */
public class ConfigBeanDefinitionParser implements BeanDefinitionParser {

	private static final String JOB_REPOSITORY_ELEMENT = "job-repository";

	private static final String JOB_REPOSITORY_BEAN_NAME = "_jobRepository";

	private static final String DATA_SOURCE_ATT = "data-source";

	private static final String DB_TYPE_ATT = "db-type";

	private static final String DB_TYPE_DB2 = "db2";

	private static final String DB_TYPE_DERBY = "derby";

	private static final String DB_TYPE_HSQL = "hsql";

	private static final String DB_TYPE_MYSQL = "mysql";

	private static final String DB_TYPE_ORACLE = "oracle";

	private static final String DB_TYPE_POSTGRES = "postgres";

	private static final String JOB_ELEMENT = "job";

	private static final String ID_ATT = "id";

	private static final String RERUN_ATT = "rerun";

	private static final String RERUN_ALWAYS = "always";

	private static final String RERUN_NEVER = "never";

	private static final String RERUN_INCOMPLETE = "incomplete";

	private static final String STEP_ELEMENT = "step";

	private static final String SIZE_ATT = "size";

	private static final String TRANSACTION_MANAGER_ATT = "transaction-manager";

	private static final String ITEM_READER_ATT = "item-reader";

	private static final String ITEM_WRITER_ATT = "item-writer";

	private static final String SKIP_LIMIT_ATT = "skip-limit";

	private static final String TASKLET_STEP_ELEMENT = "tasklet-step";

	private static final String TASKLET_ATT = "tasklet";

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(),
		        parserContext.extractSource(element));
		parserContext.pushContainingComponent(compositeDef);

		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String localName = child.getLocalName();
				if (JOB_REPOSITORY_ELEMENT.equals(localName)) {
					parseJobRepository((Element) child, parserContext);
				} else if (JOB_ELEMENT.equals(localName)) {
					parseJob((Element) child, parserContext);
				}
			}
		}

		parserContext.popAndRegisterContainingComponent();
		return null;
	}

	private void parseJobRepository(Element jobRepoEle, ParserContext parserContext) {
		RootBeanDefinition jobRepoDef = new RootBeanDefinition(SimpleJobRepository.class);
		jobRepoDef.setSource(parserContext.extractSource(jobRepoEle));

		String dataSourceId = jobRepoEle.getAttribute(DATA_SOURCE_ATT);
		if (!StringUtils.hasText(dataSourceId)) {
			parserContext.getReaderContext().error("'data-source' attribute contains empty value", jobRepoEle);
		} else {
			String dbType = jobRepoEle.getAttribute(DB_TYPE_ATT);
			ConstructorArgumentValues constructorArgumentValues = jobRepoDef.getConstructorArgumentValues();
			String templateId = createJdbcTemplateDefinition(dataSourceId, parserContext);
			constructorArgumentValues.addGenericArgumentValue(createJobInstanceDao(templateId, dataSourceId, dbType,
			        parserContext));
			constructorArgumentValues.addGenericArgumentValue(createJobExecutionDao(templateId, dataSourceId, dbType,
			        parserContext));
			constructorArgumentValues.addGenericArgumentValue(createStepExecutionDao(templateId, dataSourceId, dbType,
			        parserContext));
		}

		parserContext.registerBeanComponent(new BeanComponentDefinition(jobRepoDef, JOB_REPOSITORY_BEAN_NAME));
	}

	private String createJdbcTemplateDefinition(String dataSourceId, ParserContext parserContext) {
		RootBeanDefinition templateDef = new RootBeanDefinition(JdbcTemplate.class);
		templateDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(dataSourceId));
		return parserContext.getReaderContext().registerWithGeneratedName(templateDef);
	}

	private BeanDefinition createJobInstanceDao(String templateId, String dataSourceId, String dbType,
	        ParserContext parserContext) {
		RootBeanDefinition daoDef = new RootBeanDefinition(JdbcJobInstanceDao.class);
		MutablePropertyValues propertyValues = daoDef.getPropertyValues();
		propertyValues.addPropertyValue("jdbcTemplate", new RuntimeBeanReference(templateId));
		propertyValues.addPropertyValue("jobIncrementer", getIncrementer(dbType, dataSourceId, "BATCH_JOB_SEQ"));
		return daoDef;
	}

	private BeanDefinition createJobExecutionDao(String templateId, String dataSourceId, String dbType,
	        ParserContext parserContext) {
		RootBeanDefinition daoDef = new RootBeanDefinition(JdbcJobExecutionDao.class);
		MutablePropertyValues propertyValues = daoDef.getPropertyValues();
		propertyValues.addPropertyValue("jdbcTemplate", new RuntimeBeanReference(templateId));
		propertyValues.addPropertyValue("jobExecutionIncrementer", getIncrementer(dbType, dataSourceId,
		        "BATCH_JOB_EXECUTION_SEQ"));
		return daoDef;
	}

	private BeanDefinition createStepExecutionDao(String templateId, String dataSourceId, String dbType,
	        ParserContext parserContext) {
		RootBeanDefinition daoDef = new RootBeanDefinition(JdbcStepExecutionDao.class);
		MutablePropertyValues propertyValues = daoDef.getPropertyValues();
		propertyValues.addPropertyValue("jdbcTemplate", new RuntimeBeanReference(templateId));
		propertyValues.addPropertyValue("stepExecutionIncrementer", getIncrementer(dbType, dataSourceId,
		        "BATCH_STEP_EXECUTION_SEQ"));
		return daoDef;
	}

	private BeanDefinition getIncrementer(String dbType, String dataSourceId, String incrementerName) {
		RootBeanDefinition incrementerDef = new RootBeanDefinition();

		if (DB_TYPE_DB2.equals(dbType)) {
			incrementerDef.setBeanClass(DB2SequenceMaxValueIncrementer.class);
			addSequenceIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (DB_TYPE_DERBY.equals(dbType)) {
			incrementerDef.setBeanClass(DerbyMaxValueIncrementer.class);
			addTableIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (DB_TYPE_HSQL.equals(dbType)) {
			incrementerDef.setBeanClass(HsqlMaxValueIncrementer.class);
			addTableIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (DB_TYPE_MYSQL.equals(dbType)) {
			incrementerDef.setBeanClass(MySQLMaxValueIncrementer.class);
			addTableIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (DB_TYPE_ORACLE.equals(dbType)) {
			incrementerDef.setBeanClass(OracleSequenceMaxValueIncrementer.class);
			addSequenceIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (DB_TYPE_POSTGRES.equals(dbType)) {
			incrementerDef.setBeanClass(PostgreSQLSequenceMaxValueIncrementer.class);
			addSequenceIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		}

		return incrementerDef;
	}

	private void addSequenceIncrementer(String dataSourceId, String incrementerName,
	        ConstructorArgumentValues constructorArgumentValues) {
		constructorArgumentValues.addGenericArgumentValue(new RuntimeBeanReference(dataSourceId));
		constructorArgumentValues.addGenericArgumentValue(incrementerName);
	}

	private void addTableIncrementer(String dataSourceId, String incrementerName,
	        ConstructorArgumentValues constructorArgumentValues) {
		constructorArgumentValues.addGenericArgumentValue(new RuntimeBeanReference(dataSourceId));
		constructorArgumentValues.addGenericArgumentValue(incrementerName);
		constructorArgumentValues.addGenericArgumentValue("id");
	}

	private void parseJob(Element jobEle, ParserContext parserContext) {
		AbstractBeanDefinition jobDef = createJobBeanDefinition(jobEle, parserContext);
		List steps = new ArrayList();

		NodeList childNodes = jobEle.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String localName = child.getLocalName();
				if (STEP_ELEMENT.equals(localName)) {
					String id = parseStep((Element) child, parserContext);
					steps.add(new RuntimeBeanReference(id));
				} else if (TASKLET_STEP_ELEMENT.equals(localName)) {
					String id = parseTaskletStep((Element) child, parserContext);
					steps.add(new RuntimeBeanReference(id));
				}
			}
		}
		
		jobDef.getPropertyValues().addPropertyValue("steps", steps);
	}

	private AbstractBeanDefinition createJobBeanDefinition(Element jobEle, ParserContext parserContext) {
		RootBeanDefinition jobDef = new RootBeanDefinition(SimpleJob.class);
		jobDef.setSource(parserContext.extractSource(jobEle));
		jobDef.getPropertyValues().addPropertyValue("jobRepository", JOB_REPOSITORY_BEAN_NAME);
		return jobDef;
	}

	private String parseStep(Element stepEle, ParserContext parserContext) {
		AbstractBeanDefinition stepDef = createStepBeanDefinition(stepEle, parserContext);
		String id = stepEle.getAttribute(ID_ATT);

		if (StringUtils.hasText(id)) {
			parserContext.getRegistry().registerBeanDefinition(id, stepDef);
			return id;
		} else {
			return parserContext.getReaderContext().registerWithGeneratedName(stepDef);
		}
	}

	private AbstractBeanDefinition createStepBeanDefinition(Element stepElement, ParserContext parserContext) {
		RootBeanDefinition stepDef = new RootBeanDefinition(ItemOrientedStep.class);
		stepDef.setSource(parserContext.extractSource(stepElement));
		MutablePropertyValues propertyValues = stepDef.getPropertyValues();

		String size = stepElement.getAttribute(SIZE_ATT);
		propertyValues.addPropertyValue("commitInterval", Integer.valueOf(size));
		String transactionManager = stepElement.getAttribute(TRANSACTION_MANAGER_ATT);
		if (!StringUtils.hasText(transactionManager)) {
			parserContext.getReaderContext().error("'transaction-manager' attribute contains empty value", stepElement);
		} else {
			propertyValues.addPropertyValue("transactionManager", new RuntimeBeanReference(transactionManager));
		}

		String itemReader = stepElement.getAttribute(ITEM_READER_ATT);
		if (!StringUtils.hasText(itemReader)) {
			parserContext.getReaderContext().error("'item-reader' attribute contains empty value", stepElement);
		} else {
			propertyValues.addPropertyValue("itemReader", new RuntimeBeanReference(itemReader));
		}
		String itemWriter = stepElement.getAttribute(ITEM_WRITER_ATT);
		if (!StringUtils.hasText(itemWriter)) {
			parserContext.getReaderContext().error("'item-writer' attribute contains empty value", stepElement);
		} else {
			propertyValues.addPropertyValue("itemWriter", new RuntimeBeanReference(itemWriter));
		}

		if (stepElement.hasAttribute(SKIP_LIMIT_ATT)) {
			String skipLimit = stepElement.getAttribute(SKIP_LIMIT_ATT);
			propertyValues.addPropertyValue("skipLimit", createSkipLimitBeanDefinition(Integer.valueOf(skipLimit)));
		}

		String rerun = stepElement.getAttribute(RERUN_ATT);
		setPropertiesForRerun(rerun, propertyValues);
		propertyValues.addPropertyValue("jobRepository", new RuntimeBeanReference(JOB_REPOSITORY_BEAN_NAME));
		return stepDef;
	}

	private AbstractBeanDefinition createSkipLimitBeanDefinition(Integer skipLimit) {
		RootBeanDefinition skipLimitDef = new RootBeanDefinition(LimitCheckingItemSkipPolicy.class);
		skipLimitDef.getConstructorArgumentValues().addGenericArgumentValue(skipLimit);
		return skipLimitDef;
	}

	private String parseTaskletStep(Element taskletStepEle, ParserContext parserContext) {
		AbstractBeanDefinition stepDef = createTaskletStepBeanDefinition(taskletStepEle, parserContext);

		String id = taskletStepEle.getAttribute(ID_ATT);

		if (StringUtils.hasText(id)) {
			parserContext.getRegistry().registerBeanDefinition(id, stepDef);
			return id;
		} else {
			return parserContext.getReaderContext().registerWithGeneratedName(stepDef);
		}
	}

	private AbstractBeanDefinition createTaskletStepBeanDefinition(Element taskletElement, ParserContext parserContext) {
		RootBeanDefinition stepDef = new RootBeanDefinition(TaskletStep.class);
		stepDef.setSource(parserContext.extractSource(taskletElement));
		MutablePropertyValues propertyValues = stepDef.getPropertyValues();

		String tasklet = taskletElement.getAttribute(TASKLET_ATT);
		if (!StringUtils.hasText(tasklet)) {
			parserContext.getReaderContext().error("'tasklet' attribute contains empty value", taskletElement);
		} else {
			propertyValues.addPropertyValue("tasklet", new RuntimeBeanReference(tasklet));
		}

		String rerun = taskletElement.getAttribute(RERUN_ATT);
		setPropertiesForRerun(rerun, propertyValues);
		propertyValues.addPropertyValue("jobRepository", new RuntimeBeanReference(JOB_REPOSITORY_BEAN_NAME));

		return stepDef;
	}

	private void setPropertiesForRerun(String rerun, MutablePropertyValues propertyValues) {
		if (RERUN_ALWAYS.equals(rerun)) {
			propertyValues.addPropertyValue("allowStartIfComplete", Boolean.TRUE);
			propertyValues.addPropertyValue("startLimit", new Integer(Integer.MAX_VALUE));
		} else if (RERUN_NEVER.equals(rerun)) {
			propertyValues.addPropertyValue("allowStartIfComplete", Boolean.FALSE);
			propertyValues.addPropertyValue("startLimit", Integer.valueOf(1));
		} else if (RERUN_INCOMPLETE.equals(rerun)) {
			propertyValues.addPropertyValue("allowStartIfComplete", Boolean.FALSE);
			propertyValues.addPropertyValue("startLimit", new Integer(Integer.MAX_VALUE));
		}
	}
}
