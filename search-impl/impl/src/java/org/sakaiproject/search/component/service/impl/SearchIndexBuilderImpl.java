/**********************************************************************************
 *
 * $Header$
 *
 ***********************************************************************************
 *
 * Copyright (c) 2006 University of Cambridge
 * 
 * Licensed under the Educational Community License Version 1.0 (the "License");
 * By obtaining, using and/or copying this Original Work, you agree that you have read,
 * understand, and will comply with the terms and conditions of the Educational Community License.
 * You may obtain a copy of the License at:
 * 
 *      http://cvs.sakaiproject.org/licenses/license_1_0.html
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 **********************************************************************************/

package org.sakaiproject.search.component.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.Notification;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.search.api.EntityContentProducer;
import org.sakaiproject.search.api.SearchIndexBuilder;
import org.sakaiproject.search.dao.SearchBuilderItemDao;
import org.sakaiproject.search.model.SearchBuilderItem;

/**
 * Search index builder is expected to be registered in spring as
 * org.sakaiproject.search.api.SearchIndexBuilder as a singleton. It receives
 * resources which it adds to its list of pending documents to be indexed. A
 * seperate thread then runs thtough the list of entities to be indexed,
 * updating the index. Each time the index is updates an event is posted to
 * force the Search components that are using the index to reload. Incremental
 * updates to the Lucene index require that the searchers reload the index once
 * the idex writer has been built.
 * 
 * @author ieb
 */

public class SearchIndexBuilderImpl implements SearchIndexBuilder
{

	private static Log log = LogFactory.getLog(SearchIndexBuilderImpl.class);

	private SearchBuilderItemDao searchBuilderItemDao = null;

	private SearchIndexBuilderWorker searchIndexBuilderWorker = null;

	private List producers = new ArrayList();

	public void init()
	{
		ComponentManager cm = org.sakaiproject.component.cover.ComponentManager
				.getInstance();
		searchIndexBuilderWorker = (SearchIndexBuilderWorker) load(cm,
				SearchIndexBuilderWorker.class.getName());
		try
		{

		}
		catch (Throwable t)
		{
			log.error("Failed to init ", t);
		}
	}

	private Object load(ComponentManager cm, String name)
	{
		Object o = cm.get(name);
		if (o == null)
		{
			log.error("Cant find Spring component named " + name);
		}
		return o;
	}

	/**
	 * register an entity content producer to provide content to the search
	 * engine {@inheritDoc}
	 */
	public void registerEntityContentProducer(EntityContentProducer ecp)
	{
		log.debug("register " + ecp);
		producers.add(ecp);
	}

	/**
	 * Add a resource to the indexing queue {@inheritDoc}
	 */
	public void addResource(Notification notification, Event event)
	{
		log.debug("Add resource " + notification + "::" + event);
		String resourceName = event.getResource();
		EntityContentProducer ecp = newEntityContentProducer(event);
		Integer action = ecp.getAction(event);
		int retries = 5;
		while (retries > 0)
		{
			try
			{
				SearchBuilderItem sb = searchBuilderItemDao
						.findByName(resourceName);
				if (sb == null)
				{
					// new
					sb = searchBuilderItemDao.create();
					sb.setSearchaction(action);
					sb.setName(resourceName);
					sb.setSearchstate(SearchBuilderItem.STATE_PENDING);
					log.debug(" Didnt Find Resource, created new ");
				}
				else
				{
					sb.setSearchaction(action);
					sb.setName(resourceName);
					sb.setSearchstate(SearchBuilderItem.STATE_PENDING);
				}
				searchBuilderItemDao.update(sb);
				sb = searchBuilderItemDao.findByName(resourceName);
				log.debug(" Added Resource " + action + " " + sb.getName());
				break;
			}
			catch (Throwable t)
			{
				log.warn("Retrying to register " + resourceName
						+ " with the search engine ", t);
				retries--;
			}
		}
		if (retries == 0)
		{
			log.warn("In trying to register resource " + resourceName
					+ " in search engine, I failed after"
					+ " 5 attempts, this resource will"
					+ " not be indexed untill it is modified");
		}
		restartBuilder();
	}

	/**
	 * refresh the index from the current stored state {@inheritDoc}
	 */
	public void refreshIndex()
	{
		SearchBuilderItem sb = searchBuilderItemDao
				.findByName(SearchBuilderItem.INDEX_MASTER);
		if (sb == null)
		{
			log.debug("Created NEW " + SearchBuilderItem.INDEX_MASTER);
			sb = searchBuilderItemDao.create();
		}
		if (!SearchBuilderItem.ACTION_REBUILD.equals(sb.getSearchaction()))
		{
			sb.setSearchaction(SearchBuilderItem.ACTION_REFRESH);
			sb.setName(SearchBuilderItem.INDEX_MASTER);
			sb.setSearchstate(SearchBuilderItem.STATE_PENDING);
			searchBuilderItemDao.update(sb);
			restartBuilder();
		}
	}

