/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

public class UpgradeToJava7Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.UpgradeToJava7"));
    }

    @Test
    void testJdbcCallableStatement() {
        rewriteRun(
          version(
            //language=java
            java("""
                    package com.test.withoutV170Methods;
                    
                    import java.sql.*;
                    import java.util.Map;
                    import java.util.Properties;
                    
                    import javax.sql.*;
                    
                    public class JRE7JdbcConnection implements java.sql.Connection {
                    
                        public <T> T unwrap(Class<T> iface) throws SQLException {
                            return null;
                        }
                    
                        public boolean isWrapperFor(Class<?> iface) throws SQLException {
                            return false;
                        }
                    
                        public void clearWarnings() throws SQLException {
                    
                        }
                    
                        public void close() throws SQLException {
                    
                        }
                    
                        public void commit() throws SQLException {
                    
                        }
                    
                        public Statement createStatement() throws SQLException {
                    
                            return null;
                        }
                    
                        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
                    
                            return null;
                        }
                    
                        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                            return null;
                        }
                    
                        public boolean getAutoCommit() throws SQLException {
                            return false;
                        }
                    
                        public String getCatalog() throws SQLException {
                            return null;
                        }
                    
                        public int getHoldability() throws SQLException {
                            return 0;
                        }
                    
                        public DatabaseMetaData getMetaData() throws SQLException {
                            return null;
                        }
                    
                        public int getTransactionIsolation() throws SQLException {
                            return 0;
                        }
                    
                        public Map<String, Class<?>> getTypeMap() throws SQLException {
                            return null;
                        }
                    
                        public SQLWarning getWarnings() throws SQLException {
                            return null;
                        }
                    
                        public boolean isClosed() throws SQLException {
                            return false;
                        }
                    
                        public boolean isReadOnly() throws SQLException {
                            return false;
                        }
                    
                        public String nativeSQL(String sql) throws SQLException {
                            return null;
                        }
                    
                        public CallableStatement prepareCall(String sql) throws SQLException {
                            return null;
                        }
                    
                        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
                            return null;
                        }
                    
                        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                            return null;
                        }
                    
                        public PreparedStatement prepareStatement(String sql) throws SQLException {
                            return null;
                        }
                    
                        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
                            return null;
                        }
                    
                        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
                            return null;
                        }
                    
                        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
                            return null;
                        }
                    
                        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                            return null;
                        }
                    
                        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
                            return null;
                        }
                    
                        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
                    
                        }
                    
                        public void rollback() throws SQLException {
                    
                        }
                    
                        public void rollback(Savepoint savepoint) throws SQLException {
                    
                        }
                    
                        public void setAutoCommit(boolean autoCommit) throws SQLException {
                    
                        }
                    
                        public void setCatalog(String catalog) throws SQLException {
                    
                        }
                    
                        public void setHoldability(int holdability) throws SQLException {
                    
                        }
                    
                        public void setReadOnly(boolean readOnly) throws SQLException {
                    
                        }
                    
                        public Savepoint setSavepoint() throws SQLException {
                            return null;
                        }
                    
                        public Savepoint setSavepoint(String name) throws SQLException {
                            return null;
                        }
                    
                        public void setTransactionIsolation(int level) throws SQLException {
                    
                        }
                    
                        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
                    
                        }
                    
                        public Clob createClob() throws SQLException {
                            return null;
                        }
                    
                        public Blob createBlob() throws SQLException {
                            return null;
                        }
                    
                        public NClob createNClob() throws SQLException {
                            return null;
                        }
                    
                        public SQLXML createSQLXML() throws SQLException {
                            return null;
                        }
                    
                        public boolean isValid(int timeout) throws SQLException {
                            return false;
                        }
                    
                        public void setClientInfo(String name, String value) throws SQLClientInfoException {
                    
                        }
                    
                        public void setClientInfo(Properties properties) throws SQLClientInfoException {
                    
                        }
                    
                        public String getClientInfo(String name) throws SQLException {
                            return null;
                        }
                    
                        public Properties getClientInfo() throws SQLException {
                            return null;
                        }
                    
                        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
                            return null;
                        }
                    
                        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
                            return null;
                        }
                    
                    }
                    """,
              """
                 package com.test.withoutV170Methods;
                 
                 import java.sql.*;
                 import java.util.Map;
                 import java.util.Properties;
                 
                 import javax.sql.*;
                 
                 public class JRE7JdbcConnection implements java.sql.Connection {
                 
                     public <T> T unwrap(Class<T> iface) throws SQLException {
                         return null;
                     }
                 
                     public boolean isWrapperFor(Class<?> iface) throws SQLException {
                         return false;
                     }
                 
                     public void clearWarnings() throws SQLException {
                 
                     }
                 
                     public void close() throws SQLException {
                 
                     }
                 
                     public void commit() throws SQLException {
                 
                     }
                 
                     public Statement createStatement() throws SQLException {
                 
                         return null;
                     }
                 
                     public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
                 
                         return null;
                     }
                 
                     public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                         return null;
                     }
                 
                     public boolean getAutoCommit() throws SQLException {
                         return false;
                     }
                 
                     public String getCatalog() throws SQLException {
                         return null;
                     }
                 
                     public int getHoldability() throws SQLException {
                         return 0;
                     }
                 
                     public DatabaseMetaData getMetaData() throws SQLException {
                         return null;
                     }
                 
                     public int getTransactionIsolation() throws SQLException {
                         return 0;
                     }
                 
                     public Map<String, Class<?>> getTypeMap() throws SQLException {
                         return null;
                     }
                 
                     public SQLWarning getWarnings() throws SQLException {
                         return null;
                     }
                 
                     public boolean isClosed() throws SQLException {
                         return false;
                     }
                 
                     public boolean isReadOnly() throws SQLException {
                         return false;
                     }
                 
                     public String nativeSQL(String sql) throws SQLException {
                         return null;
                     }
                 
                     public CallableStatement prepareCall(String sql) throws SQLException {
                         return null;
                     }
                 
                     public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
                         return null;
                     }
                 
                     public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                         return null;
                     }
                 
                     public PreparedStatement prepareStatement(String sql) throws SQLException {
                         return null;
                     }
                 
                     public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
                         return null;
                     }
                 
                     public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
                         return null;
                     }
                 
                     public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
                         return null;
                     }
                 
                     public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
                         return null;
                     }
                 
                     public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
                         return null;
                     }
                 
                     public void releaseSavepoint(Savepoint savepoint) throws SQLException {
                 
                     }
                 
                     public void rollback() throws SQLException {
                 
                     }
                 
                     public void rollback(Savepoint savepoint) throws SQLException {
                 
                     }
                 
                     public void setAutoCommit(boolean autoCommit) throws SQLException {
                 
                     }
                 
                     public void setCatalog(String catalog) throws SQLException {
                 
                     }
                 
                     public void setHoldability(int holdability) throws SQLException {
                 
                     }
                 
                     public void setReadOnly(boolean readOnly) throws SQLException {
                 
                     }
                 
                     public Savepoint setSavepoint() throws SQLException {
                         return null;
                     }
                 
                     public Savepoint setSavepoint(String name) throws SQLException {
                         return null;
                     }
                 
                     public void setTransactionIsolation(int level) throws SQLException {
                 
                     }
                 
                     public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
                 
                     }
                 
                     public Clob createClob() throws SQLException {
                         return null;
                     }
                 
                     public Blob createBlob() throws SQLException {
                         return null;
                     }
                 
                     public NClob createNClob() throws SQLException {
                         return null;
                     }
                 
                     public SQLXML createSQLXML() throws SQLException {
                         return null;
                     }
                 
                     public boolean isValid(int timeout) throws SQLException {
                         return false;
                     }
                 
                     public void setClientInfo(String name, String value) throws SQLClientInfoException {
                 
                     }
                 
                     public void setClientInfo(Properties properties) throws SQLClientInfoException {
                 
                     }
                 
                     public String getClientInfo(String name) throws SQLException {
                         return null;
                     }
                 
                     public Properties getClientInfo() throws SQLException {
                         return null;
                     }
                 
                     public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
                         return null;
                     }
                 
                     public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
                         return null;
                     }
                 
                     public void abort(java.util.concurrent.Executor executor) {
                     }
                 
                     public int getNetworkTimeout() {
                         return 0;
                     }
                 
                     public java.lang.String getSchema() {
                         return null;
                     }
                 
                     public void isWrapperFor(java.util.concurrent.Executor executor, int milliseconds) {
                     }
                 
                     public void setSchema(java.lang.String schema) throws java.sql.SQLException {
                     }
                 
                     public void isWrapperFor(java.util.concurrent.Executor executor, int milliseconds) {
                     }
                 
                 }
                 """
            ), 7)
        );
    }

    @Test
    void testDataSource() {
        rewriteRun(
          version(
            //language=java
            java("""
                    package com.test.withoutV170Methods;
                    
                    import javax.sql.ConnectionPoolDataSource;
                    import javax.sql.PooledConnection;
                    
                    import java.io.PrintWriter;
                    import java.sql.SQLException;
                    
                    public class JRE7JdbcCollectionPoolDataSource implements ConnectionPoolDataSource {
                    	public int getLoginTimeout() throws SQLException {
                    		return 0;
                    	}
                    
                    	public PrintWriter getLogWriter() throws SQLException {
                    		return null;
                    	}
                    
                    	public void setLoginTimeout(int seconds) throws SQLException {
                    		
                    	}
                    
                    	public void setLogWriter(PrintWriter out) throws SQLException {
                    		
                    	}
                    
                    	public PooledConnection getPooledConnection() throws SQLException {
                    		// TODO Auto-generated method stub
                    		return null;
                    	}
                    
                    	public PooledConnection getPooledConnection(String arg0, String arg1)
                    			throws SQLException {
                    		// TODO Auto-generated method stub
                    		return null;
                    	}
                    }
                    """,
              """
                 package com.test.withoutV170Methods;
                 
                 import javax.sql.ConnectionPoolDataSource;
                 import javax.sql.PooledConnection;
                 
                 import java.io.PrintWriter;
                 import java.sql.SQLException;
                 
                 public class JRE7JdbcCollectionPoolDataSource implements ConnectionPoolDataSource {
                 	public int getLoginTimeout() throws SQLException {
                 		return 0;
                 	}
                 
                 	public PrintWriter getLogWriter() throws SQLException {
                 		return null;
                 	}
                 
                 	public void setLoginTimeout(int seconds) throws SQLException {
                 
                 	}
                 
                 	public void setLogWriter(PrintWriter out) throws SQLException {
                 
                 	}
                 
                 	public PooledConnection getPooledConnection() throws SQLException {
                 		// TODO Auto-generated method stub
                 		return null;
                 	}
                 
                 	public PooledConnection getPooledConnection(String arg0, String arg1)
                 			throws SQLException {
                 		// TODO Auto-generated method stub
                 		return null;
                 	}
                 
                     public java.util.logging.Logger getParentLogger() {
                         return null;
                     }
                 }
                 """
            ), 6)
        );
    }
}
