<%--
  -- Page for creation/modification of participant for a reservation request.
  --%>
<%@ page import="cz.cesnet.shongo.client.web.ClientWebUrl" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="tag" uri="/WEB-INF/client-web.tld" %>

<tag:url var="cancelUrl" value="<%= ClientWebUrl.RESERVATION_REQUEST_DETAIL %>">
    <tag:param name="reservationRequestId" value="${userRole.entityId}"/>
</tag:url>

<script type="text/javascript">
    angular.module('jsp:reservationRequestParticipant', ['tag:participantForm']);
</script>

<div ng-app="jsp:reservationRequestParticipant">

    <c:choose>
        <c:when test="${empty participant.id}">
            <c:set var="title" value="views.participant.add"/>
            <c:set var="confirmTitle" value="views.button.add"/>
        </c:when>
        <c:otherwise>
            <c:set var="title" value="views.participant.modify"/>
            <c:set var="confirmTitle" value="views.button.modify"/>
        </c:otherwise>
    </c:choose>

    <h1><spring:message code="${title}"/></h1>

    <tag:participantForm confirmTitle="${confirmTitle}"/>

</div>