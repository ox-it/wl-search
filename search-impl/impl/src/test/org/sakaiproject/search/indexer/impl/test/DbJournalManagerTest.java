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

package org.sakaiproject.search.indexer.impl.test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.search.journal.api.JournalExhausetedException;
import org.sakaiproject.search.journal.api.JournalManagerState;
import org.sakaiproject.search.journal.impl.DbJournalManager;

/**
 * @author ieb
 *
 */
public class DbJournalManagerTest extends TestCase
{

	private static final Log log = LogFactory.getLog(DbJournalManagerTest.class);
	private TestDataSource tds;

	/**
	 * @param name
	 */
	public DbJournalManagerTest(String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		tds = new TestDataSource();

	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	/**
	 * Test method for {@link org.sakaiproject.search.journal.impl.DbJournalManager#getNextVersion(long)}.
	 * @throws Exception 
	 */
	public final void testGetNextVersion() throws Exception
	{
		DbJournalManager dbJournalManager = new DbJournalManager();
		dbJournalManager.setDatasource(tds.getDataSource());
		for ( int i = 0; i < 10; i++ ) {
			JournalManagerState jmstate = dbJournalManager.prepareSave(i);
			dbJournalManager.commitSave(jmstate);
		}
		for ( int i = 0; i < 9; i++ ) {
			assertEquals("Incorrect Journal Number returned",i+1, dbJournalManager.getNextVersion(i));
		}
		try {
			dbJournalManager.getNextVersion(9);
			fail("Should have exhausted the journal");
		} catch ( JournalExhausetedException jex ) {
			
		}
		log.info("testGetNextVersion passed");
	}

	/**
	 * Test method for {@link org.sakaiproject.search.journal.impl.DbJournalManager#prepareSave(long)}.
	 * @throws Exception 
	 */
	public final void testPrepareSave() throws Exception
	{
		DbJournalManager dbJournalManager = new DbJournalManager();
		dbJournalManager.setDatasource(tds.getDataSource());
		for ( int i = 0; i < 10; i++ ) {
			assertNotNull("Journal State was null ",dbJournalManager.prepareSave(i));
		}
		log.info("testPrepareSave passed");
	}

	/**
	 * Test method for {@link org.sakaiproject.search.journal.impl.DbJournalManager#commitSave(org.sakaiproject.search.journal.api.JournalManagerState)}.
	 * @throws Exception 
	 */
	public final void testCommitSave() throws Exception
	{
		DbJournalManager dbJournalManager = new DbJournalManager();
		dbJournalManager.setDatasource(tds.getDataSource());
		for ( int i = 0; i < 10; i++ ) {
			JournalManagerState jmstate = dbJournalManager.prepareSave(i);
			dbJournalManager.commitSave(jmstate);
		}
		for ( int i = 0; i < 9; i++ ) {
			assertEquals("Incorrect Journal Number returned",i+1, dbJournalManager.getNextVersion(i));
		}
		try {
			dbJournalManager.getNextVersion(9);
			fail("Should have exhausted the journal");
		} catch ( JournalExhausetedException jex ) {
			
		}
		log.info("testCommitSave passed");
	}

	/**
	 * Test method for {@link org.sakaiproject.search.journal.impl.DbJournalManager#rollbackSave(org.sakaiproject.search.journal.api.JournalManagerState)}.
	 * @throws Exception 
	 */
	public final void testRollbackSave() throws Exception
	{
		DbJournalManager dbJournalManager = new DbJournalManager();
		dbJournalManager.setDatasource(tds.getDataSource());
		for ( int i = 0; i < 100; i++ ) {
			JournalManagerState jmstate = dbJournalManager.prepareSave(i);
			dbJournalManager.rollbackSave(jmstate);
		}
		try {
			long i = dbJournalManager.getNextVersion(20);
			fail("Should have exhausted the journal got "+i);
		} catch ( JournalExhausetedException jex ) {
			
		}
		log.info("testRollbackSave passed");

	}

	/**
	 * @throws Exception 
	 * 
	 */
	private void listAll() throws Exception
	{
		Connection connection = tds.getDataSource().getConnection();
		Statement s = connection.createStatement();
		ResultSet rs = s.executeQuery("select txid, txts,indexwriter from search_journal");
		log.info("Record ++++++++++");
		while (rs.next())
		{
			log.info("Record:" + rs.getString(1) + ":" + rs.getLong(2)+":"+rs.getString(3));
		}
		log.info("Record ----------");
		rs.close();
	}

}
