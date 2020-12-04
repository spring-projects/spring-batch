/*
 * Copyright 2010-2014 the original author or authors.
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
package org.springframework.batch.item.database;

import java.util.Collections;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.batch.item.database.orm.JpaNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaPagingItemReaderNativeQueryIntegrationTests.JpaConfiguration.class)
public class JpaPagingItemReaderNativeQueryIntegrationTests extends AbstractPagingItemReaderParameterTests {
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Override
    protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {

        String sqlQuery = "select * from T_FOOS where value >= :limit";

        JpaPagingItemReader<Foo> reader = new JpaPagingItemReader<>();
        
        //creating a native query provider as it would be created in configuration
        JpaNativeQueryProvider<Foo> queryProvider= new JpaNativeQueryProvider<>();
        queryProvider.setSqlQuery(sqlQuery);
        queryProvider.setEntityClass(Foo.class);
        queryProvider.afterPropertiesSet();
        
        reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 2));
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(3);
        reader.setQueryProvider(queryProvider);
        reader.afterPropertiesSet();
        reader.setSaveState(true);

        return reader;
    }

    @Configuration
    public static class JpaConfiguration {

        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.HSQL)
                    .addScript("org/springframework/batch/item/database/init-foo-schema-hsqldb.sql")
                    .build();
        }

        @Bean
        public PersistenceUnitManager persistenceUnitManager() {
            DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
            persistenceUnitManager.setDefaultDataSource(dataSource());
            persistenceUnitManager.afterPropertiesSet();
            return persistenceUnitManager;
        }

        @Bean
        public EntityManagerFactory entityManagerFactory() {
            LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
            factoryBean.setDataSource(dataSource());
            factoryBean.setPersistenceUnitManager(persistenceUnitManager());
            factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new JpaTransactionManager(entityManagerFactory());
        }
    }
}
