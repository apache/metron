<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
	
	<head>
		<title>Do something with WebSockets</title>
		
		<script src="//code.jquery.com/jquery-1.11.0.min.js"></script>	
		<script type="text/javascript">
		$(document).ready(function() {
			
				var exampleSocket = null;
				var groupId = null;
			
			   	$("#startBtn").click(
					function() {
						exampleSocket = new WebSocket("wss://localhost:8443/ws/messages");
						if( exampleSocket != null )
						{
							
							exampleSocket.onmessage = function (event)
							{
								
								var msg = event.data;
								var index = msg.search( "groupId:" ); 
								if(  index >= 0 )
								{
									console.log( "groupId message" );
									// this is our groupId from the server, save it for future use
									
									groupId = msg.substring( index + 8 ).trim();
									console.log( "groupId: " + groupId );
								}
								else
								{
									// console.log( "alert message" );
									// this is an alert message, just render it...
									
									// console.log( event.data );
									$('#msgList').append( '<li>' + msg + '</li>' );
								}
							}
							
							exampleSocket.onopen = function() {
									console.log( "exampleSocket is open.")
									console.log( "sending startMessages signal" );
									exampleSocket.send( "startMessages" );		
							}
						}
						else
						{
							alert( "exampleSocket is null!" );
						}
						
					}	   
			   )
			   
			   $("#stopBtn").click(
					function() {
						exampleSocket.send( "stopMessages");
					}
				)
			   
			})
		
		</script>
	</head>
	
	<body>
		<h3>Get some data with a WebSocket</h3>
		
		<button id="startBtn">Start</button> <span style="min-width:140px;"></span> <button id="stopBtn">Stop</button>

		<div id="msgArea" style="background-color:#F8F8F8;color:red;min-height:100px; min-width:400px;">
			<ul id="msgList" >
			
			</ul>
		
		</div>
		
		<div id="debug">
		<ul>
    		<c:forEach var="req" items="${header}">
        		<li>
            		<c:out value="${req.key}"/> &nbsp; = &nbsp; <c:out value="${req.value}"/>
        		</li>
    		</c:forEach>
    	</ul>
    	<hr />
    	<ul>
    		<c:forEach var="currentCookie" items="${cookie}">
        		<li>
            		<c:out value="${currentCookie.key}"/> &nbsp; = &nbsp; <c:out value="${currentCookie.value.value}"/>
        		</li>
    		</c:forEach>
    	</ul>
		</div>
	</body>
	
</html>