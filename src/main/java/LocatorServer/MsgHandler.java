package LocatorServer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import LocatorServer.dblayer.dbManager;
import com.google.gson.Gson;
import org.codehaus.jackson.map.ObjectMapper;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

import static LocatorServer.MainClass.*;
import static java.util.concurrent.TimeUnit.MINUTES;

public class MsgHandler extends Thread{
	MainClass obj;
	public static ConcurrentLinkedQueue<Map<String, Object>> receivedMsgs = new ConcurrentLinkedQueue<>();
    public static ConcurrentHashMap<String,String> currentLocs = new ConcurrentHashMap<>();
    public static ConcurrentLinkedQueue<ContactDetailsWithGeoHash> pendingQueue = new ConcurrentLinkedQueue<>();
	public static ConcurrentLinkedQueue<AnnouncementGen> pendingAnnouncementQueue = new ConcurrentLinkedQueue<>();



	static dbManager db;
	ObjectMapper objMapper = new ObjectMapper();



	public MsgHandler(MainClass mainObj) {
		obj = mainObj;
		db   = new dbManager();
		// TODO Auto-generated constructor stub

	}


	@Override
	public void run() {

		while (true) {
			// TODO Auto-generated method stub
			while (MainClass.receivedMsgs.size() > 0) {

				Map<String, Object> jsonObject = MainClass.receivedMsgs.remove();

				String tempStr = jsonObject.toString();

				MainClass.MYLOGGER.info("Message received is "+tempStr);


				String from = jsonObject.get("from").toString();
				int result = 0;

				// PackageName of the application that sent this message.
				// String category = jsonObject.get("category").toString();

				// Use the packageName as the collapseKey in the echo packet
				String collapseKey = "echo:CollapseKey";
				@SuppressWarnings("unchecked")
				Map<String, String> payload = (Map<String, String>) jsonObject.get("data");

				String action = payload.get("ACTION");

				if ("ECHO".equals(action)) {

					String clientMessage = payload.get("CLIENT_MESSAGE");
					payload.put(MainClass.MESSAGE_KEY, "ECHO: " + clientMessage);

					// Send an ECHO response back
					String echo = MainClass.createJsonMessage(from, obj.getRandomMessageId(), payload, collapseKey,
							null, false);
					obj.send(echo);
				} else if ("SIGNUP".equals(action)) {
					StringBuilder userNameStr = new StringBuilder();
                    String userName = "";
				//	dbManager db = new dbManager();
					try {
						 userName = payload.get("USER_NAME");
                        MainClass.MYLOGGER.info("In Signup for userName : "+userName);

						if(db == null)
							db  = new dbManager();
						//String fromMobilestamp = db.getMobileStamp(userName);
						boolean isRegistered = db.isContactRegistered(userName);
//						if(fromMobilestamp != null && fromMobilestamp.length() > 0 && fromMobilestamp.equalsIgnoreCase(from ) == true) {
//                            result = 1;
//                            MainClass.MYLOGGER.info("User already registered. So returning 1 for result");
//                        } else if(fromMobilestamp != null && fromMobilestamp
//								.length() > 0)
//						{
//                            MainClass.MYLOGGER.info("User already registered. but now registering on other phone .So returning 1 for result");
//
//                           String ret = db.updateMobileStamp(userName, from);
//                           if(ret.length() <= 0)
//							result = 0;
//                           else
//                               result = 1;
//						}
//						else {
//                            MainClass.MYLOGGER.info("User is registering. userName "+userName);
//
//                            result = db.createRegistration(userName, userName, from);
//                        }
                        if(isRegistered == true)
                        {
                            result = 1;
                        }
                        else
                        {
                            MainClass.MYLOGGER.info("User is registering. userName "+userName);

                            result = db.createRegistration(userName, userName, from);
                        }
								if(result == 1)
								{
                                    PushForLocations obj = new PushForLocations();
                                    obj.from = userName;
                                   // obj.data = new JSONObject();
                                    Cdata tempObj = new Cdata();

                                    tempObj.ACTION = "TRACK";
                                   // tempObj.FROMUSER = "111";
                                    tempObj.FROMUSER = userName;
                                    tempObj.TOUSER = userName;

                                    Gson gson = new Gson();
                                    String jsonString = gson.toJson(tempObj);

                                    JSONObject cdataJson = new JSONObject(tempObj);
                                    obj.data = new HashMap<>();

                                    // obj.data =cdataJson;
                                    obj.data.put("ACTION","TRACK");
                                   // obj.data.put("FROMUSER","111");
                                    obj.data.put("FROMUSER",userName);
                                    obj.data.put("TOUSER",userName);
                                    obj.message_id = "1";
                                    obj.category = "com.tp.locator";
                                    obj.time_to_live = 86400;
                                    try {
                                        String trackMsg = objMapper.writeValueAsString(obj);
                                        requiredLocations.add(trackMsg);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
								}
						System.out.println("result is "+result);
					

					} catch (Exception e) {
						result = 0;
						payload.put("RESULT","0");
						e.printStackTrace();
					} finally {
						String strResult = String.valueOf(result);
						Comms comsMsg = new Comms();
						comsMsg.params = new HashMap<>();
						comsMsg.params.put("RESULT",strResult);
						payload.put("RESULT",strResult);
						payload.put(MainClass.MESSAGE_KEY, "SIGNUPRESPONSE");
                        comsMsg.params.put(MainClass.MESSAGE_KEY, "SIGNUPRESPONSE");

                        payload.put("ACTION","SIGNUPRESPONSE");
                        comsMsg.params.put("ACTION","SIGNUPRESPONSE");

                        payload.put("USER_NAME", userName);
                        comsMsg.params.put("USER_NAME", userName);

                        WebSocket ws = (WebSocket) jsonObject.get("websocket");

//                        String message = MainClass.createJsonMessage(from, obj.getRandomMessageId(), payload,
//								collapseKey, null, false);
//						if(db == null)
//							db  = new dbManager();
//						obj.send(message);
                        Gson gson = new Gson();
                        String message = gson.toJson(comsMsg,Comms.class);
                        if(ws != null && ws.isOpen() == true && ws.isClosing() == false) {
                            ws.send(message);
                            currentAvailableConnections.put(userName,ws);
                            currentAvailableConnectionsWebSocketAsKey.put(ws,userName);

                        }
					}
				} else if ("USERLIST".equals(action)) {

					Map<String, String> regIdMap = obj.readFromFile();
					String users = "";
					for (Map.Entry<String, String> entry : regIdMap.entrySet()) {
						users = users + entry.getKey() + ":";
					}
					payload.put(MainClass.MESSAGE_KEY, "USERLIST");
					payload.put("USERLIST", users);

					String message = MainClass.createJsonMessage(from, obj.getRandomMessageId(), payload, collapseKey,
							null, false);
					if(db == null)
						db  = new dbManager();
					obj.send(message);
				} else if ("TRACK".equals(action)) {



//					Pusher pusher = new Pusher("490061", "75da8e4d87659a560b54", "51cc321c2559038ff8ed");
//					pusher.setCluster("ap2");
//					pusher.setEncrypted(true);
//
//					pusher.trigger("my-channel", "my-event", Collections.singletonMap("message", "hello world"));

//					Map<String, String> regIdMap = obj.readFromFile();
					payload.put(MainClass.MESSAGE_KEY, "TRACK");
					String toUser = payload.get("TOUSER").trim();
//					
					String fromUser = payload.get("FROMUSER").trim();
                    MainClass.MYLOGGER.info("In Track toUser : "+toUser+"  fromUser : "+fromUser);

                    //	dbManager db = new dbManager();
					if(db == null)
						db  = new dbManager();

					if(toUser.equalsIgnoreCase("+910000000000"))
							toUser = fromUser;
					//String toUserRegid = db.getMobileStamp(fromUser);
                    WebSocket tempSoc;
                    Gson gson = new Gson();
					if((tempSoc = currentAvailableConnections.get(toUser)) != null)
					{
					//	tempSoc = currentAvailableConnections.get(toUser);
						Comms comsObj = new Comms();
						comsObj.params = new HashMap<>();
						comsObj.params.put("SM","TRACK");
						comsObj.params.put("FROMUSER",fromUser);
                        comsObj.params.put("TOUSER", toUser);
						String rand = RandomString.randomString(10);

						comsObj.params.put("MSGID",rand);

                        String tempMsg = gson.toJson(comsObj, Comms.class);
						tempSoc.send(tempMsg);
					}
					String toUserRegid = db.getMobileStamp(toUser);

					//	String toUserRegid = regIdMap.get("+917287885882");
                    //Gopi .. I commented
//					if(toUserRegid.length() > 0) {
//						payload.put("TOUSER", toUser);
//						payload.put("FROMUSER", fromUser);
//						String message = MainClass.createJsonMessage(toUserRegid, obj.getRandomMessageId(), payload,
//								collapseKey, null, false);
//
//						obj.send(message);
//					}
				} else if ("TRACKRESPONSE".equals(action)) {

				    Comms comsObj = new Comms();
				    comsObj.params = new HashMap<>();
				    comsObj.params.put("SM","TRACKRESPONSE");
				//	Map<String, String> regIdMap = obj.readFromFile();
					payload.put(MainClass.MESSAGE_KEY, "TRACKRESPONSE");
					String toUser = payload.get("TOUSER");
                    MainClass.MYLOGGER.info("In TRACKRESPONSE toUser : "+toUser);

                    //dbManager db = new dbManager();
					if(db == null)
						db  = new dbManager();

					payload.put("TOUSER", toUser);
                    comsObj.params.put("TOUSER", toUser);

                    String address = payload.get("ADDRESS");
					String fromNum = "";
					String Name = "";
					String sex = "";
					String Lat = "";
					String Lng = "";
					String geoHashF = "";
                    String geoHash6 = "";
                    String prevgeoHash6 = "";

                    if(payload.containsKey("FROMUSER")) {
                        fromNum = payload.get("FROMUSER");
                        MainClass.MYLOGGER.info("(duplicate msg )In TRACKRESPONSE toUser : "+toUser+"  fromUser : "+fromNum);

                    }
                    fromNum = from;
                    if(payload.containsKey("Name"))
                    	Name = payload.get("Name");
                    if(payload.containsKey("sex"))
                    	sex = payload.get("sex");
					if(payload.containsKey("LAT"))
                    {
                        Lat = payload.get("LAT");
                        payload.put("LAT", Lat);
                        comsObj.params.put("LAT", Lat);


                        address += "\n LAT :"+Lat;
                    }
                    if(payload.containsKey("LNG"))
                    {
                        Lng = payload.get("LNG");
                        payload.put("LNG", Lng);
                        comsObj.params.put("LNG", Lng);

                        address += "\n LNG :"+Lng;

                    }
                    if(payload.containsKey("Geohash_F"))
                    {
                        geoHashF = payload.get("Geohash_F");
                        payload.put("Geohash_F", geoHashF);
                        comsObj.params.put("Geohash_F", geoHashF);

                        address += "\n Geohash_F :"+geoHashF;

                    }
                    if(payload.containsKey("GEOHASH_6Chars"))
                    {
                        geoHash6 = payload.get("GEOHASH_6Chars");
                        payload.put("GEOHASH_6Chars", geoHash6);
                        comsObj.params.put("GEOHASH_6Chars", geoHash6);

                        address += "\n GEOHASH_6Chars :"+geoHash6;

                    }
                    if(payload.containsKey("PREV_LOC"))
                    {
                        prevgeoHash6 = payload.get("PREV_LOC");

                    }

                  //  currentLocs.put(geoHash6, from);
					String dateTime = payload.get("at");
					payload.put("Address", address);
                    comsObj.params.put("Address", address);

                    payload.put("at", dateTime);
                    comsObj.params.put("at", dateTime);
                    String rand = RandomString.randomString(10);

                    comsObj.params.put("MSGID",rand);


                    //	if(toUser != null && toUser.equalsIgnoreCase("111"))
//                    if(toUser != null && toUser.equalsIgnoreCase(fromNum) &&
//                            !fromNum.equalsIgnoreCase("+919849124680") &&
//                            !fromNum.equalsIgnoreCase("+918179442558")&&
//                            !fromNum.equalsIgnoreCase("+918686091898"))
//					{
//						ConcurrentLinkedQueue<String> addressesList = LastKnownLocs.get(fromNum);
//						if(addressesList == null)
//							addressesList = new ConcurrentLinkedQueue<>();
//						addressesList.add(address);
//						LastKnownLocs.remove(fromNum);
//						LastKnownLocs.put(fromNum, addressesList);
//
//					}
//					else {
					if(toUser.equals("00000") == false) {
						String toUserRegid = db.getMobileStamp(toUser);//regIdMap.get(toUser);
						String message = MainClass.createJsonMessage(toUserRegid, obj.getRandomMessageId(), payload,
								collapseKey, null, false);
						Gson gson = new Gson();
                        WebSocket tempSoc;

                        if((tempSoc = currentAvailableConnections.get(toUser)) != null)
                        {
                            String tempMsg = gson.toJson(comsObj, Comms.class);
                            tempSoc.send(tempMsg);
                        }
                        //Gopi .. I commented ..
						//obj.send(message);
					}
				//	}
					if(geoHashF.length() > 0) {
						final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

						String currentDT =         dtf.format(LocalDateTime.now());

						ContactDetailsWithGeoHash ContactDetailsWithGeoHashobj = new ContactDetailsWithGeoHash();
                        ContactDetailsWithGeoHashobj.mobileDetails = new ContactDetails();
                        ContactDetailsWithGeoHashobj.geoHash = geoHashF.substring(0, 3);
                        ContactDetailsWithGeoHashobj.mobileDetails.mobileStamp = fromNum+" "+ContactDetailsWithGeoHashobj.geoHash+" "+currentDT;
                     //   ContactDetailsWithGeoHashobj.mobileDetails.Contact = fromNum+" "+ContactDetailsWithGeoHashobj.geoHash+" "+currentDT;
                        ContactDetailsWithGeoHashobj.mobileDetails.Contact = fromNum;
                        if (prevgeoHash6 != null && prevgeoHash6.length() > 0) {
                            ContactDetailsWithGeoHashobj.prevgeoHash = prevgeoHash6.substring(0, 4);
                        }
                        pendingQueue.add(ContactDetailsWithGeoHashobj);
                        ContactDetailsWithGeoHash ContactDetailsWithGeoHashobj1 = new ContactDetailsWithGeoHash();
                        ContactDetailsWithGeoHashobj1.mobileDetails = new ContactDetails();
                        ContactDetailsWithGeoHashobj1.geoHash = geoHashF.substring(0, 4);
                        ContactDetailsWithGeoHashobj1.mobileDetails.mobileStamp = fromNum+" "+ContactDetailsWithGeoHashobj1.geoHash+" "+currentDT;
                      //  ContactDetailsWithGeoHashobj1.mobileDetails.Contact = fromNum+" "+ContactDetailsWithGeoHashobj1.geoHash+" "+currentDT;
                        ContactDetailsWithGeoHashobj1.mobileDetails.Contact = fromNum;
                        pendingQueue.add(ContactDetailsWithGeoHashobj1);

                        ContactDetailsWithGeoHash ContactDetailsWithGeoHashobj2 = new ContactDetailsWithGeoHash();
                        ContactDetailsWithGeoHashobj2.mobileDetails = new ContactDetails();
                        ContactDetailsWithGeoHashobj2.geoHash = geoHashF.substring(0, 5);
                        ContactDetailsWithGeoHashobj2.mobileDetails.mobileStamp = fromNum+" "+ContactDetailsWithGeoHashobj2.geoHash+" "+currentDT;
                     //   ContactDetailsWithGeoHashobj2.mobileDetails.Contact = fromNum+" "+ContactDetailsWithGeoHashobj2.geoHash+" "+currentDT;
                        ContactDetailsWithGeoHashobj2.mobileDetails.Contact = fromNum;
                        pendingQueue.add(ContactDetailsWithGeoHashobj2);



                        ContactDetailsWithGeoHash ContactDetailsWithGeoHashobj3 = new ContactDetailsWithGeoHash();
                        ContactDetailsWithGeoHashobj3.mobileDetails = new ContactDetails();
                        ContactDetailsWithGeoHashobj3.geoHash = geoHashF.substring(0, 6);
                        ContactDetailsWithGeoHashobj3.mobileDetails.mobileStamp = fromNum+" "+ContactDetailsWithGeoHashobj3.geoHash+" "+currentDT;
                      //  ContactDetailsWithGeoHashobj3.mobileDetails.Contact = fromNum+" "+ContactDetailsWithGeoHashobj3.geoHash+" "+currentDT;
                        ContactDetailsWithGeoHashobj3.mobileDetails.Contact = fromNum;
                        pendingQueue.add(ContactDetailsWithGeoHashobj3);

                    }
                } else if ("LOCATION".equals(action)) {
					List<String> usersList = obj.readFromFileinList();
					payload.put(MainClass.MESSAGE_KEY, "LOCATION");
					String location = payload.get("LOCATION");
					payload.put("LOCATION", location);
					for (int i = 0; i < usersList.size(); ++i) {
						payload.put("TOUSER", usersList.get(i));

						String message = MainClass.createJsonMessage(usersList.get(i), obj.getRandomMessageId(),
								payload, collapseKey, null, false);
						obj.send(message);
					}

				}else if ("ANN".equals(action)) {
				//	List<String> usersList = obj.readFromFileinList();
					AnnouncementGen tempAnnouncementObj = new AnnouncementGen();
					String geoHash3 = "";
					String prevgeoHash3 = "";
					String fromNum = "";
					String todayDate = "";
					String steTimeStamp = "";
					String strMsg = "";
					String cat = "";
					String imgReceived = "";
					String Name = "";
					String sex = "";

//					dataBundle.putString("ACTION", "ANN");
//					dataBundle.putString("DATE", todayDate);
//					dataBundle.putString("MSG", strMsg);
//					dataBundle.putString("TimeStamp", steTimeStamp);
//					dataBundle.putString("FROMUSER", fromNumber);


					geoHash3 = payload.get("GEOHASH_3Chars");
					prevgeoHash3 = payload.get("PREVGEOHASH_3Chars");
					fromNum = payload.get("FROMUSER");
					strMsg = payload.get("MSG");
					todayDate = payload.get("DATE");
					steTimeStamp = payload.get("TimeStamp");
					cat = payload.get("category");
					Name = payload.get("Name");
					if(payload.get("Sex") == "1")
					{
						sex = "Male";
					}
					else
					{
						sex = "female";
					}
					if(payload.containsKey("img"))
					 imgReceived = payload.get("img");

					tempAnnouncementObj.geoHash = geoHash3;
					tempAnnouncementObj.prevgeoHash = prevgeoHash3;
					tempAnnouncementObj.cat = cat;
					tempAnnouncementObj.date = todayDate;
					tempAnnouncementObj.timestamp = steTimeStamp;
					tempAnnouncementObj.msg = strMsg;
					tempAnnouncementObj.imgReceived = imgReceived;
					tempAnnouncementObj.mobileDetails = new ContactDetails();
					tempAnnouncementObj.mobileDetails.Contact = fromNum;
					tempAnnouncementObj.mobileDetails.sex = sex;
					tempAnnouncementObj.mobileDetails.Name = Name;

				//	tempAnnouncementObj.mobileDetails.Contact = fromNum;

					pendingAnnouncementQueue.add(tempAnnouncementObj);

				}
				else if ("PATHS".equals(action)) {
					List<String> usersList = obj.readFromFileinList();
					payload.put(MainClass.MESSAGE_KEY, "PATHS");
					String paths = payload.get("LINES");
					FromVsAddress obj = new FromVsAddress();
					obj.setFrom(from);
					obj.setAddress(paths);
					// obj.writeToFileAbtPath(from, paths);
					MainClass.address.add(obj);

				} else if ("LATLNG".equals(action)) {
					List<String> usersList = obj.readFromFileinList();
					payload.put(MainClass.MESSAGE_KEY, "LATLNG");
					String paths = payload.get("LATLNG");
					// String location2 = payload.get("LOCATION2");
					payload.put("LATLNG", paths);
					// payload.put("LOCATION2",location2);
					/*
					 * for (int i = 0; i < usersList.size(); ++i) { String str =
					 * usersList.get(i); payload.put("TOUSER",
					 * usersList.get(i)); ssssssss String message =
					 * MainClass.createJsonMessage(usersList.get(i),
					 * obj.getRandomMessageId(), payload, collapseKey, null,
					 * false); obj.send(message); }
					 */
				} else if ("ADDRESS".equals(action)) {
					List<String> paths = obj.readFromFileAbtPath();
					payload.put(MainClass.MESSAGE_KEY, "LATLNG");
					String pathId = payload.get("PATHID");

					// String location2 = payload.get("LOCATION2");
					// payload.put("LATLNG", paths);
					// payload.put("LOCATION2",location2);
					/*
					 * for (int i = 0; i < usersList.size(); ++i) { String str =
					 * usersList.get(i); payload.put("TOUSER",
					 * usersList.get(i));
					 * 
					 * String message =
					 * MainClass.createJsonMessage(usersList.get(i),
					 * obj.getRandomMessageId(), payload, collapseKey, null,
					 * false); obj.send(message); }
					 */
				} else if ("LOGIN".equals(action)) {
					Map<String, String> regIdMap = obj.readFromFile();
					Map<String, String> userIdMap = obj.readFromUserFile();
					String username = payload.get("USER");
					String password = payload.get("PASSWORD");
					String pwd = userIdMap.get(username);
					if (pwd != null && pwd.intern().equals(password) == true)
						payload.put("LOGIN", "SUCCESS");
					else
						payload.put("LOGIN", "FAIL");

					String message = MainClass.createJsonMessage(from, obj.getRandomMessageId(), payload, collapseKey,
							null, false);
					obj.send(message);

				} else if ("ADDCUSTOMER".equals(action)) {
					Map<String, String> regIdMap = obj.readFromFile();
					String toUser = payload.get("TOUSER");
					String number = payload.get("CONTACT");
					String toUserRegid = regIdMap.get(toUser);
					payload.put("TOUSER", toUser);
					String toStr = regIdMap.get(toUser);
					payload.put("CONTACT", number);
					if (toStr != null) {
						String message = MainClass.createJsonMessage(toStr, obj.getRandomMessageId(), payload,
								collapseKey, null, false);
						obj.send(message);
					}
				} else if ("ADDEVENT".equals(action)) {
					Map<String, String> regIdMap = obj.readFromFile();
					String toUser = payload.get("TOUSER");
					String event = payload.get("EVENT");
					payload.put("EVENT", event);
					String toStr = regIdMap.get(toUser);
					if (toStr != null) {
						String message = MainClass.createJsonMessage(toStr, obj.getRandomMessageId(), payload,
								collapseKey, null, false);
						obj.send(message);
					}
				} else if ("NEAREST".equals(action)) {
					Map<String, String> regIdMap = obj.readFromFile();
					String latitude = payload.get("latitude");
					String longitde = payload.get("longitude");

					String toStr = regIdMap.get("TOUSER");
					if (toStr != null) {
						String message = MainClass.createJsonMessage(toStr, obj.getRandomMessageId(), payload,
								collapseKey, null, false);
						obj.send(message);
					}
				} else if ("REGISTER".equals(action)) {
					Map<String, String> regIdMap = obj.readFromFile();
					Map<String, String> userIdMap = obj.readFromUserFile();

					String username = payload.get("USER");
					String password = payload.get("PASSWORD");
					String user = regIdMap.get(username);
					if (user == null) {
						try {
							obj.writeToFile(username, from);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try {
							obj.writeToFileUserId(username, password);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						payload.put("ERROR", "registration success");
						payload.put("Action", "AutoLogin");

					} else {
						payload.put("ERROR", "Try again");
						payload.put("Action", "Registration");
					}

					String message = MainClass.createJsonMessage(from, obj.getRandomMessageId(), payload, collapseKey,
							null, false);
					obj.send(message);

				}
			}
		}

	}

}
