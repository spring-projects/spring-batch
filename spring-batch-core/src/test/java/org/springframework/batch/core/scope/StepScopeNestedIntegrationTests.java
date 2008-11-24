package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepScopeNestedIntegrationTests {
	
	@Autowired
	@Qualifier("proxied")
	private Step proxied;	
		
	@Autowired
	@Qualifier("parent")
	private Collaborator parent;	
		
	@Test
	public void testNestedScopedProxy() throws Exception {
		assertNotNull(proxied);
		assertEquals("foo", parent.getName());
	}

}
