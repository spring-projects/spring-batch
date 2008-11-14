package org.springframework.batch.core.scope;

import java.io.Serializable;


public class TestCollaborator implements Collaborator, Serializable {

	private String name;
	
	private Collaborator parent;

	public Collaborator getParent() {
		return parent;
	}

	public void setParent(Collaborator parent) {
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.scope.Collaborator#getName()
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
