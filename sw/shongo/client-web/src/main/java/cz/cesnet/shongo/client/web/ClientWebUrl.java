package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.report.ApiFaultException;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Definition of URL constants in Shongo web client.
 */
public class ClientWebUrl
{
    public static final String HOME =
            "/";
    public static final String LOGIN =
            "/login";
    public static final String LOGOUT =
            "/logout";
    public static final String CHANGELOG =
            "/changelog";

    public static final String REPORT =
            "/report";

    public static final String WIZARD =
            "/wizard";
    public static final String WIZARD_CREATE_ROOM =
            "/wizard/create";
    public static final String WIZARD_CREATE_ADHOC_ROOM =
            "/wizard/create/adhoc-room";
    public static final String WIZARD_CREATE_PERMANENT_ROOM =
            "/wizard/create/permanent-room";
    public static final String WIZARD_CREATE_ROOM_ATTRIBUTES =
            "/wizard/create/attributes";
    public static final String WIZARD_CREATE_ROOM_ROLES =
            "/wizard/create/roles";
    public static final String WIZARD_CREATE_ROOM_PARTICIPANTS =
            "/wizard/create/participants";
    public static final String WIZARD_CREATE_ROOM_PARTICIPANT_CREATE =
            "/wizard/create/participant/create";
    public static final String WIZARD_CREATE_ROOM_PARTICIPANT_MODIFY =
            "/wizard/create/participant/{participantId}/modify";
    public static final String WIZARD_CREATE_ROOM_PARTICIPANT_DELETE =
            "/wizard/create/participant/{participantId}/delete";
    public static final String WIZARD_CREATE_ROOM_ROLE_CREATE =
            "/wizard/create/role/create";
    public static final String WIZARD_CREATE_ROOM_ROLE_DELETE =
            "/wizard/create/role/{roleId}/delete";
    public static final String WIZARD_CREATE_ROOM_CONFIRM =
            "/wizard/create/confirm";
    public static final String WIZARD_CREATE_ROOM_CONFIRMED =
            "/wizard/create/confirmed";

    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY =
            "/wizard/create/permanent-room-capacity";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANTS =
            "/wizard/create/permanent-room-capacity/participants";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE =
            "/wizard/create/permanent-room-capacity/participant/create";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANT_MODIFY =
            "/wizard/create/permanent-room-capacity/participant/{participantId}/modify";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANT_DELETE =
            "/wizard/create/permanent-room-capacity/participant/{participantId}/delete";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM =
            "/wizard/create/permanent-room-capacity/confirm";
    public static final String WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRMED =
            "/wizard/create/permanent-room-capacity/confirmed";

    public static final String RESERVATION_REQUEST =
            "/reservation-request";
    public static final String RESERVATION_REQUEST_LIST =
            "/reservation-request/list";
    public static final String RESERVATION_REQUEST_LIST_DATA =
            "/reservation-request/list/data";
    public static final String RESERVATION_REQUEST_CREATE =
            "/reservation-request/create/{specificationType}";
    public static final String RESERVATION_REQUEST_DETAIL =
            "/reservation-request/{reservationRequestId:.+}/detail";
    public static final String RESERVATION_REQUEST_DETAIL_STATE =
            "/reservation-request/{reservationRequestId:.+}/detail/state";
    public static final String RESERVATION_REQUEST_DETAIL_CHILDREN =
            "/reservation-request/{reservationRequestId:.+}/detail/children";
    public static final String RESERVATION_REQUEST_DETAIL_USAGES =
            "/reservation-request/{reservationRequestId:.+}/detail/usages";
    public static final String RESERVATION_REQUEST_DETAIL_REVERT =
            "/reservation-request/{reservationRequestId:.+}/detail/revert";
    public static final String RESERVATION_REQUEST_MODIFY =
            "/reservation-request/{reservationRequestId:.+}/modify";
    public static final String RESERVATION_REQUEST_CREATE_DUPLICATE =
            "/reservation-request/{reservationRequestId:.+}/duplicate";
    public static final String RESERVATION_REQUEST_DELETE =
            "/reservation-request/{reservationRequestId:.+}/delete";
    public static final String RESERVATION_REQUEST_ROLES =
            "/reservation-request/{reservationRequestId:.+}/roles";
    public static final String RESERVATION_REQUEST_ROLE_CREATE =
            "/reservation-request/{reservationRequestId:.+}/role/create";
    public static final String RESERVATION_REQUEST_ROLE_DELETE =
            "/reservation-request/{reservationRequestId:.+}/role/{roleId}/delete";
    public static final String RESERVATION_REQUEST_PARTICIPANT_CREATE =
            "/reservation-request/{reservationRequestId:.+}/participant/create";
    public static final String RESERVATION_REQUEST_PARTICIPANT_MODIFY =
            "/reservation-request/{reservationRequestId:.+}/participant/{participantId}/modify";
    public static final String RESERVATION_REQUEST_PARTICIPANT_DELETE =
            "/reservation-request/{reservationRequestId:.+}/participant/{participantId}/delete";

    public static final String USER_SETTINGS =
            "/user/settings";
    public static final String USER_SETTINGS_ATTRIBUTE =
            USER_SETTINGS + "/{name}/{value}";
    public static final String USER_LIST_DATA =
            "/user/list/data";
    public static final String USER_DATA =
            "/user/{userId:.+}/data";

    public static final String ROOM_LIST =
            "/room/list";
    public static final String ROOM_LIST_DATA =
            "/room/list/data";
    public static final String ROOM_MANAGEMENT =
            "/room/{roomId:.+}";
    public static final String ROOM_ENTER =
            "/room/{roomId:.+}/enter";

    public static String format(String url, Object... variables)
    {
        for (Object variable : variables) {
            url = url.replaceFirst("\\{[^/]+\\}", variable != null ? variable.toString() : "");
        }
        return url;
    }

    public static String encodeUrlParam(String url)
    {
        try {
            return UriUtils.encodeQueryParam(url, "UTF-8");
        }
        catch (UnsupportedEncodingException exception) {
            throw new RuntimeException("Cannot encode URL to UTF-8.", exception);
        }
    }
}
