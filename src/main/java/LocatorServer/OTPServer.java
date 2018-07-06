package LocatorServer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class OTPServer extends WebSocketServer {
    public OTPServer(int port) {
        super(new InetSocketAddress( port ));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            MainClass.websocketOTPClient = webSocket;
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {

    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }
}
