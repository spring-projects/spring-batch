package org.springframework.batch.item.database;

import java.util.Collections;

import javax.persistence.EntityManagerFactory;

import org.junit.runner.RunWith;
import org.springframework.batch.item.database.orm.JpaNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"JpaPagingItemReaderParameterTests-context.xml"})
public class JpaPagingItemReaderNativeQueryIntegrationTests extends AbstractPagingItemReaderParameterTests {
    
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {

        String sqlQuery = "select * from T_FOOS where value >= :limit";

        JpaPagingItemReader<Foo> reader = new JpaPagingItemReader<Foo>();
        
        //creating a native query provider as it would be created in configuration
        JpaNativeQueryProvider<Foo> queryProvider= new JpaNativeQueryProvider<Foo>();
        queryProvider.setSqlQuery(sqlQuery);
        queryProvider.setEntityClass(Foo.class);
        queryProvider.afterPropertiesSet();
        
        reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 3));
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(3);
        reader.setQueryProvider(queryProvider);
        reader.afterPropertiesSet();
        reader.setSaveState(true);

        return reader;
    }
}
