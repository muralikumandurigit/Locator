package LocatorServer;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static LocatorServer.MsgHandler.db;

public class TrackMsgsPusherThread extends Thread{
    public TrackMsgsPusherThread() {
    }

    @Override
    public void run() {
        while(true) {
            List<Object[]> contactDetails = db.getContactAndMobileNumbers();
            for(Object[] info: contactDetails){
                String contact = (String)info[0];
                String MobileStamp = (String)info[1];
                PushForLocations obj = new PushForLocations();
                Comms comsObj = new Comms();
                comsObj.params = new HashMap<>();

                Map<String, String> data = new HashMap<>();
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
                data.put("ACTION","TRACK");
                //  obj.data.put("FROMUSER","111");
                obj.data.put("FROMUSER",contact);
                data.put("FROMUSER",contact);


                obj.data.put("TOUSER",contact);
                data.put("TOUSER",contact);
                comsObj.params.put("data", data);


                obj.message_id = "1";
                obj.category = "com.tp.locator";
                obj.time_to_live = 86400;
                try {
                    //String trackMsg = gson.toJson(obj);
                    String trackMsg = gson.toJson(comsObj);
                    MainClass.receivedTrackMsgs.add(trackMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            try {
                Thread.sleep(1000 * 60 * 15 );
                //TimeUnit.MINUTES.sleep(15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
