package LocatorServer;

import com.google.gson.Gson;
//import javafx.scene.control.RadioMenuItem;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
//import sun.rmi.rmic.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;

import static LocatorServer.MainClass.MYLOGGERSOCKET;
import static LocatorServer.MainClass.websocketConServer;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class ChatServer extends WebSocketServer {

    public ChatServer( int port ) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
    }

    public ChatServer( InetSocketAddress address ) {
        super( address );
    }

    @Override
    public void onOpen( WebSocket conn, ClientHandshake handshake ) {
       // this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
       // System.out.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
        if(handshake.hasFieldValue("contact") == true)
        {
            String contactVal = handshake.getFieldValue("contact");


                if (MainClass.currentAvailableConnections.contains(contactVal) == true)
                    MainClass.currentAvailableConnections.remove(contactVal);
                if (MainClass.currentAvailableConnectionsWebSocketAsKey.contains(conn) == true)
                    MainClass.currentAvailableConnectionsWebSocketAsKey.remove(conn);
                MainClass.currentAvailableConnections.put(contactVal, conn);
                MainClass.currentAvailableConnectionsWebSocketAsKey.put(conn, contactVal);
        }
        else if(handshake.hasFieldValue("Random") == true)
        {
            String rand = RandomString.randomString(10);
            Comms comsMsg = new Comms();
            comsMsg.params = new HashMap<>();
            comsMsg.params.put("SM","RANDOM");
            comsMsg.params.put("RANDOM",rand);
            Gson gson = new Gson();
            String gsonMsg = gson.toJson(comsMsg,Comms.class);
            conn.send(gsonMsg);
        }
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        String contactVal;
        if(MainClass.currentAvailableConnectionsWebSocketAsKey.contains(conn) == true) {
            contactVal = MainClass.currentAvailableConnectionsWebSocketAsKey.get(conn);
            MainClass.currentAvailableConnectionsWebSocketAsKey.remove(conn);

            if(MainClass.currentAvailableConnections.contains(contactVal) == true)
                MainClass.currentAvailableConnections.remove(contactVal);

        }
    }

    @Override
    public void onMessage( WebSocket conn, String message ) {

        Gson gsonObj = new Gson();
        int x = 40;
        try {

            Comms comsObj = gsonObj.fromJson(message, Comms.class);
            if(comsObj != null) {

//                if(comsObj.params.containsKey("ACTION") && comsObj.params.get("ACTION").equals("OTP")) {
////                    String otpStr = RandomString.randomString(4);
////                    comsObj.params.put("string", otpStr);
////                    Gson gson = new Gson();
////                    String gsonMsg = gson.toJson(comsObj, Comms.class);
////                    conn.send(gsonMsg);
//                    if(MainClass.websocketOTPClient != null) {
//                        MainClass.websocketOTPClient.send(message);
//                    }
//                }
//                else {

                    comsObj.params.put("websocket", conn);
                    MainClass.receivedMsgs.add(comsObj.params);
            //    }

            }
            else
            {
              //  byte[] b = message.getBytes();
             //  if((b[0] & 1) == 1)
               {
                   MYLOGGERSOCKET.info(message+"\n");
                   conn.send(message);

               }



            }
        }
        catch(Exception ex)
        {
         //   byte[] b = message.getBytes();
           // if((b[0] & 1) == 1)
            {
             //   if(message.contains("connec"))
                if(message.contains("null") == true) {
                    MYLOGGERSOCKET.info(message + "\n");
                }
                else
                {
                    x = 30;
                }
                conn.send(message);

            }

            int xy = 30;
        }


    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        ex.printStackTrace();
        if( conn != null ) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text
     *            The String to send across the network.
     * @throws InterruptedException
     *             When socket related I/O errors occur.
     */
    public void sendToAll( String text ) {
        Collection<WebSocket> con = connections();
        synchronized ( con ) {
            for( WebSocket c : con ) {
                c.send( text );
            }
        }
    }
}

