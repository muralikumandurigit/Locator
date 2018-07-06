package LocatorServer;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriberThread extends Thread {

    MainClass commObj;
    MainClass commObj1;

    String collapseKey = "echo:CollapseKey";


    public SubscriberThread(MainClass commObj) {
        this.commObj = commObj;
        this.commObj1 = commObj;
    }

    @Override
    public void run() {

        Map<String, String> payload = new HashMap<>();
        Map<String, String> payload1 = new HashMap<>();


        while(true)
        {
            while(MsgHandler.pendingQueue.size() > 0)
            {
                ContactDetailsWithGeoHash obj = MsgHandler.pendingQueue.remove();
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
                }

                Comms comsObj = new Comms();
                Comms comsObj1 = new Comms();
                comsObj.params = new HashMap<>();
                comsObj1.params = new HashMap<>();
                ContactDetails temp = new ContactDetails();
                temp.Contact = obj.mobileDetails.Contact;
                temp.mobileStamp = obj.mobileDetails.mobileStamp;

                cntctDetails.put(temp.Contact, temp);
                payload.put(MainClass.MESSAGE_KEY, "NEAREST");
                payload.put("ACTION","NEAREST");
                comsObj.params.put(MainClass.MESSAGE_KEY, "NEAREST");
                comsObj.params.put("ACTION", "NEAREST");

                payload1.put(MainClass.MESSAGE_KEY, "NEAREST");
                payload1.put("ACTION","NEAREST");


                comsObj1.params.put(MainClass.MESSAGE_KEY, "NEAREST");
                comsObj1.params.put("ACTION", "NEAREST");

                Gson gson = new Gson();
             //   String tempStr = gson.toJson(temp);
                String tempStr = temp.Name+" : "+temp.sex;
                payload.put("CNTS", tempStr);
                comsObj.params.put("CNTS", tempStr);

                //comsObj1.params.put("CNTS", tempStr);

                MainClass.MYLOGGER.info("for geo key : "+obj.geoHash+"  numbers are :::::::: ");

                cntctDetails.forEach((k,v)->{
                    MainClass.MYLOGGER.info("number  "+k+"\n ");

                });
                MainClass.MYLOGGER.info("for geo key : "+obj.geoHash+"  numbers loop over +++++ ");

                cntctDetails.forEach((k,v) ->
                        {
                            WebSocket conn1 = null;
                            ContactDetails temp1 = new ContactDetails();
                            temp1.Contact = v.Contact;
                            temp1.mobileStamp = v.mobileStamp;
                            WebSocket conn = MainClass.currentAvailableConnections.get(k);
                            if(temp!= null && temp.Contact!= null)
                             conn1 = MainClass.currentAvailableConnections.get(temp.Contact);
                         //   if(conn != null) {
                                Gson gson1 = new Gson();
                               // String tempStr1 = gson1.toJson(temp1);
                            String tempStr1 = temp1.Name +":" +temp1.sex;
                                payload1.put("CNTS", tempStr1);
                                comsObj1.params.put("CNTS", tempStr1);

                                String msg = gson.toJson(comsObj, Comms.class);
                                String message = MainClass.createJsonMessage(k, commObj.getRandomMessageId(), payload,
                                        collapseKey, null, false);
                                //  MainClass.MYLOGGER.info("sending NEAREST to "+v.Contact+" : "+k+" .. \n message is "+tempStr+"\n");
                                MainClass.MYLOGGER.info("sending NEAREST to " + v.Contact + " : .. contact = " + temp.Contact + " .. \n message is " + tempStr + "\n");

                                if(conn != null && conn.isOpen() == true && conn.isClosing() == false)
                                    conn.send(msg);
                               // commObj.send(message);
                                String message1 = MainClass.createJsonMessage(temp.mobileStamp, commObj.getRandomMessageId(), payload1,
                                        collapseKey, null, false);
                                //  MainClass.MYLOGGER.info("sending NEAREST to "+v.Contact+" : "+k+" .. \n message is "+tempStr+"\n");
                                MainClass.MYLOGGER.info("sending NEAREST to " + temp.Contact + " : .. contact = " + v.Contact + " .. \n message is " + tempStr1 + "\n");

                            String msg1 = gson.toJson(comsObj1, Comms.class);

                            if(conn1 != null && conn1.isOpen() == true && conn1.isClosing() == false)
                                conn1.send(msg1);

                            // commObj1.send(message1);
                           // }


            });
                TrieCode.details.put(obj.geoHash,cntctDetails);

                if(obj.prevgeoHash != null && TrieCode.details.contains(obj.prevgeoHash) == true)
                {
                    cntctDetails = TrieCode.details.get(obj.prevgeoHash);
                    cntctDetails.remove(obj.mobileDetails.mobileStamp);
                }

            }
        }
    }
}
