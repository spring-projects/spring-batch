package test.jdbc.proc.derby;

import java.sql.*;

/**
 * @author trisberg
 *
 * CALL SQLJ.install_jar('testproc.jar', 'APP.TESTPROC', 0);
 * CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.classpath', 'APP.TESTPROC');
 *
 * CALL SQLJ.replace_jar('testproc.jar', 'APP.TESTPROC');
 *
 * CALL SQLJ.remove_jar('APP.TESTPROC', 0)
 *
 */

public class TestProcedures {

    public static void readFoos(ResultSet[] rs) throws SQLException {
        String SQL = "SELECT id, name, value FROM T_FOOS";
        Connection conn = DriverManager.getConnection("jdbc:default:connection");
        PreparedStatement ps1 = conn.prepareStatement(SQL);
        rs[0] = ps1.executeQuery();
    }

    public static void readSomeFoos(int fromId, int toId, ResultSet[] rs) throws SQLException {
        String SQL = "SELECT id, name, value FROM T_FOOS WHERE id between ? and ?";
        Connection conn = DriverManager.getConnection("jdbc:default:connection");
        PreparedStatement ps2 = conn.prepareStatement(SQL);
        ps2.setInt(1, fromId);
        ps2.setInt(2, toId);
        rs[0] = ps2.executeQuery();
    }
}
