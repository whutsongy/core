package com.dotmarketing.business;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.content.elasticsearch.business.IndiciesCache;
import com.dotcms.content.elasticsearch.business.IndiciesCacheImpl;
import com.dotcms.csspreproc.CSSCache;
import com.dotcms.csspreproc.CSSCacheImpl;
import com.dotcms.notifications.business.NewNotificationCache;
import com.dotcms.notifications.business.NewNotificationCacheImpl;
import com.dotcms.publisher.assets.business.PushedAssetsCache;
import com.dotcms.publisher.assets.business.PushedAssetsCacheImpl;
import com.dotcms.publisher.endpoint.business.PublishingEndPointCache;
import com.dotcms.publisher.endpoint.business.PublishingEndPointCacheImpl;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.jgroups.JGroupsCacheTransport;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.cache.FolderCacheImpl;
import com.dotmarketing.cache.ContentTypeCacheImpl;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.FlushCacheRunnable;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.logConsole.model.LogMapperCache;
import com.dotmarketing.logConsole.model.LogMapperCacheImpl;
import com.dotmarketing.plugin.business.PluginCache;
import com.dotmarketing.plugin.business.PluginCacheImpl;
import com.dotmarketing.portlets.categories.business.CategoryCache;
import com.dotmarketing.portlets.categories.business.CategoryCacheImpl;
import com.dotmarketing.portlets.containers.business.ContainerCache;
import com.dotmarketing.portlets.containers.business.ContainerCacheImpl;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.business.ContentletCacheImpl;
import com.dotmarketing.portlets.contentlet.business.HostCache;
import com.dotmarketing.portlets.contentlet.business.HostCacheImpl;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariablesCache;
import com.dotmarketing.portlets.hostvariable.bussiness.HostVariablesCacheImpl;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCache;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageCacheImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCache;
import com.dotmarketing.portlets.languagesmanager.business.LanguageCacheImpl;
import com.dotmarketing.portlets.links.business.MenuLinkCache;
import com.dotmarketing.portlets.links.business.MenuLinkCacheImpl;
import com.dotmarketing.portlets.rules.business.RulesCache;
import com.dotmarketing.portlets.rules.business.RulesCacheImpl;
import com.dotmarketing.portlets.rules.business.SiteVisitCache;
import com.dotmarketing.portlets.rules.business.SiteVisitCacheImpl;
import com.dotmarketing.portlets.structure.factories.RelationshipCache;
import com.dotmarketing.portlets.structure.factories.RelationshipCacheImpl;
import com.dotmarketing.portlets.templates.business.TemplateCache;
import com.dotmarketing.portlets.templates.business.TemplateCacheImpl;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkCache;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkCacheImpl;
import com.dotmarketing.portlets.workflows.business.WorkflowCache;
import com.dotmarketing.portlets.workflows.business.WorkflowCacheImpl;
import com.dotmarketing.tag.business.TagCache;
import com.dotmarketing.tag.business.TagCacheImpl;
import com.dotmarketing.tag.business.TagInodeCache;
import com.dotmarketing.tag.business.TagInodeCacheImpl;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.velocity.DotResourceCache;
import com.dotmarketing.viewtools.navigation.NavToolCache;
import com.dotmarketing.viewtools.navigation.NavToolCacheImpl;


/**
 * FactoryLocator is a factory method to get single(ton) service objects.
 * This is a kind of implementation, and there may be others.
 * @author Carlos Rivas (crivas)
 * @author Jason Tesser
 * @version 1.6
 * @since 1.6
 */

public class CacheLocator extends Locator<CacheIndex>{

    private static class CommitListenerCacheWrapper implements DotCacheAdministrator {

        DotCacheAdministrator dotcache;
        public CommitListenerCacheWrapper(DotCacheAdministrator dotcache) { this.dotcache=dotcache; }

