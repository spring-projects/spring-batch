package org.springframework.batch.item.database;

import org.hibernate.SessionFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.HibernateNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 */
public class HibernateCursorItemReaderNativeQueryIntegrationTests extends HibernateCursorItemReaderIntegrationTests {

    public ItemReader<Foo> createItemReader() throws Exception {
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("Foo.hbm.xml", getClass()) });
        factoryBean.afterPropertiesSet();

        SessionFactory sessionFactory = (SessionFactory) factoryBean.getObject();


        HibernateCursorItemReader<Foo> hibernateReader = new HibernateCursorItemReader<Foo>();
        
        String nativeQuery = "select * from T_FOOS";
        
        //creating a native query provider as it would be created in configuration
        HibernateNativeQueryProvider<Foo> queryProvider = 
            new HibernateNativeQueryProvider<Foo>();

        queryProvider.setSqlQuery(nativeQuery);
        queryProvider.setEntityClass(Foo.class);
        queryProvider.afterPropertiesSet();
        
        hibernateReader.setQueryProvider(queryProvider);
        
        //hibernateReader.setUseStatelessSession(isUseStatelessSession());
        hibernateReader.setSessionFactory(sessionFactory);
        hibernateReader.setSaveState(true);
        hibernateReader.afterPropertiesSet();

        return hibernateReader;
    }
}
