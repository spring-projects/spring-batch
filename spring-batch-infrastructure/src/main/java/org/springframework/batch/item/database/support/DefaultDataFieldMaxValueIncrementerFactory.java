//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.springframework.batch.item.database.support;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.batch.support.DatabaseType;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.Db2LuwMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.Db2MainframeMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DerbyMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HanaSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MariaDBSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SqlServerSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SybaseMaxValueIncrementer;

public class DefaultDataFieldMaxValueIncrementerFactory implements DataFieldMaxValueIncrementerFactory {
    private final DataSource dataSource;
    private String incrementerColumnName = "ID";

    public void setIncrementerColumnName(String incrementerColumnName) {
        this.incrementerColumnName = incrementerColumnName;
    }

    public DefaultDataFieldMaxValueIncrementerFactory(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataFieldMaxValueIncrementer getIncrementer(String incrementerType, String incrementerName) {
        DatabaseType databaseType = DatabaseType.valueOf(incrementerType.toUpperCase());
        if (databaseType != DatabaseType.DB2 && databaseType != DatabaseType.DB2AS400) {
            if (databaseType == DatabaseType.DB2ZOS) {
                return new Db2MainframeMaxValueIncrementer(this.dataSource, incrementerName);
            } else if (databaseType == DatabaseType.DERBY) {
                return new DerbyMaxValueIncrementer(this.dataSource, incrementerName, this.incrementerColumnName);
            } else if (databaseType == DatabaseType.HSQL) {
                return new HsqlMaxValueIncrementer(this.dataSource, incrementerName, this.incrementerColumnName);
            } else if (databaseType == DatabaseType.H2) {
                return new H2SequenceMaxValueIncrementer(this.dataSource, incrementerName);
            } else if (databaseType == DatabaseType.HANA) {
                return new HanaSequenceMaxValueIncrementer(this.dataSource, incrementerName);
            } else if (databaseType == DatabaseType.MYSQL) {
                MySQLMaxValueIncrementer mySQLMaxValueIncrementer = new MySQLMaxValueIncrementer(this.dataSource, incrementerName, this.incrementerColumnName);
                mySQLMaxValueIncrementer.setUseNewConnection(true);
                return mySQLMaxValueIncrementer;
            } else if (databaseType == DatabaseType.MARIADB) {
                return new MariaDBSequenceMaxValueIncrementer(this.dataSource, incrementerName);
            } else if (databaseType == DatabaseType.ORACLE) {
                return new OracleSequenceMaxValueIncrementer(this.dataSource, incrementerName);
            } else if (databaseType == DatabaseType.POSTGRES) {
                return new PostgresSequenceMaxValueIncrementer(this.dataSource, incrementerName);
            } else if (databaseType == DatabaseType.KINGBASE) {
                return new PostgresSequenceMaxValueIncrementer(this.dataSource, incrementerName);
            } else if (databaseType == DatabaseType.SQLITE) {
                return new SqliteMaxValueIncrementer(this.dataSource, incrementerName, this.incrementerColumnName);
            } else if (databaseType == DatabaseType.SQLSERVER) {
                return new SqlServerSequenceMaxValueIncrementer(this.dataSource, incrementerName);
            } else if (databaseType == DatabaseType.SYBASE) {
                return new SybaseMaxValueIncrementer(this.dataSource, incrementerName, this.incrementerColumnName);
            } else {
                throw new IllegalArgumentException("databaseType argument was not on the approved list");
            }
        } else {
            return new Db2LuwMaxValueIncrementer(this.dataSource, incrementerName);
        }
    }

    public boolean isSupportedIncrementerType(String incrementerType) {
        DatabaseType[] var2 = DatabaseType.values();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            DatabaseType type = var2[var4];
            if (type.name().equalsIgnoreCase(incrementerType)) {
                return true;
            }
        }

        return false;
    }

    public String[] getSupportedIncrementerTypes() {
        List<String> types = new ArrayList();
        DatabaseType[] var2 = DatabaseType.values();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            DatabaseType type = var2[var4];
            types.add(type.name());
        }

        return (String[])types.toArray(new String[types.size()]);
    }
}
