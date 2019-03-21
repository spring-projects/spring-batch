/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
