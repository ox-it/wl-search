/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.search.component.adapter.site;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.search.api.EntityContentProducer;
import org.sakaiproject.search.api.SearchIndexBuilder;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.search.api.SearchUtils;
import org.sakaiproject.search.model.SearchBuilderItem;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 * @author ieb
 * @author Colin Hebert
 */
public class SiteContentProducer implements EntityContentProducer {
	private static final Log logger = LogFactory.getLog(SiteContentProducer.class);
	private EntityManager entityManager;
	private Collection<String> addEvents;
	private Collection<String> removeEvents;
	private SiteService siteService;
	private ServerConfigurationService serverConfigurationService;
	private SearchService searchService;
	private SearchIndexBuilder searchIndexBuilder;

	public void init() {
		addEvents = Arrays.asList(
				SiteService.SECURE_ADD_COURSE_SITE,
				SiteService.SECURE_ADD_SITE,
				SiteService.SECURE_ADD_USER_SITE,
				SiteService.SECURE_UPDATE_GROUP_MEMBERSHIP,
				SiteService.SECURE_UPDATE_SITE,
				SiteService.SECURE_UPDATE_SITE_MEMBERSHIP);
		removeEvents = Collections.singleton(SiteService.SECURE_REMOVE_SITE);

		if (serverConfigurationService.getBoolean("search.enable", false)) {
			for (String addEvent : addEvents) {
				searchService.registerFunction(addEvent);
			}
			for (String removeEvent : removeEvents) {
				searchService.registerFunction(removeEvent);
			}
			//TODO: Replace this with a registration on a factory
			searchIndexBuilder.registerEntityContentProducer(this);
		}
	}

	@Override
	public boolean canRead(String reference) {
		Reference ref = entityManager.newReference(reference);
		EntityProducer ep = ref.getEntityProducer();
		if (ep instanceof SiteService) {
			try {
				((SiteService) ep).getSite(ref.getId());
				return true;
			} catch (Exception ex) {
				if (logger.isDebugEnabled())
					logger.debug("Unexpected exception", ex);
			}
		}
		return false;
	}

	@Override
	public Integer getAction(Event event) {
		String evt = event.getEvent();
		if (evt == null) return SearchBuilderItem.ACTION_UNKNOWN;
		for (String match : addEvents) {
			if (evt.equals(match)) {
				return SearchBuilderItem.ACTION_ADD;
			}
		}
		for (String match : removeEvents) {
			if (evt.equals(match)) {
				return SearchBuilderItem.ACTION_DELETE;
			}
		}
		return SearchBuilderItem.ACTION_UNKNOWN;
	}

	@Override
	public String getContainer(String ref) {
		// the site document is contained by itself
		return entityManager.newReference(ref).getId();
	}

	@Override
	public String getContent(String reference) {
		Reference ref = entityManager.newReference(reference);
		EntityProducer ep = ref.getEntityProducer();
		if (ep instanceof SiteService) {
			try {
				Site site = ((SiteService) ep).getSite(ref.getId());
				return site.getTitle() + " " + site.getShortDescription() + " " + site.getDescription();

			} catch (IdUnusedException e) {
				throw new RuntimeException(" Failed to get message content ", e); //$NON-NLS-1$
			}
		}

		throw new RuntimeException(" Not a Message Entity " + reference); //$NON-NLS-1$

	}

	@Override
	public Reader getContentReader(String reference) {
		return new StringReader(getContent(reference));
	}

	@Override
	public Map<String, Collection<String>> getCustomProperties(String ref) {
		Map<String, Collection<String>> props = new HashMap<String, Collection<String>>();
		ResourceProperties rp = entityManager.newReference(ref).getEntity().getProperties();
		for (Iterator<String> i = rp.getPropertyNames(); i.hasNext(); ) {
			String key = i.next();
			props.put(key, rp.getPropertyList(key));
		}
		return props;
	}

	@Override
	public String getCustomRDF(String ref) {
		return null;
	}

	@Override
	public String getId(String ref) {
		return entityManager.newReference(ref).getId();
	}

	public List getSiteContent(String context)
	{
		try {
			return Collections.singletonList(siteService.getSite(context).getReference());
		} catch (IdUnusedException idu) {
			if (logger.isDebugEnabled())
				logger.debug("Site Not Found for context " + context, idu);
			return Collections.<String>emptyList();
		}
	}

	@Override
	public Iterator getSiteContentIterator(String context)
	{
		return getSiteContent(context).iterator();
	}

	@Override
	public String getSiteId(String ref) {
		//An indexed site belongs to itself (so you can search for it if you're supposed to have an access to that site
		return entityManager.newReference(ref).getId();
	}

	@Override
	public String getSubType(String ref) {
		return entityManager.newReference(ref).getSubType();
	}

	@Override
	public String getTitle(String ref) {
		Site s = (Site) entityManager.newReference(ref).getEntity();
		return SearchUtils.appendCleanString(s.getTitle(),null).toString();
	}

	@Override
	public String getTool() {
		return "site";
	}

	@Override
	public String getType(String ref) {
		return entityManager.newReference(ref).getType();
	}

	@Override
	public String getUrl(String ref) {
		return entityManager.newReference(ref).getUrl();
	}

	@Override
	public boolean isContentFromReader(String reference) {
		return false;
	}

	@Override
	public boolean isForIndex(String ref) {
		Site s = (Site) entityManager.newReference(ref).getEntity();
		//SAK-18545 its possible the site no longer exits
		return s != null && s.isPublished();
		}

	@Override
	public boolean matches(String ref) {
		EntityProducer ecp = entityManager.newReference(ref).getEntityProducer();
		return ecp instanceof SiteService;
	}

	@Override
	public boolean matches(Event event) {
		return addEvents.contains(event.getEvent()) || removeEvents.contains(event.getEvent());
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setSearchIndexBuilder(SearchIndexBuilder searchIndexBuilder) {
		this.searchIndexBuilder = searchIndexBuilder;
	}
}
