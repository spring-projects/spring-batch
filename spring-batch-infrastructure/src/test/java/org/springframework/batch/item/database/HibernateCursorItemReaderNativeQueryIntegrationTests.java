/*
 * Copyright 2010 the original author or authors.
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

import org.springframework.batch.item.database.orm.HibernateNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 */
public class HibernateCursorItemReaderNativeQueryIntegrationTests extends AbstractHibernateCursorItemReaderIntegrationTests {
  
    @Override
    protected void setQuery(HibernateCursorItemReader<Foo> hibernateReader) throws Exception {

    	String nativeQuery = "select * from T_FOOS";
        
        //creating a native query provider as it would be created in configuration
        HibernateNativeQueryProvider<Foo> queryProvider = 
            new HibernateNativeQueryProvider<>();

        queryProvider.setSqlQuery(nativeQuery);
        queryProvider.setEntityClass(Foo.class);
        queryProvider.afterPropertiesSet();
        
        hibernateReader.setQueryProvider(queryProvider);
        
    }
}
