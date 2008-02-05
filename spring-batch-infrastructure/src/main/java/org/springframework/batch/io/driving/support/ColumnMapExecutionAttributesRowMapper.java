/**
 * 
 */
package org.springframework.batch.io.driving.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.core.CollectionFactory;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * </p>Extension of the ColumnMapRowMapper that converts a column map to {@link ExecutionAttributes} and allows
 * {@link ExecutionAttributes} to be converted back as a PreparedStatementSetter.  This is useful in a restart 
 * scenario, as it allows for the standard functionality of the ColumnMapRowMapper to be used to 
 * create a map representing the columns returned by a query.  It should be noted that this column ordering
 * is preserved in the map using a link list version of Map. 
 * 
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @see ExecutionAttributesRowMapper
 */
public class ColumnMapExecutionAttributesRowMapper extends ColumnMapRowMapper implements ExecutionAttributesRowMapper {
	
	public static final String KEY_PREFIX = ClassUtils.getQualifiedName(ColumnMapExecutionAttributesRowMapper.class) + ".KEY.";
	
	public PreparedStatementSetter createSetter(ExecutionAttributes executionAttributes) {
		
		ColumnMapExecutionAttributes columnData = new ColumnMapExecutionAttributes(executionAttributes.getProperties());

		List columns = new ArrayList();
		for (Iterator iterator = columnData.keys.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			Object column = entry.getValue();
			columns.add(column);
		}

		return new ArgPreparedStatementSetter(columns.toArray());
	}

	public ExecutionAttributes createExecutionAttributes(Object key) {
		Assert.isInstanceOf(Map.class, key, "Input to create ExecutionAttributes must be of type Map.");
		Map keys = (Map) key;
		return new ColumnMapExecutionAttributes(keys);
	}

	private static class ColumnMapExecutionAttributes extends ExecutionAttributes {

		private final Map keys;

		public ColumnMapExecutionAttributes(Map keys) {
			this.keys = keys;
		}

		public ColumnMapExecutionAttributes(Properties props) {

			keys = CollectionFactory.createLinkedCaseInsensitiveMapIfPossible(props.size());

			for (int counter = 0; counter < props.size(); counter++) {

				String key = KEY_PREFIX + counter;
				String column = props.getProperty(key);

				if (column != null) {
					keys.put(key, column);
				}
				else {
					break;
				}

			}
		}

		public Properties getProperties() {
			Properties props = new Properties();

			int counter = 0;
			for (Iterator iterator = keys.entrySet().iterator(); iterator.hasNext();) {
				Entry entry = (Entry) iterator.next();
				props.setProperty(KEY_PREFIX + counter, entry.getValue().toString());
				counter++;
			}

			return props;
		}

	}

	/*
	 * Exact duplicate of Spring class of the same name, copied because it is
	 * package private.
	 */
	private static class ArgPreparedStatementSetter implements PreparedStatementSetter {

		private final Object[] args;

		/**
		 * Create a new ArgPreparedStatementSetter for the given arguments.
		 * @param args the arguments to set
		 */
		public ArgPreparedStatementSetter(Object[] args) {
			this.args = args;
		}

		public void setValues(PreparedStatement ps) throws SQLException {
			if (this.args != null) {
				for (int i = 0; i < this.args.length; i++) {
					Object arg = this.args[i];
					if (arg instanceof SqlParameterValue) {
						SqlParameterValue paramValue = (SqlParameterValue) arg;
						StatementCreatorUtils.setParameterValue(ps, i + 1, paramValue, paramValue.getValue());
					}
					else {
						StatementCreatorUtils.setParameterValue(ps, i + 1, SqlTypeValue.TYPE_UNKNOWN, arg);
					}
				}
			}
		}
	}
}
