package org.springframework.batch.item.database;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;

/**
 * Tests for {@link HibernateCursorItemReader} using standard hibernate {@link Session}.
 * 
 * @author Robert Kasanicky
 */
public class HibernateCursorItemReaderStatefulIntegrationTests extends AbstractHibernateCursorItemReaderIntegrationTests {

	protected boolean isUseStatelessSession() {
		return false;
	}
	
	//Ensure close is called on the stateful session correctly.
	@Test
	public void testStatefulClose(){
		
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
		itemReader.close();
		
		verify(sessionFactory);
		verify(session);
	}
	
}
