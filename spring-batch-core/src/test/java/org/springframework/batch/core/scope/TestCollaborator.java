package org.springframework.batch.core.scope;

import java.io.Serializable;
import java.util.List;


public class TestCollaborator implements Collaborator, Serializable {

	private String name;
	
	private Collaborator parent;
	
	private List<String> list;

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}

	public Collaborator getParent() {
		return parent;
	}

	public void setParent(Collaborator parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
