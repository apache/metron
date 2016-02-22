<%--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>

	<head>
		<title>Do something with WebSockets</title>

		<script src="//code.jquery.com/jquery-1.11.0.min.js"></script>
		<script type="text/javascript">
		$(document).ready(function(){
			   $("#startBtn").click(
					function() {
						var exampleSocket = new WebSocket("wss://localhost:8443/ws/messages");
						if( exampleSocket != null )
						{

							exampleSocket.onmessage = function (event)
							{

								// console.log( event.data );
								$('#msgList').append( '<li>' + event.data + '</li>' );
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
			})

		</script>
	</head>

	<body>
		<h3>Get some data with a WebSocket</h3>

		<button id="startBtn">Start</button>

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
