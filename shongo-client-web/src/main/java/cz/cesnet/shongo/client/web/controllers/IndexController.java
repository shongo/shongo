package cz.cesnet.shongo.client.web.controllers;

import com.google.common.base.Strings;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.Design;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import cz.cesnet.shongo.client.web.support.interceptors.IgnoreDateTimeZone;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Index controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class IndexController
{
    @Resource
    private Design design;

    @javax.annotation.Resource
    protected ResourceService resourceService;

    @Resource
    private Cache cache;

    /**
     * Handle main (index) view.
     */
    @RequestMapping(value = ClientWebUrl.HOME, method = RequestMethod.GET)
    public ModelAndView handleIndexView(
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            SessionStatus sessionStatus)
    {
        // Redirect authentication requests until the "redirect_uri" is fixed
        if (request.getParameter("code") != null || request.getParameter("error") != null) {
            redirectAttributes.addAllAttributes(request.getParameterMap());
            return new ModelAndView("redirect:" + ClientWebUrl.LOGIN);
        }
        ModelAndView modelAndView = new ModelAndView((authentication != null ? "indexAuthenticated" : "indexAnonymous"));
        modelAndView.addObject("mainContent", design.renderTemplateMain(request));
        modelAndView.addObject("showOnlyMeetingRooms", ClientWebConfiguration.getInstance().showOnlyMeetingRooms());

        if (authentication != null) {
//            List<ResourceSummary> resourceSummaries = new LinkedList<ResourceSummary>();

            OpenIDConnectAuthenticationToken authenticationToken = (OpenIDConnectAuthenticationToken) authentication;

            // Get all readable resources with assigned parking-place tag even not allocatable (previous reservations)
            String meetingRoomTag = ClientWebConfiguration.getInstance().getMeetingRoomTagName();
            String parkTagName = ClientWebConfiguration.getInstance().getParkingPlaceTagName();
            String vehicleTagName = ClientWebConfiguration.getInstance().getVehicleTagName();
            List<Map<String, Object>> meetingRooms = listResourcesByTag(meetingRoomTag, authenticationToken);
            List<Map<String, Object>> ppItems = listResourcesByTag(parkTagName, authenticationToken);
            List<Map<String, Object>> vehicleItems = listResourcesByTag(vehicleTagName, authenticationToken);

            modelAndView.addObject("meetingRoomResources", meetingRooms);
            modelAndView.addObject("parkingPlaceResources", ppItems);
            modelAndView.addObject("vehicleResources", vehicleItems);
            List<Map<String, Object>> physicalResources = new LinkedList<Map<String, Object>>();
            if (meetingRooms != null) {
                physicalResources.addAll(meetingRooms);
            }
            if (ppItems != null) {
                physicalResources.addAll(ppItems);
            }
            if (vehicleItems != null) {
                physicalResources.addAll(vehicleItems);
            }
            modelAndView.addObject("physicalResources", physicalResources);
        }

        // Not functional without controller annotation @SessionAttributes
        sessionStatus.setComplete();

        return modelAndView;
    }

    private List<Map<String, Object>> listResources (ResourceListRequest resourceListRequest, OpenIDConnectAuthenticationToken authenticationToken) {
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ResourceSummary resourceSummary : resourceService.listResources(resourceListRequest)) {
            Set<ObjectPermission> permissions;
            permissions = cache.getObjectPermissions(authenticationToken.getSecurityToken(), resourceSummary.getId());

//                resourceSummaries.add(resourceSummary);
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", resourceSummary.getId());
            item.put("name", resourceSummary.getName());
            item.put("domainName", resourceSummary.getDomainName());
            item.put("calendarUriKey", resourceSummary.getCalendarUriKey());
            item.put("isCalendarPublic", resourceSummary.isCalendarPublic());
            item.put("isReservable", permissions.contains(ObjectPermission.RESERVE_RESOURCE));
            items.add(item);
        }
        return items;
    }

    /**
     * Handle help view.
     */
    @RequestMapping(value = ClientWebUrl.LOGGED, method = RequestMethod.GET)
    @IgnoreDateTimeZone
    @ResponseBody
    public String handleLogged(HttpServletResponse response)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OpenIDConnectAuthenticationToken) {
            return "YES";
        }
        else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "NO";
        }
    }

    /**
     * Handle help view.
     */
    @RequestMapping(value = ClientWebUrl.HELP, method = RequestMethod.GET)
    @IgnoreDateTimeZone
    public String handleHelpView()
    {
        return "help";
    }

    /**
     * Handle only layout view.
     */
    @RequestMapping(value = "/layout", method = RequestMethod.GET)
    @IgnoreDateTimeZone
    public String handleLayoutView()
    {
        return "development";
    }

    /**
     * Handle development view.
     */
    @RequestMapping(value = "/development", method = RequestMethod.GET)
    public String handleDevelopmentView()
    {
        return "development";
    }

    protected List<Map<String, Object>> listResourcesByTag(String tagName, OpenIDConnectAuthenticationToken authenticationToken) {
        if (Strings.isNullOrEmpty(tagName)) {
            return null;
        }
        ResourceListRequest resourceListRequest = new ResourceListRequest(authenticationToken.getSecurityToken());
        resourceListRequest.setTagName(tagName);
        resourceListRequest.setAllocatable(false);
        List<Map<String, Object>> resources = listResources(resourceListRequest, authenticationToken);
        for (Map<String, Object> resource: resources) {
            resource.put("tag", tagName);
        }
        return resources;

    }
}
