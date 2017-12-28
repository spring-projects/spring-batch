package org.springframework.batch.item.xmlpathreader.test.entities;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.test.Const;

@XmlPath(path = Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document)
public class TWrongFunctionParameterCountAnnotated extends TSuper {
	private String name;

	public String getName() {
		return name;
	}

	@XmlPath(path = "name")
	public void setName(String name, String toMatch) {
		this.name = name;
	}

}
