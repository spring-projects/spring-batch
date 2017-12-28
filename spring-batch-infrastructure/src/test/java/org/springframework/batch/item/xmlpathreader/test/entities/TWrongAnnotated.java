package org.springframework.batch.item.xmlpathreader.test.entities;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.test.Const;

@XmlPath(path = Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document)
public class TWrongAnnotated extends TSuper {
	private String name;

	public String getName() {
		return name;
	}

	@XmlPath(path = "name")
	public void addName(String name) {
		this.name = name;
	}

}
