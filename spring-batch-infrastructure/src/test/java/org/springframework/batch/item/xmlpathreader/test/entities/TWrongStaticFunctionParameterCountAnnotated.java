package org.springframework.batch.item.xmlpathreader.test.entities;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.test.Const;

@XmlPath(path = Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document)
public class TWrongStaticFunctionParameterCountAnnotated extends TSuper {

	public TWrongStaticFunctionParameterCountAnnotated() {
		super();
	}

	@XmlPath(path = "name")
	public static TWrongStaticFunctionParameterCountAnnotated createObject(String name, String toMatch) {
		return new TWrongStaticFunctionParameterCountAnnotated();
	}

}