		public void initProviders () {dotcache.initProviders();}
		public Set<String> getGroups () {return dotcache.getGroups();}
		public void flushAll() { dotcache.flushAll(); }
        public void flushGroup(String group) { dotcache.flushGroup(group); }
        public void flushAlLocalOnly() { dotcache.flushAlLocalOnly(); }
        public void flushGroupLocalOnly(String group) { dotcache.flushGroupLocalOnly(group); }
        public Object get(String key, String group) throws DotCacheException { return dotcache.get(key, group); }
        public void remove(String key, String group) { dotcache.remove(key,group); }
        public void removeLocalOnly(String key, String group) { dotcache.removeLocalOnly(key, group); }
        public void shutdown() { dotcache.shutdown(); }
        public List<Map<String, Object>> getCacheStatsList() { return dotcache.getCacheStatsList(); }
		public CacheTransport getTransport () {return dotcache.getTransport();}
		public void setTransport ( CacheTransport transport ) {dotcache.setTransport(transport);}
        public Class<?> getImplementationClass() { return dotcache.getClass(); }
        public void put(final String key, final Object content, final String group) {
            dotcache.put(key, content, group);
            try {
                if(DbConnectionFactory.inTransaction()) {
                    HibernateUtil.addRollbackListener(new FlushCacheRunnable() {
                       public void run() {
                           dotcache.remove(key, group);
                       }
                    });
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        public DotCacheAdministrator getImplementationObject() {
            return dotcache;
        }
	}

	private static CacheLocator instance;
	private static DotCacheAdministrator adminCache;

	private CacheLocator() {
		super();
	}

	public synchronized static void init(){
		long start = System.currentTimeMillis();
		if(instance != null)
			return;

		String clazz = Config.getStringProperty("cache.locator.class", ChainableCacheAdministratorImpl.class.getCanonicalName());
		Logger.info(CacheLocator.class, "loading cache administrator: "+clazz);
		try{
			adminCache = new CommitListenerCacheWrapper((DotCacheAdministrator) Class.forName(clazz).newInstance());
			adminCache.setTransport(new JGroupsCacheTransport());
		}
		catch(Exception e){
			Logger.fatal(CacheLocator.class, "Unable to load Cache Admin:" + clazz);
		}

		instance = new CacheLocator();

		/*
		Initializing the Cache Providers:

		 It needs to be initialized in a different call as the providers depend on the
		 license level, and the license level needs an already created instance of the CacheLocator
		 to work.
		 */
		adminCache.initProviders();
		System.setProperty(WebKeys.DOTCMS_STARTUP_TIME_CACHE, String.valueOf(System.currentTimeMillis() - start));
	}

	public static PermissionCache getPermissionCache() {
		return (PermissionCache)getInstance(CacheIndex.Permission);
	}
    public static RoleCache getRoleCache() {
        return (RoleCache)getInstance(CacheIndex.Role);
    }

    public static com.dotmarketing.business.RoleCache getCmsRoleCache() {
        return (com.dotmarketing.business.RoleCache)getInstance(CacheIndex.CMSRole);
    }

	public static CategoryCache getCategoryCache() {
		return (CategoryCache)getInstance(CacheIndex.Category);
	}

	public static TagCache getTagCache() {
		return (TagCache)getInstance(CacheIndex.Tag);
	}

	public static TagInodeCache getTagInodeCache() {
		return (TagInodeCache)getInstance(CacheIndex.TagInode);
	}

	public static ContentletCache getContentletCache() {
		return (ContentletCache)getInstance(CacheIndex.Contentlet);
	}

	public static DotResourceCache getVeloctyResourceCache(){
		return (DotResourceCache)getInstance(CacheIndex.Velocity);
	}


    public static LogMapperCache getLogMapperCache () {
        return ( LogMapperCache ) getInstance( CacheIndex.LogMapper );
    }

	public static RelationshipCache getRelationshipCache() {
		return (RelationshipCache)getInstance(CacheIndex.Relationship);
	}

	public static PluginCache getPluginCache() {
		return (PluginCache)getInstance(CacheIndex.Plugin);
	}

	public static LanguageCache getLanguageCache() {
		return (LanguageCache)getInstance(CacheIndex.Language);
	}

	public static UserCache getUserCache() {
		return (UserCache)getInstance(CacheIndex.User);
	}

	public static UserProxyCache getUserProxyCache() {
		return (UserProxyCache)getInstance(CacheIndex.Userproxy);
	}

	public static LayoutCache getLayoutCache() {
		return (LayoutCache)getInstance(CacheIndex.Layout);
	}

	public static IdentifierCache getIdentifierCache() {
		return (IdentifierCache)getInstance(CacheIndex.Identifier);
	}
	public static HTMLPageCache getHTMLPageCache() {
		return (HTMLPageCache)getInstance(CacheIndex.HTMLPage);
	}

	public static MenuLinkCache getMenuLinkCache() {
		return (MenuLinkCache)getInstance(CacheIndex.Menulink);
	}

	public static ContainerCache getContainerCache() {
		return (ContainerCache)getInstance(CacheIndex.Container);
	}

	public static TemplateCache getTemplateCache() {
		return (TemplateCache)getInstance(CacheIndex.Template);
	}
	public static HostCache getHostCache() {
		return (HostCache)getInstance(CacheIndex.Host);
	}
	public static BlockDirectiveCache getBlockDirectiveCache() {
		return (BlockDirectiveCache)getInstance(CacheIndex.Block_Directive);
	}
	public static BlockPageCache getBlockPageCache() {
		return (BlockPageCache) getInstance(CacheIndex.Block_Page);
	}
	public static VersionableCache getVersionableCache() {
		return (VersionableCache)getInstance(CacheIndex.Versionable);
	}
	public static FolderCache getFolderCache() {
		return (FolderCache)getInstance(CacheIndex.FolderCache);
	}
	public static WorkflowCache getWorkFlowCache() {
		return (WorkflowCache) getInstance(CacheIndex.WorkflowCache);
	}

	public static VirtualLinkCache getVirtualLinkCache() {
		return (VirtualLinkCache) getInstance(CacheIndex.VirtualLinkCache);
	}

	public static HostVariablesCache getHostVariablesCache() {
		return (HostVariablesCache)getInstance(CacheIndex.HostVariables);
	}

	public static IndiciesCache getIndiciesCache() {
	    return (IndiciesCache)getInstance(CacheIndex.Indicies);
	}

	public static NavToolCache getNavToolCache() {
	    return (NavToolCache)getInstance(CacheIndex.NavTool);
	}

	public static PublishingEndPointCache getPublishingEndPointCache() {
		return (PublishingEndPointCache)getInstance(CacheIndex.PublishingEndPoint);
	}

	public static PushedAssetsCache getPushedAssetsCache() {
		return (PushedAssetsCache)getInstance(CacheIndex.PushedAssets);
	}

	public static CSSCache getCSSCache() {
	    return (CSSCache)getInstance(CacheIndex.CSSCache);
	}

	public static NewNotificationCache getNewNotificationCache() {
		return (NewNotificationCache)getInstance(CacheIndex.NewNotification);
	}

	public static RulesCache getRulesCache() {
		return (RulesCache) getInstance(CacheIndex.RulesCache);
	}
	
	public static SiteVisitCache getSiteVisitCache() {
		return (SiteVisitCache) getInstance(CacheIndex.SiteVisitCache);
	}
    public static ContentTypeCache getContentTypeCache() {
        return (ContentTypeCache) getInstance(CacheIndex.ContentTypeCache);
    }

	/**
	 * The legacy cache administrator will invalidate cache entries within a cluster
	 * on a put where the non legacy one will not.
	 * @return
	 */
	public static DotCacheAdministrator getCacheAdministrator(){
		return adminCache;
	}

	private static Object getInstance(CacheIndex index) {

		if(instance == null){
			init();
			if(instance == null){
				Logger.fatal(CacheLocator.class, "CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
				throw new DotRuntimeException("CACHE IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
			}
		}

		Object serviceRef = instance.getServiceInstance(index);

		Logger.debug(CacheLocator.class, instance.audit(index));

		return serviceRef;

	 }

	@Override
	protected Object createService(CacheIndex enumObj) {
		return enumObj.create();
	}

	@Override
	protected Locator<CacheIndex> getLocatorInstance() {
		return instance;
	}

	public static CacheIndex[] getCacheIndexes(){
		return CacheIndex.values();
	}

	public static Cachable getCache (String value) {
		return (Cachable)getInstance(CacheIndex.getCacheIndex(value));
	}

}



enum CacheIndex
{
	Permission("Permission"),
	CMSRole("CMS Role"),
	Role("Role"),
	Category("Category"),
	Tag("Tag"),
	TagInode("TagInode"),
	Contentlet("Contentlet"),
	Chain("Chain"),
	LogMapper("LogMapper"),
	Relationship("Relationship"),
	Plugin("Plugin"),
	Language("Language"),
	User("User"),
	Velocity("Velocity"),
	Layout("Layout"),
	Userproxy("User Proxy"),
	Host("Host"),
	File("File"),
	HTMLPage("Page"),
	Menulink("Menu Link"),
	Container("Container"),
	Template("Template"),
	Identifier("Identifier"),
	Versionable("Versionable"),
	FolderCache("FolderCache"),
	WorkflowCache("Workflow Cache"),
	VirtualLinkCache("Virtual Link Cache"),
	HostVariables("Host Variables"),
	Block_Directive("Block Directive"),
	Block_Page("Block Page"),
	Indicies("Indicies"),
	NavTool("Navigation Tool"),
	PublishingEndPoint("PublishingEndPoint Cache"),
	PushedAssets("PushedAssets Cache"),
	CSSCache("Processed CSS Cache"),
	RulesCache("Rules Cache"),
	SiteVisitCache("Rules Engine - Site Visits"),
	NewNotification("NewNotification Cache"),
	ContentTypeCache("Content Type Cache");

	Cachable create() {
		switch(this) {
		case Permission: return new PermissionCacheImpl();
      	case Category: return new CategoryCacheImpl();
      	case Tag: return new TagCacheImpl();
      	case TagInode: return new TagInodeCacheImpl();
      	case Role: return new RoleCacheImpl();
      	case Contentlet: return new ContentletCacheImpl();
      	case Velocity : return new DotResourceCache();
      	case Relationship: return new RelationshipCacheImpl();
        case LogMapper: return new LogMapperCacheImpl();
      	case Plugin : return new PluginCacheImpl();
      	case Language : return new LanguageCacheImpl();
      	case User : return new UserCacheImpl();
      	case Userproxy : return new UserProxyCacheImpl();
      	case Layout : return new LayoutCacheImpl();
      	case CMSRole : return new com.dotmarketing.business.RoleCacheImpl();
      	case HTMLPage : return new HTMLPageCacheImpl();
      	case Menulink : return new MenuLinkCacheImpl();
      	case Container : return new ContainerCacheImpl();
      	case Template : return new TemplateCacheImpl();
      	case Host : return new HostCacheImpl();
      	case Identifier : return new IdentifierCacheImpl();
      	case HostVariables : return new HostVariablesCacheImpl();
      	case Block_Directive : return new BlockDirectiveCacheImpl();
      	case Block_Page : return new BlockPageCacheImpl();
      	case Versionable : return new VersionableCacheImpl();
      	case FolderCache : return new FolderCacheImpl();
      	case WorkflowCache : return new WorkflowCacheImpl();
      	case VirtualLinkCache : return new VirtualLinkCacheImpl();
      	case Indicies: return new IndiciesCacheImpl();
      	case NavTool: return new NavToolCacheImpl();
      	case PublishingEndPoint: return new PublishingEndPointCacheImpl();
      	case PushedAssets: return new PushedAssetsCacheImpl();
      	case CSSCache: return new CSSCacheImpl();
      	case NewNotification: return new NewNotificationCacheImpl();
      	case RulesCache : return new RulesCacheImpl();
      	case SiteVisitCache : return new SiteVisitCacheImpl();
      	case ContentTypeCache: return new ContentTypeCacheImpl();
		}
		throw new AssertionError("Unknown Cache index: " + this);
	}

	private String value;

	CacheIndex (String value) {
		this.value = value;
	}

	public String toString () {
		return value;
	}

	public static CacheIndex getCacheIndex (String value) {
		CacheIndex[] types = CacheIndex.values();
		for (CacheIndex type : types) {
			if (type.value.equals(value))
				return type;
		}
		return null;
	}
}

