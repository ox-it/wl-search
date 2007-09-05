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

package org.sakaiproject.search.transaction.api;

/**
 * Generates a transaction sequence. The implementation of this interface must operate in thread safe fashon over the
 * entire cluster returning the next id in the sequence at all times.
 * @author ieb
 *
 */
public interface TransactionSequence
{

	/**
	 * This returns a safe clusterwide id
	 * @return
	 */
	long getNextId();

	/**
	 * This retuns the next id for the local JVM
	 * @return
	 */
	long getLocalId();

}