/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.hibernate.wizard.hibernatecachewizard;import java.util.*;import org.eclipse.jface.viewers.*;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.ui.INewWizard;import org.eclipse.ui.IWorkbench;import org.jboss.tools.hibernate.core.*;import org.jboss.tools.hibernate.core.exception.ExceptionHandler;import org.jboss.tools.hibernate.core.hibernate.*;import org.jboss.tools.hibernate.view.ViewPlugin;import org.jboss.tools.hibernate.view.views.ReadOnlyWizard;import org.jboss.tools.hibernate.view.views.ViewsUtils;import org.jboss.tools.hibernate.wizard.hibernatecachewizard.datamodel.ICacheable;import org.jboss.tools.hibernate.wizard.hibernatecachewizard.ui.*;
public class HibernateCacheWizard extends ReadOnlyWizard implements INewWizard {	private CachedClassesPage page1 = null;	private CachedCollectionsPage page2 = null;	private CachedRegionsPage page3 = null;	private IMapping theMapping;	private Hashtable<String,IPersistentClassMapping> cacheableClasses = new Hashtable<String,IPersistentClassMapping>();	private Hashtable<String,IPersistentValueMapping> cacheableCollections = new Hashtable<String,IPersistentValueMapping>();	private Hashtable<String,Hashtable<String,Object>> cacheableRegions = new Hashtable<String,Hashtable<String,Object>>();	private Hashtable<String,IPersistentClassMapping> nativeCachedClasses = new Hashtable<String,IPersistentClassMapping>();	private Hashtable<String,IPersistentValueMapping> nativeCachedCollections = new Hashtable<String,IPersistentValueMapping>();		public boolean performCancel() 	{ 		try		{			theMapping.reload( true); // edit tau 17.11.2005		}		catch(final Exception exc){			getShell().getDisplay().asyncExec(new Runnable() {					public void run() { 						ExceptionHandler.handle(exc, ViewPlugin.getActiveWorkbenchShell(),null, exc.getMessage());					}				});		}		return true;	}	public HibernateCacheWizard(IMapping mapping, TreeViewer viewer)	{		super(mapping, viewer);		theMapping = mapping;		setWindowTitle(ICacheable.bundle.getString("HibernateCacheWizard.WIZARD_NAME"));		parseIncomingMapping();	}	/**	 * parse current mapping.	 */	private void parseIncomingMapping()	{		getCachedItems();	}
	/**	 * formed  cacheable persistent classes list	 *	 */	private void getCachedItems()	{		for(int i = 0; i < theMapping.getPersistentClassMappings().length; i++)		{			IPersistentClassMapping item = theMapping.getPersistentClassMappings()[i]; 			createCachedClassesList(item);			createCachedCollectionsList(item);				}	}
	private void createCachedClassesList(IPersistentClassMapping item)	{		if( isPersistentClassCacheable(item) )		{			String strategy = ((IRootClassMapping)item).getCacheConcurrencyStrategy();			if( strategy == null)			{// class not cached.				cacheableClasses.put(item.getName(),item);			}			else			{				String cachedname = item.getName() + ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN") + strategy; 				nativeCachedClasses.put(cachedname,item);				//add tau 31.03.2006				((IRootClassMapping)item).setDirtyCacheConcurrencyStrategy(false);			}		}	}
	/**	 * check for cacheable of a persistent class.	 * @param persistclass - class for checking	 * @return sign of cacheability the class	 */	private boolean isPersistentClassCacheable(IPersistentClassMapping persistclass)	{		return ( ((IHibernateClassMapping) persistclass ).isClass() ); // downcasting.	}		/**	 * check if the persistent class cached.	 * @param item - mapping of the class.	 * @return sign of cached of the class.	 */	private boolean isPersistentClassCached(IRootClassMapping item)	{		return ( (item.getCacheConcurrencyStrategy() != null) ? true : false );	}
	private void createCachedCollectionsList(IPersistentClassMapping item)	{		for(Iterator it = item.getFieldMappingIterator();it.hasNext();)		{			IPersistentFieldMapping prop = (IPersistentFieldMapping)it.next();			IPersistentValueMapping value = prop.getPersistentValueMapping();			if(isPersistentCollectionCacheable(value))			{				ICollectionMapping clmap = (ICollectionMapping)value;				String name = item.getName();				name += (ICacheable.bundle.getString("HibernateCacheWizard.CACHEABLE_NAME_TOKEN1") + prop.getName());				if( clmap.getCacheConcurrencyStrategy() == null)				{					cacheableCollections.put(name, value);				}				else				{// collection was cached.					String cachedname = name + 					ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN") + 					clmap.getCacheConcurrencyStrategy(); 					nativeCachedCollections.put(cachedname, value);					//add tau 31.03.2006					((ICollectionMapping)value).setDirtyCacheConcurrencyStrategy(false);									}			}		}	}
	private boolean isPersistentCollectionCacheable(IPersistentValueMapping val)	{		boolean sign = false;		if(val instanceof ICollectionMapping)		{			if( !((ICollectionMapping)val).isOneToMany() )			{// isOneToMany() == false				sign = true;			}		}		return sign;	}	private boolean isPersistentCollectionCached(IPersistentValueMapping mapping)	{		boolean cached = false;		if(mapping instanceof ICollectionMapping)		{			cached = ( ((ICollectionMapping)mapping).getCacheConcurrencyStrategy() != null ) ? true : false; 		}		return cached;	}
	public boolean performFinish()	{		try		{			//edit tau 31.03.2006			//theMapping.save();						markCachedClasses(cacheableClasses);						markCachedClasses(nativeCachedClasses);						markCachedCollections(cacheableCollections);						markCachedCollections(nativeCachedCollections);									theMapping.save();					}		catch(final Exception exc) {			getShell().getDisplay().asyncExec(new Runnable() {				public void run() {						ExceptionHandler.handle(exc, ViewPlugin.getActiveWorkbenchShell(),null, exc.getMessage());				}			});		}		return true;	}		//add tau 31.03.2006	private void markCachedClasses(Hashtable hashtable){		Collection collection = hashtable.values();		for (Iterator iter = collection.iterator(); iter.hasNext();) {			IRootClassMapping classMapping = (IRootClassMapping) iter.next();			if (classMapping.isDirtyCacheConcurrencyStrategy()){				classMapping.getStorage().setDirty(true);				classMapping.setDirtyCacheConcurrencyStrategy(false);			}		}			}		//add tau 31.03.2006	private void markCachedCollections(Hashtable hashtable){		Collection collection = hashtable.values();		for (Iterator iter = collection.iterator(); iter.hasNext();) {			ICollectionMapping classMapping = (ICollectionMapping) iter.next();			if (classMapping.isDirtyCacheConcurrencyStrategy()){				classMapping.getOwner().getStorage().setDirty(true);				classMapping.setDirtyCacheConcurrencyStrategy(false);			}		}			}	
	private void createCachedRegionsList() {		cacheableRegions.clear();		addCacheableRegion(ICacheable.CLASS);		addCacheableRegion(ICacheable.COLLECTION);	}
	private void addCacheableRegion(int sign)	{		Hashtable items = getItemsTable(sign);		Enumeration keys = items.keys();		while( keys.hasMoreElements()) {			String name = (String)keys.nextElement();			Object cacheableclass = items.get(name);			String regionname = ICacheable.bundle.getString("HibernateCacheWizard.EMPTY_STRING");						if( cacheableclass instanceof IPersistentClassMapping) {				if(!isPersistentClassCached((IRootClassMapping)cacheableclass))				{	continue;	}				regionname = ((IRootClassMapping)cacheableclass).getCacheRegionName();}			if(cacheableclass instanceof ICollectionMapping) {				if(!isPersistentCollectionCached((ICollectionMapping)cacheableclass))				{					continue;				}				regionname = ((ICollectionMapping)cacheableclass).getCacheRegionName();}						if(regionname == null) {				regionname = createDefaultRegionName(name,false);			}			if(!cacheableRegions.containsKey(regionname)) {				cacheableRegions.put(regionname,new Hashtable<String,Object>());			}						(cacheableRegions.get(regionname)).put(name,cacheableclass);		}	}	private Hashtable getItemsTable(int sign) {		Hashtable temp = null;		switch(sign)		{		case 1:			temp = nativeCachedClasses;			break;		case 2:			temp = nativeCachedCollections;			break;		default: break;		}		return temp;	}
	/**	 * Create default region name from list of cacheable classes or cacheable collections.	 * if <fullname> is class name - return <full class name>;	 * if <fullname> is collection name - return <full class name>.<property name>;  	 * @param fullname - full name of cacheable class or cacheable collection.	 * @param iscollectionname - sign of collection name.	 * @return default region name.	 */	private String createDefaultRegionName(String fullname, boolean iscollectionname)	{		return (iscollectionname) ? createRegionNameFromCollectionName(fullname): fullname;	}
	/**	 * Create default region name from collection name.	 * @param collname - full collection name.	 * @return default region name. 	 */	private String createRegionNameFromCollectionName(String collname)	{		String[] tokens = collname.split(ICacheable.bundle.getString("HibernateCacheWizard.CACHEABLE_NAME_TOKEN2"));		return tokens[0];	}	public void cacheClass(final String classname, String param, boolean changestrategy)	{		IRootClassMapping tocache = getClassMapping(classname, param, changestrategy);;		if(tocache != null)		{			tocache.setCacheConcurrencyStrategy(param);			changeCachedClasses(classname, param);		}	}		private IRootClassMapping getClassMapping(String classname, String param, boolean changestrategy)	{		IRootClassMapping tocache = null;		if(cacheableClasses.containsKey(classname))		{	tocache = (IRootClassMapping)cacheableClasses.get(classname);						}				Comparator<Object> cachedcomp = (changestrategy) ? cachedComparator2() : cachedComparator();				Object[] cachedclasses = nativeCachedClasses.keySet().toArray();		Arrays.sort(cachedclasses, cachedcomp);		int index = Arrays.binarySearch(cachedclasses,classname,cachedcomp);		if(index >= 0)		{					tocache = (IRootClassMapping)nativeCachedClasses.get((String)cachedclasses[index]);			if(param != null)			{				nativeCachedClasses.remove((String)cachedclasses[index]);				String newname = classname + 				ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN") + param;				nativeCachedClasses.put(newname, tocache);			}		}				return tocache;	}	/**	 *  for add / remove items.	 * @return	 */	private Comparator<Object> cachedComparator() {		Comparator<Object> cmp = new Comparator<Object>() {			public int compare(Object arg0, Object arg1) {				String cached_name 	= (String) arg0;				String desired		= (String) arg1;				return cached_name.compareTo(desired);			}		};		return cmp;	}	/**	 * change strategy for cached items.	 * @return	 */	private Comparator<Object> cachedComparator2() {		Comparator<Object> cmp = new Comparator<Object>() {			public int compare(Object arg0, Object arg1) {				String cached_name 	= (String) arg0;				String desired		= (String) arg1;				String[] temp = cached_name.split(ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN"));				return temp[0].compareTo(desired);			}		};		return cmp;	}	public void cacheCollections(final String clname, String param, boolean changestrategy)	{		ICollectionMapping clmapping = getCollectionMapping(clname,param, changestrategy);		if(clmapping != null)		{						clmapping.setCacheConcurrencyStrategy(param);;			changeCachedCollections(clname, param);		}	}	private void changeCachedClasses(String itemname, String param) {		IPersistentClassMapping temp = null;		if(param == null) { // remove from cache			if(nativeCachedClasses.containsKey(itemname)) {				temp = nativeCachedClasses.get(itemname);				nativeCachedClasses.remove(itemname);				String[] item = itemname.split(ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN"));				cacheableClasses.put(item[0], temp);			}		} else { // add to cache			if(cacheableClasses.containsKey(itemname)) {				temp = cacheableClasses.get(itemname);				cacheableClasses.remove(itemname);				itemname +=  					ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN") + param;  				nativeCachedClasses.put(itemname, temp);			}		}	}	private void changeCachedCollections(String itemname, String param) {		IPersistentValueMapping temp = null;		if(param == null) { // remove from cache			if(nativeCachedCollections.containsKey(itemname)) {				temp = nativeCachedCollections.get(itemname);				nativeCachedCollections.remove(itemname);				String[] item = itemname.split(ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN"));				cacheableCollections.put(item[0], temp);				cacheableCollections.put(itemname, temp);			}		} else { // add to cache			if(cacheableCollections.containsKey(itemname)) {				temp = cacheableCollections.get(itemname);				cacheableCollections.remove(itemname);				itemname +=  					ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN") + param;  				nativeCachedCollections.put(itemname, temp);			}		}	}	private ICollectionMapping getCollectionMapping(String clname, String param, boolean changestrategy) {		ICollectionMapping clmap = null;		if(cacheableCollections.containsKey(clname)) {// class exist in incoming tree.			clmap = (ICollectionMapping)cacheableCollections.get(clname); 		}		Comparator<Object> cachedcomp = (changestrategy) ? cachedComparator2() : cachedComparator();		Object[] cachedcollect = nativeCachedCollections.keySet().toArray();		Arrays.sort(cachedcollect, cachedcomp);		int index = Arrays.binarySearch(cachedcollect,clname,cachedcomp);		if(index >= 0)		{						//clmap = (ICollectionMapping)nativeCachedCollections.get((String)cachedcollect[index]);			clmap = (ICollectionMapping)nativeCachedCollections.get((String)cachedcollect[index]);			if(param != null)			{				//edit tau for /ESORM-560 java.lang.ClassCastException in Cache wizard				//nativeCachedClasses.remove((String)cachedcollect[index]);				nativeCachedCollections.remove((String)cachedcollect[index]);								String newname = clname + 				ICacheable.bundle.getString("HibernateCacheWizard.CACHED_STRATEGY_TOKEN") + param;								//edit tau for /ESORM-560 java.lang.ClassCastException in Cache wizard								//nativeCachedClasses.put(newname, clmap);				nativeCachedCollections.put(newname, clmap);						}		}		return clmap;	}		/**	 * set new region name for an item.	 * @param itemname item name.	 * @param regionname new name of region.	 */
	public void setRegionName(final String itemname, final String regionname)	{		try		{			if(nativeCachedClasses.containsKey(itemname))			{				((IRootClassMapping)nativeCachedClasses.get(itemname)).setCacheRegionName(regionname);			}			if(nativeCachedCollections.containsKey(itemname))			{				((ICollectionMapping)nativeCachedCollections.get(itemname)).setCacheRegionName(regionname);			}		}		catch(final Exception exc){			//edit tau 30.03.2006			//ExceptionHandler.logInfo(this.getClass().getName() + ". Error of setting regionname for " + itemname);			getShell().getDisplay().asyncExec(new Runnable() {				public void run() {						ExceptionHandler.handle(exc, ViewPlugin.getActiveWorkbenchShell(),null, exc.getMessage());				}			});					}	}	public void init(IWorkbench workbench, IStructuredSelection selection) 	{	}		private Hashtable<String,ArrayList<String>> createRegionsKeys() {		Hashtable<String,ArrayList<String>> htkeys = new Hashtable<String,ArrayList<String>>();		Enumeration enumkeys = cacheableRegions.keys();		while(enumkeys.hasMoreElements()) {			String name = (String)enumkeys.nextElement();			Enumeration tempkeys = ((Hashtable)cacheableRegions.get(name)).keys();			ArrayList<String> alist = new ArrayList<String>();			while(tempkeys.hasMoreElements()) {				alist.add((String)tempkeys.nextElement());			}			htkeys.put(name,alist);		}		return htkeys;	}
	public void addPages() 	{		page1 = new CachedClassesPage(ICacheable.bundle.getString("HibernateCacheWizard.CACHING_CLASSES"),cacheableClasses.keySet(), nativeCachedClasses.keySet());		page1.setTitle(ICacheable.bundle.getString("HibernateCacheWizard.CACHING_CLASSES"));		page1.setCallback(this);
		page2 = new CachedCollectionsPage(ICacheable.bundle.getString("HibernateCacheWizard.CACHING_COLLECTION"),cacheableCollections.keySet(), nativeCachedCollections.keySet());		page2.setTitle(ICacheable.bundle.getString("HibernateCacheWizard.CACHING_COLLECTION"));		page2.setCallback(this);		page3 = new CachedRegionsPage(ICacheable.bundle.getString("HibernateCacheWizard.CACHING_REGIONS"),createRegionsKeys());		page3.setTitle(ICacheable.bundle.getString("HibernateCacheWizard.CACHING_REGIONS"));		page3.setCallback(this);		addPage(page1);		addPage(page2);		addPage(page3);	}	public IWizardPage getNextPage(IWizardPage page) 	{		IWizardPage nextpage = super.getNextPage(page);		if(nextpage instanceof CachedCollectionsPage)			page1.getCachedViewer().getList().deselectAll();		if(nextpage instanceof CachedRegionsPage)		{			createCachedRegionsList();			page3.initialize(createRegionsKeys());			page3.stuffInitialViewer();			page3.stuffCachedTreeViewer();			page2.getCachedViewer().getList().deselectAll();		}		return nextpage;	}		// add tau 13.02.2006	// for ESORM-513 Overwrites and changes our Hibernate mapping files, even if they are marked read-only?		public boolean canFinish() {		boolean result = super.canFinish();				// use ViewsUtils.ReadOnlyMappimg(theMapping)		//if (this.readOnly) result = false;				if (ViewsUtils.isReadOnlyMappimg(theMapping)) {			result = false;					}				return result;	}	}