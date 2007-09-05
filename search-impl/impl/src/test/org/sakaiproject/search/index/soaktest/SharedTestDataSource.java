/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.search.index.soaktest;

import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp.datasources.SharedPoolDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.search.model.SearchBuilderItem;

/**
 * @author ieb
 */
public class SharedTestDataSource
{
	private static final Log log = LogFactory.getLog(SharedTestDataSource.class);

	private SharedPoolDataSource tds;

	private DataSource wds;

	protected int nopen = 0;

	private int docid = 0;


	public SharedTestDataSource(String dblocation, int pool, final boolean poolLogging, String driver, String url, String username, String password) throws Exception
	{
		DriverAdapterCPDS cpds = new DriverAdapterCPDS();
		cpds.setDriver("org.hsqldb.jdbcDriver");
		cpds.setUrl("jdbc:hsqldb:mem:aname");
		cpds.setUser("sa");
		cpds.setPassword("");

		tds = new SharedPoolDataSource();
		tds.setConnectionPoolDataSource(cpds);
		tds.setMaxActive(pool);
		tds.setMaxWait(5);
		tds.setDefaultAutoCommit(false);

		wds = new DataSource()
		{

			public Connection getConnection() throws SQLException
			{
				final Connection c = tds.getConnection();
				nopen++;
				if ( poolLogging) log.info("++++++++++++Opened " + nopen);
				Exception ex = new Exception();
				StackTraceElement[] ste = ex.getStackTrace();
				log.debug("Stack Trace " + ste[1].toString());
				return new Connection()
				{

					public void clearWarnings() throws SQLException
					{
						c.clearWarnings();
					}

					public void close() throws SQLException
					{
						c.close();
						nopen--;
						if ( poolLogging) log.info("------------Closed " + nopen);

					}

					public void commit() throws SQLException
					{
						c.commit();
					}

					public Statement createStatement() throws SQLException
					{

						return c.createStatement();
					}

					public Statement createStatement(int resultSetType,
							int resultSetConcurrency) throws SQLException
					{
						return c.createStatement(resultSetType, resultSetConcurrency);
					}

					public Statement createStatement(int resultSetType,
							int resultSetConcurrency, int resultSetHoldability)
							throws SQLException
					{
						return c.createStatement(resultSetType, resultSetConcurrency,
								resultSetHoldability);
					}

					public boolean getAutoCommit() throws SQLException
					{
						return c.getAutoCommit();
					}

					public String getCatalog() throws SQLException
					{
						return c.getCatalog();
					}

					public int getHoldability() throws SQLException
					{
						return c.getHoldability();
					}

					public DatabaseMetaData getMetaData() throws SQLException
					{
						return c.getMetaData();
					}

					public int getTransactionIsolation() throws SQLException
					{
						return c.getTransactionIsolation();
					}

					public Map<String, Class<?>> getTypeMap() throws SQLException
					{
						return c.getTypeMap();
					}

					public SQLWarning getWarnings() throws SQLException
					{
						return c.getWarnings();
					}

					public boolean isClosed() throws SQLException
					{
						return c.isClosed();
					}

					public boolean isReadOnly() throws SQLException
					{
						return c.isReadOnly();
					}

					public String nativeSQL(String sql) throws SQLException
					{
						return c.nativeSQL(sql);
					}

					public CallableStatement prepareCall(String sql) throws SQLException
					{
						return c.prepareCall(sql);
					}

					public CallableStatement prepareCall(String sql, int resultSetType,
							int resultSetConcurrency) throws SQLException
					{
						return c.prepareCall(sql, resultSetType, resultSetConcurrency);
					}

					public CallableStatement prepareCall(String sql, int resultSetType,
							int resultSetConcurrency, int resultSetHoldability)
							throws SQLException
					{
						return c.prepareCall(sql, resultSetType, resultSetConcurrency,
								resultSetHoldability);
					}

					public PreparedStatement prepareStatement(String sql)
							throws SQLException
					{
						return c.prepareStatement(sql);
					}

					public PreparedStatement prepareStatement(String sql,
							int autoGeneratedKeys) throws SQLException
					{
						return c.prepareStatement(sql, autoGeneratedKeys);
					}

					public PreparedStatement prepareStatement(String sql,
							int[] columnIndexes) throws SQLException
					{
						return c.prepareStatement(sql, columnIndexes);
					}

					public PreparedStatement prepareStatement(String sql,
							String[] columnNames) throws SQLException
					{
						return c.prepareStatement(sql, columnNames);
					}

					public PreparedStatement prepareStatement(String sql,
							int resultSetType, int resultSetConcurrency)
							throws SQLException
					{
						return c.prepareStatement(sql, resultSetType,
								resultSetConcurrency);
					}

					public PreparedStatement prepareStatement(String sql,
							int resultSetType, int resultSetConcurrency,
							int resultSetHoldability) throws SQLException
					{
						return c.prepareStatement(sql, resultSetType,
								resultSetConcurrency, resultSetHoldability);
					}

					public void releaseSavepoint(Savepoint savepoint) throws SQLException
					{
						c.releaseSavepoint(savepoint);
					}

					public void rollback() throws SQLException
					{
						c.rollback();

					}

					public void rollback(Savepoint savepoint) throws SQLException
					{
						c.rollback(savepoint);

					}

					public void setAutoCommit(boolean autoCommit) throws SQLException
					{
						c.setAutoCommit(autoCommit);

					}

					public void setCatalog(String catalog) throws SQLException
					{
						c.setCatalog(catalog);

					}

					public void setHoldability(int holdability) throws SQLException
					{
						c.setHoldability(holdability);

					}

					public void setReadOnly(boolean readOnly) throws SQLException
					{
						c.setReadOnly(readOnly);

					}

					public Savepoint setSavepoint() throws SQLException
					{
						return c.setSavepoint();
					}

					public Savepoint setSavepoint(String name) throws SQLException
					{
						return c.setSavepoint(name);
					}

					public void setTransactionIsolation(int level) throws SQLException
					{
						c.setTransactionIsolation(level);

					}

					public void setTypeMap(Map<String, Class<?>> map) throws SQLException
					{
						c.setTypeMap(map);

					}

				};
			}

			public Connection getConnection(String username, String password)
					throws SQLException
			{
				return tds.getConnection(username, password);
			}

			public PrintWriter getLogWriter() throws SQLException
			{
				return tds.getLogWriter();
			}

			public int getLoginTimeout() throws SQLException
			{
				// TODO Auto-generated method stub
				return tds.getLoginTimeout();
			}

			public void setLogWriter(PrintWriter out) throws SQLException
			{
				tds.setLogWriter(out);
			}

			public void setLoginTimeout(int seconds) throws SQLException
			{
				tds.setLoginTimeout(seconds);
			}

		};

		Connection connection = tds.getConnection();
		Statement s = connection.createStatement();
		try
		{
			s.execute("create table search_transaction ( txname varchar, txid bigint )");
		}
		catch (Exception ex)
		{
			log.warn("Create Table Said :" + ex.getMessage());
		}
		try
		{
			s.execute("CREATE TABLE searchbuilderitem ( id varchar(64) NOT NULL, "
					+ " version datetime NOT NULL, " + " name varchar(255) NOT NULL, "
					+ " context varchar(255) NOT NULL, "
					+ " searchaction int default NULL, "
					+ " searchstate int default NULL, " + " itemscope int default NULL, "
					+ " PRIMARY KEY  (id), " + " UNIQUE (name) )");
		}
		catch (Exception ex)
		{
			log.warn("Create Table Said :" + ex.getMessage());
		}
		try
		{
			s.execute("CREATE TABLE search_journal ( " + " txid bigint NOT NULL, "
					+ " txts bigint NOT NULL, " + " indexwriter varchar(255) NOT NULL, "
					+ " PRIMARY KEY  (txid) )");
		}
		catch (Exception ex)
		{
			log.warn("Create Table Said :" + ex.getMessage());
		}

		try
		{
			s.execute("CREATE TABLE search_node_status ( " + " jid bigint NOT NULL, "
					+ " jidts bigint NOT NULL, " + " serverid varchar(255) NOT NULL, "
					+ " PRIMARY KEY  (serverid) )");
		}
		catch (Exception ex)
		{
			log.warn("Create Table Said :" + ex.getMessage());
		}

		connection.commit();
		connection.close();
	}

