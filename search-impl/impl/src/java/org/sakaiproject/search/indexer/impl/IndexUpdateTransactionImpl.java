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

package org.sakaiproject.search.indexer.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.sakaiproject.search.indexer.api.IndexUpdateTransaction;
import org.sakaiproject.search.indexer.api.NoItemsToIndexException;
import org.sakaiproject.search.model.SearchBuilderItem;
import org.sakaiproject.search.transaction.api.IndexTransaction;
import org.sakaiproject.search.transaction.api.IndexTransactionException;
import org.sakaiproject.search.transaction.impl.IndexTransactionImpl;
import org.sakaiproject.search.transaction.impl.TransactionManagerImpl;
import org.sakaiproject.search.util.FileUtils;

/**
 * A trnasaction to manage the 2PC of a journaled indexing operation, this is created by a Transaction Manager
 * @author ieb
 * 
 * Unit test @see org.sakaiproject.search.indexer.impl.test.TransactionalIndexWorkerTest
 */
public class IndexUpdateTransactionImpl extends IndexTransactionImpl implements IndexUpdateTransaction
{

	private static final Log log = LogFactory.getLog(IndexUpdateTransactionImpl.class);

	private IndexWriter indexWriter;

	private File tempIndex;

	private List<SearchBuilderItem> txList;

	private SearchBuilderItemSerializer searchBuilderItemSerializer = new SearchBuilderItemSerializer();

	/**
	 * @param m
	 * @param impl
	 * @throws IndexTransactionException
	 */
	public IndexUpdateTransactionImpl(TransactionManagerImpl manager,
			Map<String, Object> m) throws IndexTransactionException
	{
		super(manager, m);
	}

	/**
	 * @see org.sakaiproject.search.component.service.index.transactional.api.IndexUpdateTransaction#addItemIterator()
	 */
	public Iterator<SearchBuilderItem> addItemIterator() throws IndexTransactionException
	{
		if (transactionState != IndexTransaction.STATUS_ACTIVE)
		{
			throw new IndexTransactionException("Transaction is not active ");
		}

		if ( log.isDebugEnabled() ) {
			log.debug("Tx list on "+this+" is now "+txList);
		}
		return txList.iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.search.transaction.impl.IndexTransactionImpl#doBeforePrepare()
	 */
	@Override
	protected void doBeforePrepare() throws IndexTransactionException
	{
		try
		{
			transactionId = manager.getSequence().getNextId();
			indexWriter.close();
			indexWriter = null;
			searchBuilderItemSerializer.saveTransactionList(tempIndex, txList);
		}
		catch (Exception ex)
		{
			throw new IndexTransactionException("Failed to prepare transaction", ex);
		}
		super.doBeforePrepare();
	}


	/* (non-Javadoc)
	 * @see org.sakaiproject.search.transaction.impl.IndexTransactionImpl#doAfterCommit()
	 */
	@Override
	protected void doAfterCommit() throws IndexTransactionException
	{
		try
		{
			FileUtils.deleteAll(tempIndex);
		}
		catch (Exception e)
		{
			throw new IndexTransactionException("Failed to commit ", e);
		}
		super.doAfterCommit();
	}


	/**
	 * @see org.sakaiproject.search.component.service.index.transactional.api.IndexUpdateTransaction#getIndexWriter()
	 */
	public IndexWriter getIndexWriter() throws IndexTransactionException
	{
		if (transactionState != IndexTransaction.STATUS_ACTIVE)
		{
			throw new IndexTransactionException("Transaction is not active ");
		}
		if (indexWriter == null)
		{
			try
			{
				tempIndex = ((TransactionIndexManagerImpl)manager).getTemporarySegment(transactionId);
				indexWriter = new IndexWriter(tempIndex, ((TransactionIndexManagerImpl)manager).getAnalyzer(), true);
				indexWriter.setUseCompoundFile(true);
				// indexWriter.setInfoStream(System.out);
				indexWriter.setMaxMergeDocs(50);
				indexWriter.setMergeFactor(50);
			}
			catch (IOException ex)
			{
				throw new IndexTransactionException(
						"Cant Create Transaction Index working space ", ex);
			}
		}
		return indexWriter;
	}

	/**
	 * The name of the temp index shouldbe used to locate the index, and NOT the
	 * transaction ID.
	 * 
	 * @see org.sakaiproject.search.component.service.index.transactional.api.IndexUpdateTransaction#getTempIndex()
	 */
	public String getTempIndex()
	{
		return tempIndex.getAbsolutePath();
	}

	/**
	 * The transaction ID will change as the status cahnges. While the
	 * transaction is active it will have a local ID, when the transaction is
	 * prepared the cluster wide transaction id will be created. Once prepare
	 * has been performed, the transaction should be committed
	 * 
	 * @see org.sakaiproject.search.component.service.index.transactional.api.IndexUpdateTransaction#getTransactionId()
	 */
	public long getTransactionId()
	{

		return transactionId;
	}

	
	
	/* (non-Javadoc)
	 * @see org.sakaiproject.search.transaction.impl.IndexTransactionImpl#doBeforeRollback()
	 */
	@Override
	protected void doBeforeRollback() throws IndexTransactionException
	{
		try
		{
		indexWriter.close();
		indexWriter = null;
		searchBuilderItemSerializer.saveTransactionList(tempIndex, txList);
		}
		catch (Exception ex)
		{
			throw new IndexTransactionException("Failed to start rollback ",ex);
		}
		super.doBeforeRollback();
	}
	
	/* (non-Javadoc)
	 * @see org.sakaiproject.search.transaction.impl.IndexTransactionImpl#doAfterRollback()
	 */
	@Override
	protected void doAfterRollback() throws IndexTransactionException
	{
		super.doAfterRollback();
		try
		{
			FileUtils.deleteAll(tempIndex);
		}
		catch (Exception ex)
		{
			throw new IndexTransactionException("Failed to complete rollback ",ex);
		}
		tempIndex = null;
	}


	/**
	 * @see org.sakaiproject.search.component.service.index.transactional.api.IndexUpdateTransaction#setItems(java.util.List)
	 */
	public void setItems(List<SearchBuilderItem> items) throws IndexTransactionException
	{
		super.setItems(items);
		
		txList = new ArrayList<SearchBuilderItem>();
		for (Iterator<SearchBuilderItem> itxList = items.iterator(); itxList.hasNext();)
		{
			SearchBuilderItem sbi = itxList.next();
			if (sbi != null
					&& SearchBuilderItem.STATE_LOCKED.equals(sbi.getSearchstate())
					&& SearchBuilderItem.ACTION_ADD.equals(sbi.getSearchaction()))
			{
				String ref = sbi.getName();
				if (ref != null)
				{
					txList.add(sbi);
				}
				else
				{
					log.warn("Null Reference presented to Index Queue, ignoring ");
				}
			}
		}
		if (txList.size() == 0)
		{
			log.warn("No Items found to index, only deletes");
			txList = null;
			items = null;
			throw new NoItemsToIndexException("No Items available to index");
		}
	}



}
