# WebSocket开发笔记

## 关键代码

### 客户端
通过 `new WebSocket` 创建WebSocket对象，构造方法`WebSocket` 的参数是请求的类型和服务端地址，连接成功之后会执行WebSocket对象的`onpen`事件

```JavaScript
var wsUrl = "ws://localhost:8080/chat/chatRoomServer";
var ws = new WebSocket(wsUrl);
```
`ws.onopen` 事件在客户端与服务端建立连接之后触发
```javascript
ws.onopen = function() {
  //...
}
```

服务器推送消息到客户端的时候触发 `ws.onmessage` 事件
```javascript
ws.onmessage = function(message) {
  //message.data是信息的内容
 	//...
}
```

用户退出或者刷新浏览器的时候要结束与服务端的连接 `ws.close()`方法会通知服务端断开连接

```JavaScript
ws.close();
```

### 服务端

| 注解              | 作用                                                         |
| ----------------- | ------------------------------------------------------------ |
| `@ServerEndpoint` | 声明`WebSocket`服务端地址                                    |
| `@OnOpen`         | 服务端收到客户端的连接请求，连接成功后会执行该注解标注的方法 |
| `@OnMessage`      | 当接收到客户端发过来的信息                                   |
| `@OnClose`        | 客户端`ws.close()`事件，会触发后台标注的`OnClose`方法        |



@ServerEndpoint标记在类上，标明`WebSocket`服务端地址

```java
//声明WebSocket服务端地址
@ServerEndpoint("/chatRoomServer")
```



## 客户端代码

```html
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

```



## 服务端代码

```java
package com.zhangjinbang.chat;

import java.io.IOException;
import java.util.HashMap;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

//声明WebSocket服务端地址
@ServerEndpoint("/chatRoomServer")
public class ChatRoomServer {
	
	private boolean firstFlag = true;
	private Session session;
	private String username;

	// 记录此次聊天室有多少客户端连接
	// key 代表此次客户端的session，value代表此次连接对象
	private static final HashMap<String, Object> connectMap = new HashMap<String, Object>();

	// 用户列表（实际环境中用redis比较好）
	// key 是 session的Id,value是用户名
	private static final HashMap<String, String> userMap = new HashMap<String, String>();

	// 服务端收到客户端的连接请求，连接成功后会执行这个方法
	@OnOpen
	public void start(Session session) {

		// 记录客户端的唯一标识
		this.session = session;
		connectMap.put(session.getId(), this);
	}

	// 当接收到客户端发过来的信息
	@OnMessage
	public void chat(String clientMessage, Session session) {
		ChatRoomServer client = null;
		// 判断客户端是不是第一次
		if (firstFlag) {
			this.username = clientMessage;
			// 将新进来的用户保存到用户列表
			userMap.put(session.getId(), this.username);
			// 构造发给客户端的提示信息
			String message = "系统消息：" + this.username + "进入聊天室";
			// 将消息广播给所有的用户
			for (String connectKey : connectMap.keySet()) {
				client = (ChatRoomServer) connectMap.get(connectKey);
				// 给对应的web端发送一个文本信息（message）
				try {
					client.session.getBasicRemote().sendText(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// 输入完昵称之后，firstFlag=false
			this.firstFlag = false;
		} else {
			for (String connectKey : connectMap.keySet()) {
				String username=userMap.get(session.getId());
				client = (ChatRoomServer) connectMap.get(connectKey);
				// 给对应的web端发送一个文本信息（message）
				try {
client.session.getBasicRemote().sendText(username+":"+clientMessage);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	//ws.close()事件，会触发后台标注的OnClose方法
	@OnClose
	public void close(Session session) {
		ChatRoomServer client=null;
		//当某一个用户退出的时候，对其他用户进行广播
		String message="系统消息:"+userMap.get(session.getId())+"退出了聊天室";
		userMap.remove(session.getId());
		connectMap.remove(session.getId());
		//将用户退出的消息广播给所有的在线用户
		for(String connectKey:connectMap.keySet()) {
			client=(ChatRoomServer)connectMap.get(connectKey);
			try {
				client.session.getBasicRemote().sendText(message);
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}

```











