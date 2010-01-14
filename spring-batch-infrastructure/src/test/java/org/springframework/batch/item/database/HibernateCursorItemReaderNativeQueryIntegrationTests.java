package org.springframework.batch.item.database;

import org.springframework.batch.item.database.orm.HibernateNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 */
public class HibernateCursorItemReaderNativeQueryIntegrationTests extends AbstractHibernateCursorItemReaderIntegrationTests {
  
    @Override
    protected void setQuery(HibernateCursorItemReader<?> hibernateReader) throws Exception {

    	String nativeQuery = "select * from T_FOOS";
        
        //creating a native query provider as it would be created in configuration
        HibernateNativeQueryProvider<Foo> queryProvider = 
            new HibernateNativeQueryProvider<Foo>();

        queryProvider.setSqlQuery(nativeQuery);
        queryProvider.setEntityClass(Foo.class);
        queryProvider.afterPropertiesSet();
        
        hibernateReader.setQueryProvider(queryProvider);
        
    }
}
