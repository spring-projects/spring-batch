/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum representing on of 3 possible actions on a customer update:
 * Add, update, or delete
 * 
 * @author Lucas Ward
 *
 */
public enum CustomerOperation {
	ADD('A'), UPDATE('U'), DELETE('D');
	
	private final char code;
	private static final Map<Character,CustomerOperation> codeMap;
	
	private CustomerOperation(char code) {
		this.code = code;
	}
	
	static{
		codeMap = new HashMap<Character,CustomerOperation>();
		for(CustomerOperation operation:values()){
			codeMap.put(operation.getCode(), operation);
		}
	}
	
	public static CustomerOperation fromCode(char code){
		if(codeMap.containsKey(code)){
			return codeMap.get(code);
		}
		else{
			throw new IllegalArgumentException("Invalid code: [" + code + "]");
		}
	}
	
	public char getCode() {
		return code;
	}
}