	public void destroy()
	{
		searchIndexBuilderWorker.destroy();
	}

	/*
	 * List l = searchBuilderItemDao.getAll(); for (Iterator i = l.iterator();
	 * i.hasNext();) { SearchBuilderItemImpl sbi = (SearchBuilderItemImpl)
	 * i.next(); sbi.setSearchstate(SearchBuilderItem.STATE_PENDING);
	 * sbi.setSearchaction(SearchBuilderItem.ACTION_ADD); try { log.info("
	 * Updating " + sbi.getName()); searchBuilderItemDao.update(sbi); } catch
	 * (Exception ex) { try { sbi = (SearchBuilderItemImpl) searchBuilderItemDao
	 * .findByName(sbi.getName()); if (sbi != null) {
	 * sbi.setSearchstate(SearchBuilderItem.STATE_PENDING);
	 * sbi.setSearchaction(SearchBuilderItem.ACTION_ADD);
	 * searchBuilderItemDao.update(sbi); } } catch (Exception e) {
	 * log.warn("Failed to update on second attempt " + e.getMessage()); } } }
	 * restartBuilder(); }
	 */
	/**
	 * Rebuild the index from the entities own stored state {@inheritDoc}
	 */
	public void rebuildIndex()
	{

		try
		{
			SearchBuilderItem sb = searchBuilderItemDao
					.findByName(SearchBuilderItem.INDEX_MASTER);
			if (sb == null)
			{
				log.info("Created NEW " + SearchBuilderItem.INDEX_MASTER);
				sb = searchBuilderItemDao.create();
			}
			sb.setSearchaction(SearchBuilderItem.ACTION_REBUILD);
			sb.setName(SearchBuilderItem.INDEX_MASTER);
			sb.setSearchstate(SearchBuilderItem.STATE_PENDING);
			searchBuilderItemDao.update(sb);
		}
		catch (Exception ex)
		{
			log.warn(" rebuild index encountered a problme " + ex.getMessage());
		}
		restartBuilder();
	}

	/*
	 * for (Iterator i = producers.iterator(); i.hasNext();) {
	 * EntityContentProducer ecp = (EntityContentProducer) i.next(); List
	 * contentList = ecp.getAllContent(); for (Iterator ci =
	 * contentList.iterator(); ci.hasNext();) { String resourceName = (String)
	 * ci.next(); } } }
	 */
	/**
	 * This adds and event to the list and if necessary starts a processing
	 * thread The method is syncronised with removeFromList
	 * 
	 * @param e
	 */
	private void restartBuilder()
	{
		searchIndexBuilderWorker.checkRunning();
	}

	/**
	 * Generates a SearchableEntityProducer
	 * 
	 * @param ref
	 * @return
	 * @throws PermissionException
	 * @throws IdUnusedException
	 * @throws TypeException
	 */
	protected EntityContentProducer newEntityContentProducer(Reference ref)
	{
		log.debug(" new entitycontent producer");
		for (Iterator i = producers.iterator(); i.hasNext();)
		{
			EntityContentProducer ecp = (EntityContentProducer) i.next();
			if (ecp.matches(ref))
			{
				return ecp;
			}
		}
		return null;
	}

	/**
	 * get hold of an entity content producer using the event
	 * 
	 * @param event
	 * @return
	 */
	protected EntityContentProducer newEntityContentProducer(Event event)
	{
		log.debug(" new entitycontent producer");
		for (Iterator i = producers.iterator(); i.hasNext();)
		{
			EntityContentProducer ecp = (EntityContentProducer) i.next();
			if (ecp.matches(event))
			{
				return ecp;
			}
		}
		return null;
	}

	/**
	 * @return Returns the searchBuilderItemDao.
	 */
	public SearchBuilderItemDao getSearchBuilderItemDao()
	{
		return searchBuilderItemDao;
	}

	/**
	 * @param searchBuilderItemDao
	 *        The searchBuilderItemDao to set.
	 */
	public void setSearchBuilderItemDao(
			SearchBuilderItemDao searchBuilderItemDao)
	{
		this.searchBuilderItemDao = searchBuilderItemDao;
	}

	/**
	 * return true if the queue is empty
	 * 
	 * @{inheritDoc}
	 */
	public boolean isBuildQueueEmpty()
	{
		int n = searchBuilderItemDao.countPending();
		log.debug("Queue has " + n);
		return (n == 0);
	}


	/**
	 * get all the producers registerd, as a clone to avoid concurrent
	 * modification exceptions
	 * 
	 * @return
	 */
	public List getContentProducers()
	{
		return new ArrayList(producers);
	}

	public int getPendingDocuments()
	{
		return searchBuilderItemDao.countPending();
	}

}
