/**
 * 
 */
package org.springframework.batch.core;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Domain representation of a parameter to a batch job.  Only the following types can be
 * parameters: String, Long, Date, and Double.
 * 
 * @author Lucas Ward
 * @since 2.0
 *
 */
public class JobParameter implements Serializable{
	
	private final Object parameter;
	private final ParameterType parameterType;
	
	/**
	 * Construct a new JobParameter as a String.
	 */
	public JobParameter(String parameter){
		this.parameter = parameter;
		parameterType = ParameterType.STRING;
	}
	
	/**
	 * Construct a new JobParameter as a Long.
	 * 
	 * @param parameter
	 */
	public JobParameter(Long parameter){
		this.parameter = parameter;
		parameterType = ParameterType.LONG;
	}
	
	/**
	 * Construct a new JobParameter as a Date.
	 * 
	 * @param parameter
	 */
	public JobParameter(Date parameter) {
		this.parameter = new Date(parameter.getTime());
		parameterType = ParameterType.DATE;
	}
	
	/**
	 * Construct a new JobParameter as a Double.
	 * 
	 * @param parameter
	 */
	public JobParameter(Double parameter){
		this.parameter = parameter;
		parameterType = ParameterType.DOUBLE;
	}
	
	/**
	 * @return the value contained within this JobParameter.
	 */
	public Object getValue(){
		
		if(parameter.getClass().isInstance(Date.class)){
			return new Date(((Date)parameter).getTime());
		}
		else{
			return parameter;
		}
	}
	
	/**
	 * @return a ParameterType representing the type of this parameter.
	 */
	public ParameterType getType(){
		return parameterType;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof JobParameter == false){
			return false;
		}
		
		if(this == obj){
			return true;
		}
		
		JobParameter rhs = (JobParameter)obj;
		return this.parameter.equals(rhs.parameter);
	}
	
	@Override
	public String toString() {
		return parameter.toString();
	}
	
	public int hashCode() {
		return new HashCodeBuilder(7, 21).append(parameter).toHashCode();
	}
	
	/**
	 * Enumeration representing the type of a JobParameter. 
	 */
	public enum ParameterType{
		
		STRING, DATE, LONG, DOUBLE;
	}
}
