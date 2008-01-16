package org.springframework.batch.io.file.mapping;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Properties;

public interface FieldSet {

	public String[] getNames();

	public String[] getValues();
	
	public String readString(int index); 
	
	public String readString(String name);
	
	public boolean readBoolean(int index); 
	
	public boolean readBoolean(String name);
	
	public boolean readBoolean(int index, String trueValue);
	
	public boolean readBoolean(String name, String trueValue);
	
	public char readChar(int index);
	
	public char readChar(String name);
	
	public byte readByte(int index);
	
	public byte readByte(String name);
	
	public short readShort(int index);
	
	public short readShort(String name);
	
	public int readInt(int index);
	
	public int readInt(String name);
	
	public int readInt(int index, int defaultValue);
	
	public int readInt(String name, int defaultValue);
	
	public long readLong(int index);
	
	public long readLong(String name);
	
	public long readLong(int index, long defaultValue);
	
	public long readLong(String name, long defaultValue);
	
	public float readFloat(int index);
	
	public float readFloat(String name);
	
	public double readDouble(int index);
	
	public double readDouble(String name);
	
	public BigDecimal readBigDecimal(int index);
	
	public BigDecimal readBigDecimal(String name);
	
	public BigDecimal readBigDecimal(int index, BigDecimal defaultValue);
	
	public BigDecimal readBigDecimal(String name, BigDecimal defaultValue);
	
	public Date readDate(int index);
	
	public Date readDate(String name);
	
	public Date readDate(int index, String pattern);
	
	public Date readDate(String name, String pattern);
	
	public int getFieldCount();

	public Properties getProperties();
}
