package LocatorServer;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnouncementsThread extends Thread {

    MainClass commObj;
    MainClass commObj1;

    String collapseKey = "echo:CollapseKey";

    public AnnouncementsThread(MainClass commObj) {
        this.commObj = commObj;
        this.commObj1 = commObj;
    }

    @Override
    public void run() {
        Map<String, String> payload = new HashMap<>();
        Map<String, String> payload1 = new HashMap<>();


        while(true)
        {
            while(MsgHandler.pendingAnnouncementQueue.size() > 0)
            {
                AnnouncementGen obj = MsgHandler.pendingAnnouncementQueue.remove();
                ConcurrentHashMap<String, ContactDetails> cntctDetails;
                if(TrieCode.details.containsKey(obj.geoHash) == true)
                {
                    MainClass.MYLOGGER.info("In subscriberThread .TrieCode.details. contains  key : "+obj.geoHash);
                    cntctDetails = TrieCode.details.get(obj.geoHash);
                    MainClass.MYLOGGER.info("_____________________");
                    cntctDetails.forEach((k,v) -> {
                        MainClass.MYLOGGER.info("In subscriberThread .TrieCode.details. contains  key : value "+k+":"+ v.Contact);
                    });
                    MainClass.MYLOGGER.info("++++++++++++++++++++++++");

                }
                else
                {
                    cntctDetails = new ConcurrentHashMap<>();
                    if(obj.mobileDetails != null && obj.mobileDetails.Contact != null)
                        cntctDetails.put(obj.mobileDetails.Contact, obj.mobileDetails);
                    TrieCode.details.put(obj.geoHash, cntctDetails);
                }

//                ContactDetails temp = new ContactDetails();
//                temp.Contact = obj.mobileDetails.Contact;
//                temp.mobileStamp = obj.mobileDetails.mobileStamp;
//
//                cntctDetails.put(temp.mobileStamp, temp);



//                strMsg = payload.get("strMsg");
//                todayDate = payload.get("todayDate");
//                steTimeStamp = payload.get("steTimeStamp");
//                cat = payload.get("category");


                payload.put(MainClass.MESSAGE_KEY, "ANN");
                payload.put("ACTION","ANN");
                payload.put("strMsg",obj.msg);
                payload.put("date",obj.date);
                payload.put("TimeStamp",obj.timestamp);
                if(obj.imgReceived.length() > 0)
                    payload.put("img", obj.imgReceived);

                Comms comsObj = new Comms();
                comsObj.params = new HashMap<>();
                comsObj.params.put(MainClass.MESSAGE_KEY, "ANN");
                comsObj.params.put("ACTION","ANN");
                comsObj.params.put("strMsg",obj.msg);
                comsObj.params.put("date",obj.date);
                comsObj.params.put("TimeStamp",obj.timestamp);
                comsObj.params.put("Name",obj.mobileDetails.Name);
                comsObj.params.put("Sex",obj.mobileDetails.sex);
                comsObj.params.put("Contact",obj.mobileDetails.Contact);
                comsObj.params.put("geoHash",obj.geoHash);
                if(obj.imgReceived.length() > 0)
                    comsObj.params.put("img", obj.imgReceived);



                cntctDetails.forEach((k,v) ->
                {

                    org.java_websocket.WebSocket wsock = MainClass.currentAvailableConnections.get(k);
                    String message = MainClass.createJsonMessage(k, commObj.getRandomMessageId(), payload,
                            collapseKey, null, false);
                    //  MainClass.MYLOGGER.info("sending NEAREST to "+v.Contact+" : "+k+" .. \n message is "+tempStr+"\n");

                    Gson gson = new Gson();
                    message = gson.toJson(comsObj, Comms.class);
                    if(wsock != null && wsock.isOpen() == true && wsock.isClosing() == false)
                        wsock.send(message);
                   // commObj.send(message);
                });
             //   TrieCode.details.put(obj.geoHash,cntctDetails);

                if(obj.prevgeoHash != null && TrieCode.details.contains(obj.prevgeoHash) == true)
                {
                    cntctDetails = TrieCode.details.get(obj.prevgeoHash);
                    cntctDetails.remove(obj.mobileDetails.mobileStamp);
                }

            }
        }    }
}
