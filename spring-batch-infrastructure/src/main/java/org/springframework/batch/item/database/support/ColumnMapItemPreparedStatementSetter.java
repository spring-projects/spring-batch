/**
 * 
 */
package org.springframework.batch.item.database.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.util.Assert;

/**
 * </p>Implementation of the {@link KeyMappingPreparedStatementSetter} interface that assumes all
 * keys are contained within a {@link Map} with the column name as the key.  It assumes nothing 
 * about ordering, and assumes that the order the entry set can be iterated over is the same as
 * the PreparedStatement should be set.</p>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @see ItemPreparedStatementSetter
 * @see ColumnMapRowMapper
 */
public class ColumnMapItemPreparedStatementSetter implements ItemPreparedStatementSetter {

	public void setValues(Object item, PreparedStatement ps) throws SQLException {
		Assert.isInstanceOf(Map.class, item, "Input to map PreparedStatement parameters must be of type Map.");
		Set keySet = ((Map)item).entrySet();
		int counter = 1;
		for(Iterator it = keySet.iterator(); it.hasNext();){
			Entry entry = (Entry)it.next();
			StatementCreatorUtils.setParameterValue(ps, counter, SqlTypeValue.TYPE_UNKNOWN, entry.getValue());
			counter++;
		}
	}

}
