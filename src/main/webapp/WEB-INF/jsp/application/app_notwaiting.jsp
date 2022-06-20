<%@page contentType="text/html" pageEncoding="UTF-8" isErrorPage="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>

<!DOCTYPE html>
<html lang="${language}" class="tw-<c:out value='${theme}' />">

<head>
    <title>
        <spring:message code="application.error.notWaiting.title"/>
    </title>
    <uv:custom-head/>
    <uv:asset-dependencies-preload asset="application_notwaiting.js" />
    <script type="module" src="<asset:url value='application_notwaiting.js' />"></script>
</head>

<body>

<div class="tw-text-center tw-mt-24">
    <h1 class="tw-text-6xl md:tw-text-10rem tw-leading-normal" style="background-color: #b2c900;">
        <icon:emoji-sad className="tw-w-40 tw-h-40"/>
    </h1>
    <p class="md:tw-text-2xl">
        <spring:message code="application.error.notWaiting.body"/>
    </p>
</div>
</body>
</html>
