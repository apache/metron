<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
	
	<head>
		<title>Login</title>
	</head>
	
	<body>
		<h3>Logged In!</h3>
		
		 <ul>
    		<c:forEach var="currentCookie" items="${cookie}">
        		<li>
            		<c:out value="${currentCookie.key}"/>:Object=<c:out value="${currentCookie.value.name}"/>, value=<c:out value="${currentCookie.value.value}"/>
        		</li>
    		</c:forEach>
    	</ul>
		
	</body>
	
</html>