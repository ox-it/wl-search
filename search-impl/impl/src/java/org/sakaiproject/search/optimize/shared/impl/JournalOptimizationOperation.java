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

package org.sakaiproject.search.optimize.shared.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.search.journal.api.JournalErrorException;
import org.sakaiproject.search.journal.api.ManagementOperation;
import org.sakaiproject.search.optimize.api.NoOptimizationRequiredException;
import org.sakaiproject.search.optimize.shared.api.JournalOptimizationManager;
import org.sakaiproject.search.optimize.shared.api.JournalOptimizationTransaction;
import org.sakaiproject.search.transaction.api.IndexTransactionException;

/**
 * Performs an optimize operation using an OptimizIndexManager to manage the
 * 2PC.
 * 
 * @author ieb
 */
public class JournalOptimizationOperation implements ManagementOperation
{

	private static final Log log = LogFactory.getLog(JournalOptimizationOperation.class);

	/**
	 * The manager that performs the optimization
	 */
	private JournalOptimizationManager journalOptimizationManager;

	public void init()
	{
	}

	/**
	 * @see org.sakaiproject.search.journal.api.ManagementOperation#runOnce()
	 */
	public void runOnce()
	{

		/*
		 * Run the optimizer transaction once
		 */
		JournalOptimizationTransaction journalOptimizationTransaction = null;
		try
		{
			Map<String, Object> m = new HashMap<String, Object>();
			journalOptimizationTransaction = (JournalOptimizationTransaction) journalOptimizationManager
					.openTransaction(m);
			journalOptimizationTransaction.prepare();
			journalOptimizationTransaction.commit();
			log.info("Optimize complete ");
		}
		catch (NoOptimizationRequiredException nop)
		{
			log.info("No Merge Performed " + nop.getMessage());
		}
		catch (JournalErrorException jex)
		{
			if (journalOptimizationTransaction != null)
			{
				log.warn("Failed to compete Optimize ", jex);
			}
			else
			{
				log.warn("Failed to start merge operation ", jex);

			}
			try
			{
				journalOptimizationTransaction.rollback();
			}
			catch (Exception ex)
			{
				log.warn("Failed to rollback transaction ", ex);
			}
		}
		catch (IndexTransactionException iupex)
		{

			log.warn("Failed to compete optimize ", iupex);
			try
			{
				journalOptimizationTransaction.rollback();
			}
			catch (Exception ex)
			{
				log.warn("Failed to rollback transaction ", ex);
			}
		}
		finally
		{
			try
			{
				journalOptimizationTransaction.close();
			}
			catch (Exception ex)
			{
			}

		}
	}

	/**
	 * @return the journalOptimizationManager
	 */
	public JournalOptimizationManager getJournalOptimizationManager()
	{
		return journalOptimizationManager;
	}

	/**
	 * @param journalOptimizationManager the journalOptimizationManager to set
	 */
	public void setJournalOptimizationManager(
			JournalOptimizationManager journalOptimizationManager)
	{
		this.journalOptimizationManager = journalOptimizationManager;
	}

}