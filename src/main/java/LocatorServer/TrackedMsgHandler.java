package LocatorServer;

import LocatorServer.dblayer.dbManager;
import com.google.gson.Gson;
import org.codehaus.jackson.map.ObjectMapper;
import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static LocatorServer.MainClass.LastKnownLocs;
import static LocatorServer.MainClass.requiredLocations;
import static java.util.concurrent.TimeUnit.MINUTES;

public class TrackedMsgHandler extends Thread{
    MainClass obj;
    public static ConcurrentLinkedQueue<Map<String, Object>> receivedMsgs = new ConcurrentLinkedQueue<>();
    public static ConcurrentHashMap<String, ArrayList<String>> receivedPaths = new ConcurrentHashMap<>();
    static dbManager db;
    ObjectMapper objMapper = new ObjectMapper();



    public TrackedMsgHandler(MainClass mainObj) {
        obj = mainObj;
        db   = new dbManager();
        // TODO Auto-generated constructor stub

        List<Object[]> contactDetails = db.getContactAndMobileNumbers();
        for(Object[] info: contactDetails){
            String contact = (String)info[0];
            String MobileStamp = (String)info[1];
            PushForLocations obj = new PushForLocations();
         //   obj.from = "111";
            obj.from = contact;
            obj.data = new HashMap<>();
            Cdata tempObj = new Cdata();

            tempObj.ACTION = "TRACK";
         //   tempObj.FROMUSER = "111";
            tempObj.FROMUSER = contact;
            tempObj.TOUSER = contact;

            Gson gson = new Gson();
            String jsonString = gson.toJson(tempObj);

            JSONObject cdataJson = null;
            try {
                cdataJson = new JSONObject(jsonString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
           // obj.data =cdataJson;
            obj.data.put("ACTION","TRACK");
          //  obj.data.put("FROMUSER","111");
            obj.data.put("FROMUSER",contact);
            obj.data.put("TOUSER",contact);

            obj.message_id = "1";
            obj.category = "com.tp.locator";
            obj.time_to_live = 86400;
            try {
                String trackMsg = gson.toJson(obj);
                //Gopi .. stopped temp.
           //     MainClass.receivedTrackMsgs.add(trackMsg);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
//
//		ScheduledExecutorService service = Executors
//				.newSingleThreadScheduledExecutor();
//		service.scheduleAtFixedRate(runnable, 0, 1, MINUTES);

    }


    @Override
    public void run() {
        while(obj.connection == null || obj.connection.isConnected() == false)
        {

        }

        while (true) {
            // TODO Auto-generated method stub
            while (MainClass.receivedTrackMsgs.size() > 0) {
                String strjsonObject = MainClass.receivedTrackMsgs.remove();
                String from = null;
                Comms tempPush = null;

                JSONObject jsonObject = null;
                Gson gson=new Gson();

                tempPush = gson.fromJson(strjsonObject,Comms.class);


                // PackageName of the application that sent this message.
                // String category = jsonObject.get("category").toString();

                // Use the packageName as the collapseKey in the echo packet
                String collapseKey = "echo:CollapseKey";
                @SuppressWarnings("unchecked")
                Map<String, String> payload = null;
                JSONObject jsonPayLoad= null;
                //  jsonPayLoad = (JSONObject) jsonObject.get("data");
                payload = (Map<String, String>) tempPush.params.get("data");

                String action = payload.get("ACTION");

             if ("TRACK".equals(action)) {

//					Map<String, String> regIdMap = obj.readFromFile();
                    payload.put(MainClass.MESSAGE_KEY, "TRACK");
                    String toUser = payload.get("TOUSER").trim();
//
                    String fromUser = payload.get("FROMUSER").trim();


                 Comms comsObj = new Comms();
                 comsObj.params = new HashMap<>();
                 comsObj.params.put("SM","TRACK");
                 comsObj.params.put("FROMUSER",fromUser);
                 comsObj.params.put("TOUSER", toUser);

                 String tempMsg = gson.toJson(comsObj, Comms.class);
                 WebSocket tempSoc = MainClass.currentAvailableConnections.get(toUser);
                 if(tempSoc != null)
                    tempSoc.send(tempMsg);

                    //	dbManager db = new dbManager();
//                    if(db == null)
//                        db  = new dbManager();
//
//                    if(toUser.equalsIgnoreCase("+910000000000"))
//                        toUser = fromUser;
//                    //String toUserRegid = db.getMobileStamp(fromUser);
//                    String toUserRegid = db.getMobileStamp(toUser);
//
//                    //	String toUserRegid = regIdMap.get("+917287885882");
//                    if(toUserRegid.length() > 0) {
//                        payload.put("TOUSER", toUser);
//                        payload.put("FROMUSER", fromUser);
//                        String message = MainClass.createJsonMessage(toUserRegid, obj.getRandomMessageId(), payload,
//                                collapseKey, null, false);
//
//                        obj.send(message);
//                    }
                } else if ("TRACKRESPONSE".equals(action)) {

                    //	Map<String, String> regIdMap = obj.readFromFile();
                    payload.put(MainClass.MESSAGE_KEY, "TRACKRESPONSE");
                    String toUser = payload.get("TOUSER");
                    //dbManager db = new dbManager();
                    if(db == null)
                        db  = new dbManager();

                    payload.put("TOUSER", toUser);
                    String address = payload.get("ADDRESS");
                    String fromNum = "";
                    if(payload.containsKey("FROMUSER"))
                        fromNum = payload.get("FROMUSER");
                    String dateTime = payload.get("at");
                    payload.put("Address", address);
                    payload.put("at", dateTime);
                    if(toUser != null && toUser == "111")
                    {
                        ConcurrentLinkedQueue<String> addressesList = LastKnownLocs.get(fromNum);
                        if(addressesList == null)
                            addressesList = new ConcurrentLinkedQueue<>();
                        addressesList.add(address);
                        LastKnownLocs.remove(fromNum);
                        LastKnownLocs.put(fromNum, addressesList);

                    }
                    else {
                        String toUserRegid = db.getMobileStamp(toUser);//regIdMap.get(toUser);
                        String message = MainClass.createJsonMessage(toUserRegid, obj.getRandomMessageId(), payload,
                                collapseKey, null, false);
                        obj.send(message);
                    }
                }
            }
        }

    }

}
