package com.dotcms.rest.api.v1.site;

import static com.dotmarketing.util.Logger.debug;
import static com.dotmarketing.util.Logger.error;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.PaginationUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.util.HostNameComparator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Provides all the utility methods used by the {@link SiteBrowserResource}
 * class to provide the required data to the UI layer or any other type of
 * client.
 *
 * @author jsanca
 */
public class SiteHelper implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final HostAPI hostAPI;

    public static final String EXT_HOSTADMIN = "sites";
    
    public static final String HAS_PREVIOUS = "hasPrevious";
    public static final String HAS_NEXT = "hasNext";
    public static final String TOTAL_SITES = "total";
    public static final String RESULTS = "results";
    
    @VisibleForTesting
    public SiteHelper (HostAPI hostAPI) {
        this.hostAPI = hostAPI;
    }

    /**
     * Private constructor for the singleton holder.
     */
    private SiteHelper () {
        this.hostAPI = APILocator.getHostAPI();
    }

    private static class SingletonHolder {
        private static final SiteHelper INSTANCE = new SiteHelper();
    }

    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static SiteHelper getInstance() {
        return SiteHelper.SingletonHolder.INSTANCE;
    } // getInstance.


    /**
     * Check if a Site is archived or not, keeping the exception quietly
     * @param showArchived {@link Boolean}
     * @param host {@link Host}
     * @return Boolean
     */
    public boolean checkArchived (final boolean showArchived, final Host host) {
        boolean checkArchived = false;
        try {

            checkArchived = (showArchived || !host.isArchived());
        } catch (Exception e) {
            error(SiteHelper.class, e.getMessage(), e);
        }

        return checkArchived;
    } // checkArchived.
    
    /**
     * Return a site by user and site id
     *
     * @param user User to filter the host to return
     * @param siteId Id to filter the host to return
     * @return host that the given user has permissions and with id equal to hostId, if any exists then return null
     * @throws DotSecurityException if one is thrown when the sites are search
     * @throws DotDataException if one is thrown when the sites are search
     */
    public Host getSite(User user, String siteId) throws DotSecurityException, DotDataException {
        Host site = this.hostAPI.find(siteId, user, Boolean.TRUE);
        
        return site;
    }

	/**
	 * Determines what site is to be marked as "selected" by a user. If the
	 * currently selected site in the HTTP Session is part of the sites that a
	 * user (actual or impersonated user) has access to, the Identifier of such
	 * a site is returned. If the site in the session is not in the list of
	 * sites, the Identifier of the first site in the list must be returned.
	 * 
	 * @param siteList
	 *            - The list of sites (their metadata) that a user has access
	 *            to.
	 * @param siteInSession
	 *            - The Identifier of the site that is marked as selected in the
	 *            current user session.
	 * @param user
	 *            - The current user session.            
	 * @return The Identifier of the site that will be marked as "selected".
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
    public String getSelectedSite(final List<Host> siteList, final String siteInSession, User user)  {
		String selectedSite = UtilMethods.isSet(siteInSession) ? siteInSession : StringUtils.EMPTY;
		boolean siteFound = false;
		if (siteList != null && !siteList.isEmpty()) {
			Host host = null;
			try {
				host = this.hostAPI.find(siteInSession, user, Boolean.FALSE);
			} catch (DotDataException | DotSecurityException e) {
				/** The user doesn't have permission to see this host **/
				debug(SiteHelper.class, "User doesn't have permission to see host ["+siteInSession+"}. error"+e.getMessage());
			}
			
			if (null != host && UtilMethods.isSet(host.getIdentifier())) {
				siteFound = true;
				if(!siteList.contains(host)){
					siteList.add(host);
				}
			}
						
			/**
			 * If the user doesn't have permission to see the host or 
			 * the host doesn't exist then get the first one available
			 * for the user
			 */
			if (!siteFound) {
				selectedSite = siteList.get(0).getIdentifier();
			}
		}
		return selectedSite;
	}
	
	/**
	 * Returns a subset of the list of sites that the given user has access to
	 * by query, page and limit 
	 * .
	 *
	 * @param showArchived
	 *            - Is set to {@code true}, archived sites will be returned.
	 *            Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param filter
	 *            - (Optional) If specified, returns the sites whose name starts
	 *            with the value of the {@code filter} variable.
	 *            
	 * @param currentPage
	 *           - indicate the page to obtain the subset of sites to be returned.
	 *           
	 * @param sitesPerPage
	 *           - indicates how many site should be included in the subset to be returned.
	 *            
	 * @param respectFrontendRoles
	 *            -
	 * @return The list of sites that the given user has permissions to access.
	 * @throws DotDataException
	 *             An error occurred when retrieving the sites' data.
	 * @throws DotSecurityException
	 *             A system error occurred.
	 */
    public Map<String,Object> getPaginatedOrderedSites(final boolean showArchived, final User user, final String filter, final int currentPage, final int perPage, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
    	final String sanitizedFilter = filter != null && !filter.equals("all") ? filter : StringUtils.EMPTY;
    	
    	Map<String, Object> results = new HashMap<String,Object>(); 
    	
    	int minIndex = PaginationUtil.getMinIndex(currentPage, perPage);
    	int maxIndex = PaginationUtil.getMaxIndex(currentPage, perPage);
    	
    	PaginatedArrayList<Host> hosts = this.hostAPI.search(sanitizedFilter, showArchived, Boolean.FALSE, perPage, minIndex, user, respectFrontendRoles);
    	   	
    	int totalCount = (int)hosts.getTotalResults();
        
        if((minIndex + perPage) >= totalCount){
        	maxIndex = totalCount;
        }
        
		results.put(TOTAL_SITES, totalCount);
    	results.put(RESULTS, hosts);
    	results.put(HAS_NEXT, maxIndex < totalCount);
    	results.put(HAS_PREVIOUS, minIndex > 0);
    	return results;
    	
    }

}
