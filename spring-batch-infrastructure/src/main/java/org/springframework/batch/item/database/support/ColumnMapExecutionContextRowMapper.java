/**
 * 
 */
package org.springframework.batch.item.database.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * </p>Extension of the {@link ColumnMapRowMapper} that converts a column map to {@link ExecutionContext} and allows
 * {@link ExecutionContext} to be converted back as a PreparedStatementSetter.  This is useful in a restart 
 * scenario, as it allows for the standard functionality of the ColumnMapRowMapper to be used to 
 * create a map representing the columns returned by a query.  It should be noted that this column ordering
 * is preserved in the map using a link list version of Map.</p>
 * 
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @see ExecutionContextRowMapper
 * @see ColumnMapRowMapper
 */
public class ColumnMapExecutionContextRowMapper extends ColumnMapRowMapper implements ExecutionContextRowMapper {
	
	public static final String KEY_PREFIX = ClassUtils.getQualifiedName(ColumnMapExecutionContextRowMapper.class) + ".KEY.";
	
	public PreparedStatementSetter createSetter(ExecutionContext executionContext) {
		List columns = new ArrayList();
		int count=0;
		while(executionContext.containsKey(KEY_PREFIX+count)) {
			Object column = executionContext.get(KEY_PREFIX+count);
			columns.add(column);
			count++;
		}

		return new ArgPreparedStatementSetter(columns.toArray());
	}

	public void mapKeys(Object key, ExecutionContext executionContext) {
		Assert.isInstanceOf(Map.class, key, "Input to create ExecutionContext must be of type Map.");
		Map keys = (Map) key;
		int count = 0;
		for (Iterator it = keys.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry)it.next();
			executionContext.put(KEY_PREFIX+count, entry.getValue());
			count++;
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
