package org.springframework.batch.io.cursor;

import org.hibernate.Session;

/**
 * Tests for {@link HibernateCursorInputSource} using standard hibernate {@link Session}.
 * 
 * @author Robert Kasanicky
 */
public class HibernateCursorInputSourceStatefulIntegrationTests extends HibernateCursorInputSourceIntegrationTests{

	protected boolean isUseStatelessSession() {
		return false;
	}
	
}
