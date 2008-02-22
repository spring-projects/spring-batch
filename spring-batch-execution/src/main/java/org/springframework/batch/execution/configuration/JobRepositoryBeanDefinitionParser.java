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

import org.springframework.batch.execution.repository.SimpleJobRepository;
import org.springframework.batch.execution.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.execution.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.execution.repository.dao.JdbcStepExecutionDao;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
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

/**
 * @author Ben Hale
 */
class JobRepositoryBeanDefinitionParser implements BeanDefinitionParser {

	private static final String ATT_DATA_SOURCE = "data-source";

	private static final String ATT_DB_TYPE = "db-type";

	private static final String ENUM_DB2 = "db2";

	private static final String ENUM_DERBY = "derby";

	private static final String ENUM_HSQL = "hsql";

	private static final String ENUM_MYSQL = "mysql";

	private static final String ENUM_ORACLE = "oracle";

	private static final String ENUM_POSTGRES = "postgres";

	private static final String PROP_JDBC_TEMPLATE = "jdbcTemplate";

	private static final String PROP_JOB_INCREMENTER = "jobIncrementer";

	private static final String PROP_JOB_EXECUTION_INCREMENTER = "jobExecutionIncrementer";

	private static final String PROP_STEP_EXECUTION_INCREMENTER = "stepExecutionIncrementer";

	private static final String REPOSITORY_BEAN_NAME = "_jobRepository";

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		RootBeanDefinition repositoryDef = new RootBeanDefinition(SimpleJobRepository.class);

		String dbType = element.getAttribute(ATT_DB_TYPE);
		String dataSourceId = element.getAttribute(ATT_DATA_SOURCE);
		if (!StringUtils.hasText(dataSourceId)) {
			parserContext.getReaderContext().error("'data-source' attribute contains empty value", element);
		} else {
			ConstructorArgumentValues constructorArgumentValues = repositoryDef.getConstructorArgumentValues();
			String templateId = createJdbcTemplateDefinition(dataSourceId, parserContext);
			constructorArgumentValues.addGenericArgumentValue(createJobInstanceDao(templateId, dataSourceId, dbType,
			        parserContext));
			constructorArgumentValues.addGenericArgumentValue(createJobExecutionDao(templateId, dataSourceId, dbType,
			        parserContext));
			constructorArgumentValues.addGenericArgumentValue(createStepExecutionDao(templateId, dataSourceId, dbType,
			        parserContext));
		}

		parserContext.getRegistry().registerBeanDefinition(REPOSITORY_BEAN_NAME, repositoryDef);
		return null;
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
		propertyValues.addPropertyValue(PROP_JDBC_TEMPLATE, new RuntimeBeanReference(templateId));
		propertyValues.addPropertyValue(PROP_JOB_INCREMENTER, getIncrementer(dbType, dataSourceId, "BATCH_JOB_SEQ"));
		return daoDef;
	}

	private BeanDefinition createJobExecutionDao(String templateId, String dataSourceId, String dbType,
	        ParserContext parserContext) {
		RootBeanDefinition daoDef = new RootBeanDefinition(JdbcJobExecutionDao.class);
		MutablePropertyValues propertyValues = daoDef.getPropertyValues();
		propertyValues.addPropertyValue(PROP_JDBC_TEMPLATE, new RuntimeBeanReference(templateId));
		propertyValues.addPropertyValue(PROP_JOB_EXECUTION_INCREMENTER, getIncrementer(dbType, dataSourceId,
		        "BATCH_JOB_EXECUTION_SEQ"));
		return daoDef;
	}

	private BeanDefinition createStepExecutionDao(String templateId, String dataSourceId, String dbType,
	        ParserContext parserContext) {
		RootBeanDefinition daoDef = new RootBeanDefinition(JdbcStepExecutionDao.class);
		MutablePropertyValues propertyValues = daoDef.getPropertyValues();
		propertyValues.addPropertyValue(PROP_JDBC_TEMPLATE, new RuntimeBeanReference(templateId));
		propertyValues.addPropertyValue(PROP_STEP_EXECUTION_INCREMENTER, getIncrementer(dbType, dataSourceId,
		        "BATCH_STEP_EXECUTION_SEQ"));
		return daoDef;
	}

	private BeanDefinition getIncrementer(String dbType, String dataSourceId, String incrementerName) {
		RootBeanDefinition incrementerDef = new RootBeanDefinition();

		if (ENUM_DB2.equals(dbType)) {
			incrementerDef.setBeanClass(DB2SequenceMaxValueIncrementer.class);
			addSequenceIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (ENUM_DERBY.equals(dbType)) {
			incrementerDef.setBeanClass(DerbyMaxValueIncrementer.class);
			addTableIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (ENUM_HSQL.equals(dbType)) {
			incrementerDef.setBeanClass(HsqlMaxValueIncrementer.class);
			addTableIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (ENUM_MYSQL.equals(dbType)) {
			incrementerDef.setBeanClass(MySQLMaxValueIncrementer.class);
			addTableIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (ENUM_ORACLE.equals(dbType)) {
			incrementerDef.setBeanClass(OracleSequenceMaxValueIncrementer.class);
			addSequenceIncrementer(dataSourceId, incrementerName, incrementerDef.getConstructorArgumentValues());
		} else if (ENUM_POSTGRES.equals(dbType)) {
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
    
    private void addTableIncrementer(String dataSourceId, String incrementerName, ConstructorArgumentValues constructorArgumentValues) {
	    constructorArgumentValues.addGenericArgumentValue(new RuntimeBeanReference(dataSourceId));
		constructorArgumentValues.addGenericArgumentValue(incrementerName);
		constructorArgumentValues.addGenericArgumentValue("id");
    }

}
