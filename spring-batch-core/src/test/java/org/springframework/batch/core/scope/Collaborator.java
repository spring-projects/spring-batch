package org.springframework.batch.core.scope;

public interface Collaborator {

	String getName();

	Collaborator getParent();
	
}