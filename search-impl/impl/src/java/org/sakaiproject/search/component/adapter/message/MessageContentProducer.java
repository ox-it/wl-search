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

package org.sakaiproject.search.component.adapter.message;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.message.api.Message;
import org.sakaiproject.message.api.MessageChannel;
import org.sakaiproject.message.api.MessageHeader;
import org.sakaiproject.message.api.MessageService;
import org.sakaiproject.search.api.EntityContentProducer;
import org.sakaiproject.search.api.SearchIndexBuilder;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.search.model.SearchBuilderItem;


/**
 * @author ieb
 */
public class MessageContentProducer implements EntityContentProducer
{

	/**
	 * debug logger
	 */
	private static Log log = LogFactory.getLog(MessageContentProducer.class);

	// runtime dependency
	private String toolName = null;

	// runtime dependency
	private List addEvents = null;

	// runtime dependency
	private List removeEvents = null;

	// injected dependency
	private MessageService messageService = null;

	// runtime dependency
	private SearchService searchService = null;

	// runtime dependency
	private SearchIndexBuilder searchIndexBuilder = null;

	private EntityManager entityManager = null;

	public void init()
	{
		ComponentManager cm = org.sakaiproject.component.cover.ComponentManager
				.getInstance();

		searchService = (SearchService) load(cm, SearchService.class.getName());
		searchIndexBuilder = (SearchIndexBuilder) load(cm,
				SearchIndexBuilder.class.getName());

		entityManager = (EntityManager) load(cm, EntityManager.class.getName());
		
		if ("true".equals(ServerConfigurationService
				.getString("wiki.experimental")))
		{
			for (Iterator i = addEvents.iterator(); i.hasNext();)
			{
				searchService.registerFunction((String) i.next());
			}
			for (Iterator i = removeEvents.iterator(); i.hasNext();)
			{
				searchService.registerFunction((String) i.next());
			}
			searchIndexBuilder.registerEntityContentProducer(this);
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
	 * {@inheritDoc}
	 */
	public boolean isContentFromReader(Entity cr)
	{
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Reader getContentReader(Entity cr)
	{
		return new StringReader(getContent(cr));
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContent(Entity cr)
	{
		log.info(" Getting content " + cr);
		Reference ref = entityManager.newReference(cr.getReference());
		log.info(" Got reference " + ref);
		EntityProducer ep = ref.getEntityProducer();
		log.info(" Got ep " + ep);

		if (ep instanceof MessageService)
		{
			try
			{
				MessageService ms = (MessageService) ep;
				Message m = ms.getMessage(ref);
				MessageHeader mh = m.getHeader();
				StringBuffer sb = new StringBuffer();
				sb.append("Message Headers\n");
				sb.append("From ").append(mh.getFrom().getDisplayName())
						.append("\n");
				sb.append("Message Body\n");
				sb.append(m.getBody()).append("\n");
				log.info("Index Content is " + sb.toString());
				return sb.toString();
			}
			catch (IdUnusedException e)
			{
				throw new RuntimeException(" Failed to get message content ", e);
			}
			catch (PermissionException e)
			{
				throw new RuntimeException(" Failed to get message content ", e);
			}
		}
		throw new RuntimeException(" Not a Message Entity " + cr);
	}

	/**
	 * @{inheritDoc}
	 */
	public String getTitle(Entity cr)
	{
		Reference ref = entityManager.newReference(cr.getReference());
		EntityProducer ep = ref.getEntityProducer();
		if (ep instanceof MessageService)
		{
			try
			{
				MessageService ms = (MessageService) ep;
				Message m = ms.getMessage(ref);
				MessageHeader mh = m.getHeader();
				return "Message From " + mh.getFrom().getDisplayName();
			}
			catch (IdUnusedException e)
			{
				throw new RuntimeException(" Failed to get message content ", e);
			}
			catch (PermissionException e)
			{
				throw new RuntimeException(" Failed to get message content ", e);
			}
		}
		throw new RuntimeException(" Not a Message Entity " + cr);

	}

	/**
	 * @{inheritDoc}
	 */
	public String getUrl(Entity entity)
	{
		return entity.getUrl();
	}

	/**
	 * @{inheritDoc}
	 */
	public boolean matches(Reference ref)
	{

		EntityProducer ep = ref.getEntityProducer();

		if (ep.getClass().equals(messageService.getClass()))
		{
			return true;
		}
		return false;
	}

	/**
	 * @{inheritDoc}
	 */
	public List getAllContent()
	{
		List all = new ArrayList();
		List l = messageService.getChannels();
		for (Iterator i = l.iterator(); i.hasNext();)
		{
			try
			{
				MessageChannel c = (MessageChannel) i.next();

				List messages = c.getMessages(null, true);
				// WARNING: I think the implementation caches on thread, if this
				// is
				// a builder
				// thread this may not work
				for (Iterator mi = messages.iterator(); mi.hasNext();)
				{
					Message m = (Message) i.next();
					all.add(m.getReference());
				}
			}
			catch (Exception ex)
			{

			}
		}
		return all;
	}

	/**
	 * @{inheritDoc}
	 */
	public Integer getAction(Event event)
	{
		String evt = event.getEvent();
		if (evt == null) return SearchBuilderItem.ACTION_UNKNOWN;
		for (Iterator i = addEvents.iterator(); i.hasNext();)
		{
			String match = (String) i.next();
			if (evt.equals(match))
			{
				log.info(" event is add " + evt);
				return SearchBuilderItem.ACTION_ADD;
			}
			else
			{
				log.info(" no match " + evt + ":" + match);
			}
		}
		for (Iterator i = removeEvents.iterator(); i.hasNext();)
		{
			String match = (String) i.next();
			if (evt.equals(match))
			{
				log.info(" event is delete " + evt);
				return SearchBuilderItem.ACTION_DELETE;
			}
			else
			{
				log.info(" no match " + evt + ":" + match);
			}
		}
		log.info(" event is unknown ");
		return SearchBuilderItem.ACTION_UNKNOWN;
	}

	/**
	 * @{inheritDoc}
	 */
	public boolean matches(Event event)
	{
		Reference ref = entityManager.newReference(event.getResource());
		return matches(ref);
	}

	/**
	 * @{inheritDoc}
	 */
	public String getTool()
	{
		return toolName;
	}

	/**
	 * @return Returns the addEvents.
	 */
	public List getAddEvents()
	{
		return addEvents;
	}

	/**
	 * @param addEvents
	 *        The addEvents to set.
	 */
	public void setAddEvents(List addEvents)
	{
		this.addEvents = addEvents;
	}

	/**
	 * @return Returns the messageService.
	 */
	public MessageService getMessageService()
	{
		return messageService;
	}

	/**
	 * @param messageService
	 *        The messageService to set.
	 */
	public void setMessageService(MessageService messageService)
	{
		this.messageService = messageService;
	}

	/**
	 * @return Returns the toolName.
	 */
	public String getToolName()
	{
		return toolName;
	}

	/**
	 * @param toolName
	 *        The toolName to set.
	 */
	public void setToolName(String toolName)
	{
		this.toolName = toolName;
	}

	/**
	 * @return Returns the removeEvents.
	 */
	public List getRemoveEvents()
	{
		return removeEvents;
	}

	/**
	 * @param removeEvents
	 *        The removeEvents to set.
	 */
	public void setRemoveEvents(List removeEvents)
	{
		this.removeEvents = removeEvents;
	}

	

	

	public String getSiteId(Reference ref)
	{
		return ref.getContext();
	}

	public String getSiteId(String resourceName)
	{
		
		return getSiteId(entityManager.newReference(resourceName));
	}

	public List getSiteContent(String context)
	{
		List all = new ArrayList();
		List l = messageService.getChannelIds(context);
		for (Iterator i = l.iterator(); i.hasNext();)
		{
			String chanellId = (String) i.next();
			try
			{
				
				MessageChannel c = messageService.getChannel(chanellId);

				List messages = c.getMessages(null, true);
				// WARNING: I think the implementation caches on thread, if this
				// is
				// a builder
				// thread this may not work
				for (Iterator mi = messages.iterator(); mi.hasNext();)
				{
					Message m = (Message) i.next();
					all.add(m.getReference());
				}
			}
			catch (Exception ex)
			{
				log.warn("Failed to get channel "+chanellId);

			}
		}
		return all;
	}

}
