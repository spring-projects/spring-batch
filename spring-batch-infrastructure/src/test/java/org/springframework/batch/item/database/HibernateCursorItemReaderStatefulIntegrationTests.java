package org.springframework.batch.item.database;

import static org.easymock.EasyMock.*;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.junit.runner.RunWith;
import org.junit.Test;

/**
 * Tests for {@link HibernateCursorItemReader} using standard hibernate {@link Session}.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
public class HibernateCursorItemReaderStatefulIntegrationTests extends HibernateCursorItemReaderIntegrationTests {

	protected boolean isUseStatelessSession() {
		return false;
	}
	
	//Ensure close is called on the stateful session correctly.
	@Test
	public void testStatfulClose(){
		
		SessionFactory sessionFactory = createMock(SessionFactory.class);
		Session session = createMock(Session.class);
		Query scrollableResults = createNiceMock(Query.class);
		HibernateCursorItemReader<Foo> itemReader = new HibernateCursorItemReader<Foo>();
		itemReader.setSessionFactory(sessionFactory);
		itemReader.setQueryString("testQuery");
		itemReader.setUseStatelessSession(false);
		
		expect(sessionFactory.openSession()).andReturn(session);
		expect(session.createQuery("testQuery")).andReturn(scrollableResults);
		expect(scrollableResults.setFetchSize(0)).andReturn(scrollableResults);
		expect(session.close()).andReturn(null);
		
		replay(sessionFactory);
		replay(session);
		replay(scrollableResults);
		
		itemReader.open(new ExecutionContext());
		itemReader.close(new ExecutionContext());
		
		verify(sessionFactory);
		verify(session);
	}
	
}