	/**
	 * @return
	 */
	public DataSource getDataSource()
	{
		return wds;

	}

	/**
	 * @throws Exception
	 */
	public void close() throws Exception
	{
		tds.close();
	}

	public int populateDocuments(long targetItems, String instanceName) throws SQLException
	{
		int nitems = 0;
		Connection connection = null;
		PreparedStatement insertPST = null;
		try
		{
			connection = getDataSource().getConnection();
			insertPST = connection
					.prepareStatement("insert into searchbuilderitem "
							+ "(id,version,name,context,searchaction,searchstate,itemscope) values "
							+ "(?,?,?,?,?,?,?)");
			for (int i = 0; i < targetItems; i++)
			{
				int state = i % SearchBuilderItem.states.length;
				String name = SearchBuilderItem.states[state];
				int action = i % 3;
				if (state == SearchBuilderItem.STATE_PENDING
						&& action == SearchBuilderItem.ACTION_ADD)
				{
					nitems++;
				}
				insertPST.clearParameters();
				insertPST.setString(1, String.valueOf(instanceName+System.currentTimeMillis())
						+ String.valueOf(i)+":"+String.valueOf(docid++));
				insertPST.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
				insertPST.setString(3, "/"+instanceName+ "/" + name + "/at/a/location/" + docid++);
				insertPST.setString(4, "/" + name + "/at/a");
				insertPST.setInt(5, action);
				insertPST.setInt(6, state);
				insertPST.setInt(7, SearchBuilderItem.ITEM);
				insertPST.execute();
			}
			connection.commit();
		}
		finally
		{
			try
			{
				insertPST.close();
			}
			catch (Exception ex2)
			{
			}
			try
			{
				connection.close();
			}
			catch (Exception ex2)
			{
			}
		}
		return nitems;

	}

}