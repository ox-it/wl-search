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

package org.sakaiproject.search.optimize.api;

import org.sakaiproject.search.transaction.api.IndexTransactionException;

/**
 * The optimization may fail
 * @author ieb
 *
 */
public class OptimizedFailedIndexTransactionException extends IndexTransactionException
{

	/**
	 * 
	 */
	public OptimizedFailedIndexTransactionException()
	{
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public OptimizedFailedIndexTransactionException(String arg0)
	{
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public OptimizedFailedIndexTransactionException(Throwable arg0)
	{
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public OptimizedFailedIndexTransactionException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

}