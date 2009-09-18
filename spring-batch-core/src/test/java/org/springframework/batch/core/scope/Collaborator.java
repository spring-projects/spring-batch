package org.springframework.batch.core.scope;

import java.util.List;

public interface Collaborator {

	String getName();

	Collaborator getParent();
	
	List<String> getList();
	
}