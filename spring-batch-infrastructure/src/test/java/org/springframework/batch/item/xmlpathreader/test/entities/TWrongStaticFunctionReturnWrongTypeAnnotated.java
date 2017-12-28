package org.springframework.batch.item.xmlpathreader.test.entities;

import org.springframework.batch.item.xmlpathreader.annotations.XmlPath;
import org.springframework.batch.item.xmlpathreader.test.Const;

@XmlPath(path = Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document)
public class TWrongStaticFunctionReturnWrongTypeAnnotated extends TSuper {

	public TWrongStaticFunctionReturnWrongTypeAnnotated() {
		super();
	}

	@XmlPath(path = "name")
	public static Double createObject() {
		return new Double(2.0);
	}

}
