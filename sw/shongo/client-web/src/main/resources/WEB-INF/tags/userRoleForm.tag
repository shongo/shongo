<%--
  -- Reservation request form.
  --%>
<%@ tag body-content="empty" trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="app" uri="/WEB-INF/client-web.tld" %>

<%@attribute name="entityType" required="true" type="cz.cesnet.shongo.controller.EntityType" %>
<%@attribute name="confirmUrl" required="false" type="java.lang.String" %>
<%@attribute name="confirmTitle" required="false" type="java.lang.String" %>
<%@attribute name="cancelUrl" required="false" type="java.lang.String" %>
<%@attribute name="cancelTitle" required="false" type="java.lang.String" %>

<c:set var="tabIndex" value="1"/>

<script type="text/javascript">
    window.formatUser = function(user) {
        var text = user.firstName;
        if ( user.lastName != null ) {
            text += " " + user.lastName;
        }
        if ( user.originalId != null ) {
            text += " (" + user.originalId + ")";
        }
        return text;
    };
    $(function () {
        $("#userId").select2({
            placeholder: "<spring:message code="views.select.user"/>",
            width: 'resolve',
            minimumInputLength: 2,
            ajax: {
                url: "/user",
                dataType: 'json',
                data: function (term, page) {
                    return {
                        filter: term
                    };
                },
                results: function (data, page) {
                    var results = [];
                    for (var index = 0; index < data.length; index++) {
                        var dataItem = data[index];
                        results.push({id: dataItem.userId, text: window.formatUser(dataItem)});
                    }
                    return {results: results};
                }
            },
            initSelection: function (element, callback) {
                var id = $(element).val();
                callback({id: 0, text: '<spring:message code="views.select.loading"/>'});
                $.ajax("/user/" + id, {
                    dataType: "json"
                }).done(function (data) {
                            callback({id: id, text: window.formatUser(data)});
                        });
            }
        });
    });
</script>

<form:form class="form-horizontal"
           commandName="userRole"
           action="${confirmUrl}"
           method="post">

    <fieldset>

        <form:hidden path="id"/>

        <c:if test="${not empty userRole.entityId}">
            <div class="control-group">
                <form:label class="control-label" path="entityId">
                    <spring:message code="views.aclRecord.entity.${entityType}"/>:
                </form:label>
                <div class="controls double-width">
                    <form:input path="entityId" readonly="true" tabindex="${tabIndex}"/>
                </div>
            </div>
        </c:if>

        <div class="control-group">
            <form:label class="control-label" path="userId">
                <spring:message code="views.aclRecord.user"/>:
            </form:label>
            <div class="controls double-width">
                <form:input path="userId" cssErrorClass="error" tabindex="${tabIndex}"/>
                <form:errors path="userId" cssClass="error"/>
            </div>
        </div>

        <div class="control-group">
            <form:label class="control-label" path="role">
                <spring:message code="views.aclRecord.role"/>:
            </form:label>
            <div class="controls">
                <form:select path="role" tabindex="${tabIndex}">
                    <spring:eval var="roles" expression="entityType.getOrderedRoles()"/>
                    <c:forEach items="${roles}" var="role">
                        <form:option value="${role}"><spring:message code="views.aclRecord.role.${role}"/></form:option>
                    </c:forEach>
                </form:select>
                <form:errors path="role" cssClass="error"/>
            </div>
        </div>

    </fieldset>

    <c:if test="${not empty confirmTitle || cancelUrl != null}">
        <div class="control-group">
            <div class="controls">
                <c:if test="${not empty confirmTitle}">
                    <spring:message code="${confirmTitle}" var="confirmTitle"/>
                    <input class="btn btn-primary" type="submit" value="${confirmTitle}" tabindex="${tabIndex}"/>
                </c:if>
                <c:if test="${cancelUrl != null}">
                    <c:if test="${empty cancelTitle}">
                        <c:set var="cancelTitle" value="views.button.cancel"/>
                    </c:if>
                    <a class="btn" href="${cancelUrl}" tabindex="${tabIndex}"><spring:message code="${cancelTitle}"/></a>
                </c:if>
            </div>
        </div>
    </c:if>

</form:form>