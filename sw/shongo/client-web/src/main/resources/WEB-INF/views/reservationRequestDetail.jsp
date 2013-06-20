<%--
  -- Page for displaying details about a single reservation request.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}"/>
<security:accesscontrollist hasPermission="WRITE" domainObject="${reservationRequest}" var="writable"/>
<c:if test="${reservationRequest.type != 'CREATED'}">
    <c:set var="writable" value="false"/>
</c:if>

<script type="text/javascript">
    // Angular application
    angular.module('ngReservationRequestDetail', ['ngPagination']);
</script>

<div ng-app="ngReservationRequestDetail">

    <dl class="dl-horizontal">

        <dt><spring:message code="views.reservationRequest.identifier"/>:</dt>
        <dd>${reservationRequest.id} </dd>

        <dt><spring:message code="views.reservationRequest.dateTime"/>:</dt>
        <dd><joda:format value="${reservationRequest.dateTime}" style="MM"/></dd>

        <dt><spring:message code="views.reservationRequest.purpose"/>:</dt>
        <dd>
            <c:choose>
                <c:when test="${reservationRequest.purpose == 'SCIENCE'}">
                    <spring:message code="views.reservationRequest.purpose.science"/>
                </c:when>
                <c:when test="${reservationRequest.purpose == 'EDUCATION'}">
                    <spring:message code="views.reservationRequest.purpose.education"/>
                </c:when>
                <c:when test="${reservationRequest.purpose == 'MAINTENANCE'}">
                    <spring:message code="views.reservationRequest.purpose.maintenance"/>
                </c:when>
                <c:when test="${reservationRequest.purpose == 'OWNER'}">
                    <spring:message code="views.reservationRequest.purpose.owner"/>
                </c:when>
            </c:choose>
        </dd>

        <dt><spring:message code="views.reservationRequest.description"/>:</dt>
        <dd>${reservationRequest.description} </dd>

    </dl>

    <div ng-controller="PaginationController"
         ng-init="init('list_acls', '${contextPath}/reservation-request/:id/acl?start=:start&count=:count', {id: '${reservationRequest.id}'})">
        <pagination-page-size class="pull-right">
            <spring:message code="views.pagination.records"/>
        </pagination-page-size>
        <h2><spring:message code="views.reservationRequestDetail.userRoles"/></h2>
        <div class="spinner" ng-hide="ready"></div>
        <table class="table table-striped table-hover" ng-show="ready">
            <thead>
            <tr>
                <th><spring:message code="views.aclRecord.user"/></th>
                <th><spring:message code="views.aclRecord.role"/></th>
                <th><spring:message code="views.aclRecord.email"/></th>
                <c:if test="${writable}">
                    <th><spring:message code="views.list.action"/></th>
                </c:if>
            </tr>
            </thead>
            <tbody>
            <tr ng-repeat="userRole in items">
                <td>{{userRole.user.fullName}}</td>
                <td>{{userRole.role}}</td>
                <td>{{userRole.user.primaryEmail}}</td>
                <c:if test="${writable}">
                    <td>
                        <a href="${contextPath}/reservation-request/${reservationRequest.id}/acl/delete/{{userRole.id}}">
                            <spring:message code="views.list.action.delete"/>
                        </a>
                    </td>
                </c:if>
            </tr>
            <tr ng-hide="items.length">
                <td colspan="7" class="empty">- - - None - - -</td>
            </tr>
            </tbody>
        </table>
        <c:if test="${writable}">
            <a class="btn btn-primary" href="${contextPath}/reservation-request/${reservationRequest.id}/acl/create">
                <spring:message code="views.button.create"/>
            </a>
        </c:if>
        <div style="display: inline-block;"></div>
        <pagination-pages class="pull-right"><spring:message code="views.pagination.pages"/></pagination-pages>
    </div>

    <hr/>

    <h2 ><spring:message code="views.reservationRequestDetail.history"/></h2>
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th><spring:message code="views.reservationRequest.dateTime"/></th>
            <th><spring:message code="views.reservationRequest.type"/></th>
            <th><spring:message code="views.list.action"/></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${history}" var="historyItem">
            <c:set var="rowClass" value=""></c:set>
            <c:choose>
                <c:when test="${historyItem.id == reservationRequest.id}">
                    <c:set var="rowClass" value="selected"/>
                </c:when>
                <c:otherwise>
                    <c:set var="rowClass" value=""/>
                </c:otherwise>
            </c:choose>
            <tr class="${rowClass}">
                <td><joda:format value="${historyItem.dateTime}" style="MM"/></td>
                <td>${historyItem.type}</td>
                <td>
                    <c:if test="${historyItem.id != reservationRequest.id}">
                        <a href="${contextPath}/reservation-request/detail/${historyItem.id}">
                            <spring:message code="views.list.action.show"/>
                        </a>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>


</div>

<hr/>

<div class="pull-right">
    <a class="btn btn-primary" href="${contextPath}/reservation-request">
        <spring:message code="views.button.back"/>
    </a>
    <a class="btn" href="">
        <spring:message code="views.button.refresh"/>
    </a>
    <c:if test="${writable}">
        <a class="btn" href="${contextPath}/reservation-request/modify/${reservationRequest.id}">
            <spring:message code="views.button.modify"/>
        </a>
        <a class="btn" href="${contextPath}/reservation-request/delete/${reservationRequest.id}">
            <spring:message code="views.button.delete"/>
        </a>
    </c:if>
</div>