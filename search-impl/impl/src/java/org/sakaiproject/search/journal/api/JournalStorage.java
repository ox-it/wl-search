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

package org.sakaiproject.search.journal.api;

import java.io.IOException;


/**
 * Journal storare provides bulk storage to the journal objects being stored.
 * @author ieb
 *
 */
public interface JournalStorage
{

	/**
	 * Get a version from the commited store into a permanent local space, indexed on the transaction or version id
	 * @param version
	 * @param workingSpace
	 * @throws IOException 
	 */
	void retrieveVersion(long version, String workingSpace) throws IOException;

	/**
	 * prepare the current transaction for commit, 2PC
	 * @param location
	 * @param transactionId
	 * @return
	 * @throws IOException 
	 */
	JournalStorageState prepareSave(String location, long transactionId) throws IOException;

	/**
	 * perform the commit on the transaction in jss
	 * @param jss
	 * @throws IOException 
	 */
	void commitSave(JournalStorageState jss) throws IOException;

	/**
	 * rollback the transaction in jss
	 * @param jss
	 */
	void rollbackSave(JournalStorageState jss);

	
}