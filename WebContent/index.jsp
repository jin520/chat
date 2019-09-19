<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>聊天室</title>
<style type="text/css">
* {
	margin: 0;
	padding: 0;
	background: black;
	color: yellow;
}

.messageBox {
	width: 100%;
	height: 500px;
	background: black;
	color: red;
	padding: 30px;
	font-size: 50px;
	overflow-x: hidden;
	overflow-y: scroll;
}

.messageBox::-webkit-scrollbar {
	display: none;
}

#message {
	width: 100%;
	height: 100px;
	font-size: 80px;
	border: none;
	border-top: 5px solid green;
}
</style>
</head>
<body>
	<div id="outputMessage" class="messageBox"></div>
	<input type="text" id="message" placeholder="输入要发送的消息">
</body>

<script type="text/javascript">
	var outputMessage = document.getElementById("outputMessage");
	//服务端地址和请求类型
	var wsUrl = "ws://localhost:8080/chat/chatRoomServer";
	//客户端和服务器建立连接，建立连接后，他会触发一个ws.onopen事件
	var ws = new WebSocket(wsUrl);

	//建立连接后，提示浏览器客户端输入昵称
	ws.onopen = function() {
		var username = prompt("请给自己取一个名字");
		ws.send(username);
	}
	//客户端收到服务器的消息
	ws.onmessage = function(message) {
		//message.data是信息的内容
		//将信息渲染到界面上

		outputMessage.innerHTML += message.data + "<br>";

	}

	//获取用户输入的聊天内容，并发送到服务端，让服务端广播给所有人
	function getMessage() {

		var inputMessage = document.getElementById("message").value;

		if (typeof (inputMessage) == "undefined" || inputMessage == "") {
			alert("请输入您要发送的消息");
		} else {
			//发送消息给服务端
			ws.send(inputMessage);
			//清空文本框的内容
			document.getElementById("message").value = "";

		}
	}

	//回车事件
	document.onkeydown = function(e) {
		if (e.keyCode == 13) {

			getMessage();
		}
	}

	//当关闭页面，或用户退出，要执行一个ws.close()方法
	window.onbeforeunload = function() {
		//ws.close会触发后台的一个方法
		ws.close();
	}
</script>
</html>
