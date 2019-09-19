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
