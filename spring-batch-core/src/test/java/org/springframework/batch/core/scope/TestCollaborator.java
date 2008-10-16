package org.springframework.batch.core.scope;


public class TestCollaborator implements Collaborator {

	private String name;

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
