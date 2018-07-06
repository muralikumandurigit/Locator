package LocatorServer;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class MainClass {

	static final String REG_ID_STORE = "usernameBased.txt";
	static final String REG_USER_STORE = "regIdBased.txt";
	static final String USER_ID_STORE = "user.txt";
	static final String PATH_ID_STORE = "pathBased.txt";
	public static ChatServer websocketConServer;
	public static OTPServer websocketOtpServer;
	public static WebSocket websocketOTPClient = null;

	static final String MESSAGE_KEY = "SM";
	Logger logger = Logger.getLogger("SmackCcsClient");

	public static final String GCM_SERVER = "gcm-xmpp.googleapis.com";
	public static final int GCM_PORT = 5235;

	public static final String GCM_ELEMENT_NAME = "gcm";
	public static final String GCM_NAMESPACE = "google:mobile:data";
	public static ConcurrentLinkedQueue<Map<String, Object>> receivedMsgs = new ConcurrentLinkedQueue<>();
	public static ConcurrentLinkedQueue<String> receivedTrackMsgs = new ConcurrentLinkedQueue<>();

	public static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> LastKnownLocs = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, WebSocket> currentAvailableConnections = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, WebSocket> currentRandomConnections = new ConcurrentHashMap<>();


    public static ConcurrentHashMap<WebSocket, String> currentAvailableConnectionsWebSocketAsKey = new ConcurrentHashMap<>();

	public static ConcurrentLinkedQueue<FromVsAddress> address = new ConcurrentLinkedQueue<>();
	public static ConcurrentLinkedQueue<String> requiredLocations = new ConcurrentLinkedQueue<>();
	Connection dbConnection;

	static Random random = new Random();
	public XMPPConnection connection;
	ConnectionConfiguration config;

	/**
	 * XMPP Packet Extension for GCM Cloud Connection Server.
	 */
	class GcmPacketExtension extends DefaultPacketExtension {
		String json;

		public GcmPacketExtension(String json) {
			super(GCM_ELEMENT_NAME, GCM_NAMESPACE);
			this.json = json;
		}

		public String getJson() {
			return json;
		}

		@Override
		public String toXML() {
			return String.format("<%s xmlns=\"%s\">%s</%s>", GCM_ELEMENT_NAME, GCM_NAMESPACE, json, GCM_ELEMENT_NAME);
		}

		public Packet toPacket() {
			return new Message() {
				// Must override toXML() because it includes a <body>
				@Override
				public String toXML() {

					StringBuilder buf = new StringBuilder();
					buf.append("<message");
					if (getXmlns() != null) {
						buf.append(" xmlns=\"").append(getXmlns()).append("\"");
					}
					if (getLanguage() != null) {
						buf.append(" xml:lang=\"").append(getLanguage()).append("\"");
					}
					if (getPacketID() != null) {
						buf.append(" id=\"").append(getPacketID()).append("\"");
					}
					if (getTo() != null) {
						buf.append(" to=\"").append(StringUtils.escapeForXML(getTo())).append("\"");
					}
					if (getFrom() != null) {
						buf.append(" from=\"").append(StringUtils.escapeForXML(getFrom())).append("\"");
					}
					buf.append(">");
					buf.append(GcmPacketExtension.this.toXML());
					buf.append("</message>");
					return buf.toString();
				}
			};
		}
	}

	public MainClass() {
		ProviderManager.getInstance().addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE,
				new PacketExtensionProvider() {

					@Override
					public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
						String json = parser.nextText();
						GcmPacketExtension packet = new GcmPacketExtension(json);
						return packet;
					}
				});
        try {
          //  WebSocketImpl.DEBUG = true;

            websocketConServer = new ChatServer(9090);
            websocketOtpServer = new OTPServer(8090);
            websocketConServer.start();
            websocketOtpServer.start();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        logger.log(Level.ALL,"Gopi");

	}

	/**
	 * Returns a random message id to uniquely identify a message.
	 *
	 * <p>
	 * Note: This is generated by a pseudo random number generator for
	 * illustration purpose, and is not guaranteed to be unique.
	 *
	 */
	public String getRandomMessageId() {
		return "m-" + Long.toString(random.nextLong());
	}

	/**
	 * Sends a downstream GCM message.
	 */
	public void send(String jsonRequest) {
	    try {
	        MYLOGGER.info("sending jsonRequest "+jsonRequest);
            Packet request = new GcmPacketExtension(jsonRequest).toPacket();
            if (connection != null && connection.isConnected() == true)
                connection.sendPacket(request);
        }
        catch(Exception ex)
        {
            int x = 200;
        }
	}

	/**
	 * Handles an upstream data message from a device application.
	 *
	 * <p>
	 * This sample echo server sends an echo message back to the device.
	 * Subclasses should override this method to process an upstream message.
	 */
	public void handleIncomingDataMessage(Map<String, Object> jsonObject) {
		receivedMsgs.add(jsonObject);

	}

	/**
	 * Handles an ACK.
	 *
	 * <p>
	 * By default, it only logs a INFO message, but subclasses could override it
	 * to properly handle ACKS.
	 */
	public void handleAckReceipt(Map<String, Object> jsonObject) {
		String messageId = jsonObject.get("message_id").toString();
		String from = jsonObject.get("from").toString();
		logger.log(Level.INFO, "handleAckReceipt() from: " + from + ", messageId: " + messageId);
	}

	/**
	 * Handles a NACK.
	 *
	 * <p>
	 * By default, it only logs a INFO message, but subclasses could override it
	 * to properly handle NACKS.
	 */
	public void handleNackReceipt(Map<String, Object> jsonObject) {
		String messageId = jsonObject.get("message_id").toString();
		String from = jsonObject.get("from").toString();
		logger.log(Level.INFO, "handleNackReceipt() from: " + from + ", messageId: " + messageId);
	}

	/**
	 * Creates a JSON encoded GCM message.
	 *
	 * @param to
	 *            RegistrationId of the target device (Required).
	 * @param messageId
	 *            Unique messageId for which CCS will send an "ack/nack"
	 *            (Required).
	 * @param payload
	 *            Message content intended for the application. (Optional).
	 * @param collapseKey
	 *            GCM collapse_key parameter (Optional).
	 * @param timeToLive
	 *            GCM time_to_live parameter (Optional).
	 * @param delayWhileIdle
	 *            GCM delay_while_idle parameter (Optional).
	 * @return JSON encoded GCM message.
	 */
	public static String createJsonMessage(String to, String messageId, Map<String, String> payload, String collapseKey,
			Long timeToLive, Boolean delayWhileIdle) {
		Map<String, Object> message = new HashMap<String, Object>();
		message.put("to", to);
		if (collapseKey != null) {
			message.put("collapse_key", collapseKey);
		}
		if (timeToLive != null) {
			message.put("time_to_live", timeToLive);
		}
		if (delayWhileIdle != null && delayWhileIdle) {
			message.put("delay_while_idle", true);
		}
		message.put("message_id", messageId);
		message.put("data", payload);
		return JSONValue.toJSONString(message);
	}

	/**
	 * Creates a JSON encoded ACK message for an upstream message received from
	 * an application.
	 *
	 * @param to
	 *            RegistrationId of the device who sent the upstream message.
	 * @param messageId
	 *            messageId of the upstream message to be acknowledged to CCS.
	 * @return JSON encoded ack.
	 */
	public static String createJsonAck(String to, String messageId) {
		Map<String, Object> message = new HashMap<String, Object>();
		message.put("message_type", "ack");
		message.put("to", to);
		message.put("message_id", messageId);
		return JSONValue.toJSONString(message);
	}

	/**
	 * Connects to GCM Cloud Connection Server using the supplied credentials.
	 *
	 * @param username
	 *            GCM_SENDER_ID@gcm.googleapis.com
	 * @param password
	 *            API Key
	 * @throws XMPPException
	 */
	public void connect(String username, String password) throws XMPPException {
		config = new ConnectionConfiguration(GCM_SERVER, GCM_PORT);
		config.setSecurityMode(SecurityMode.enabled);
		config.setReconnectionAllowed(true);
		config.setRosterLoadedAtLogin(false);
		config.setSendPresence(false);
		config.setSocketFactory(SSLSocketFactory.getDefault());

		// NOTE: Set to true to launch a window with information about packets
		// sent and received
		config.setDebuggerEnabled(true);

		// -Dsmack.debugEnabled=true
		XMPPConnection.DEBUG_ENABLED = true;

		connection = new XMPPConnection(config);
		connection.connect();

		connection.addConnectionListener(new ConnectionListener() {

			@Override
			public void reconnectionSuccessful() {
				logger.info("Reconnecting..");
			}

			@Override
			public void reconnectionFailed(Exception e) {
				logger.log(Level.INFO, "Reconnection failed.. ", e);
			}

			@Override
			public void reconnectingIn(int seconds) {
				logger.log(Level.INFO, "Reconnecting in %d secs", seconds);
			}

			@Override
			public void connectionClosedOnError(Exception e) {
				logger.log(Level.INFO, "Connection closed on error.");
			}

			@Override
			public void connectionClosed() {
				logger.info("Connection closed.");
			}
		});

		// Handle incoming packets
		connection.addPacketListener(new PacketListener() {

			@Override
			public void processPacket(Packet packet) {
				logger.log(Level.INFO, "Received: " + packet.toXML());
				Message incomingMessage = (Message) packet;
				GcmPacketExtension gcmPacket = (GcmPacketExtension) incomingMessage.getExtension(GCM_NAMESPACE);
				String json = gcmPacket.getJson();
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> jsonObject = (Map<String, Object>) JSONValue.parseWithException(json);

					// present for "ack"/"nack", null otherwise
					Object messageType = jsonObject.get("message_type");

					if (messageType == null) {
						// Normal upstream data message
						handleIncomingDataMessage(jsonObject);

						// Send ACK to CCS
						String messageId = jsonObject.get("message_id").toString();
						String from = jsonObject.get("from").toString();
						String ack = createJsonAck(from, messageId);
						send(ack);
					} else if ("ack".equals(messageType.toString())) {
						// Process Ack
						handleAckReceipt(jsonObject);
					} else if ("nack".equals(messageType.toString())) {
						// Process Nack
						handleNackReceipt(jsonObject);
					} else {
						logger.log(Level.WARNING, "Unrecognized message type (%s)", messageType.toString());
					}
				} catch (ParseException e) {
					logger.log(Level.SEVERE, "Error parsing JSON " + json, e);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Couldn't send echo.", e);
				}
			}
		}, new PacketTypeFilter(Message.class));

		// Log all outgoing packets
		connection.addPacketInterceptor(new PacketInterceptor() {
			@Override
			public void interceptPacket(Packet packet) {
				logger.log(Level.INFO, "Sent: {0}", packet.toXML());
			}
		}, new PacketTypeFilter(Message.class));

		connection.login(username, password);
	}

	public void writeToFile(String name, String regId) throws IOException {
		Map<String, String> regIdMap = readFromFile();
		regIdMap.put(name, regId);
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(REG_ID_STORE, false)));
		for (Map.Entry<String, String> entry : regIdMap.entrySet()) {
			out.println(entry.getKey() + "," + entry.getValue());
		}
		out.println(name + "," + regId);
		out.close();

	}

	public void writeToFileAbtPath(String fromLoc) throws IOException {
		List<String> regIdMap = readFromFileAbtPath();

		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(PATH_ID_STORE, false)));
		for (int i = 0; i < regIdMap.size(); ++i) {
			out.println(regIdMap.get(i));
		}
		out.println(fromLoc);
		out.close();

	}

	public void writeToFileInReverse(String name, String regId) throws IOException {
		Map<String, String> regIdMap = readFromFileInReverse();
		regIdMap.put(regId, name);
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(REG_USER_STORE, false)));
		for (Map.Entry<String, String> entry : regIdMap.entrySet()) {
			out.println(entry.getKey() + "," + entry.getValue());
		}
		out.println(name + "," + regId);
		out.close();

	}

	public void writeToFileUserId(String name, String regId) throws IOException {
		Map<String, String> regIdMap = readFromFile();
		regIdMap.put(name, regId);
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(USER_ID_STORE, false)));
		for (Map.Entry<String, String> entry : regIdMap.entrySet()) {
			out.println(entry.getKey() + "," + entry.getValue());
		}
		out.println(name + "," + regId);
		out.close();

	}

	public void createTable(String table, String field, String fieldType, Integer fieldSize) {
		Statement stmt = null;
		Boolean tableExist = false;
		try {
			stmt = dbConnection.createStatement();

			String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "';";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				tableExist = true;
			}
			if (tableExist == false) {

			}
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void createTables() {

		try {
			String table = "UserTable";
			String field = "Number";
			String fieldType = "String";
			Integer fieldSize = 15;
			createTable(table, field, fieldType, fieldSize);

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Table created successfully");
	}

	public void getDatabase() {

		try {

			Class.forName("org.sqlite.JDBC");
			dbConnection = DriverManager.getConnection("jdbc:sqlite:tracker.db");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	public Map<String, String> readFromFile() {
		Map<String, String> regIdMap = null;
		BufferedReader br;
		try {
			try {
				br = new BufferedReader(new FileReader(REG_ID_STORE));
			} catch (IOException ex) {
				new FileWriter(REG_ID_STORE);
				br = new BufferedReader(new FileReader(REG_ID_STORE));

			}
			String regIdLine = "";
			regIdMap = new HashMap<String, String>();
			while ((regIdLine = br.readLine()) != null) {
				String[] regArr = regIdLine.split(",");
				regIdMap.put(regArr[0], regArr[1]);
			}
			br.close();
		} catch (IOException ioe) {
		}
		return regIdMap;
	}

	public List<String> readFromFileAbtPath() {
		Map<String, String> regIdMap = null;
		List<String> paths = new ArrayList<>();
		BufferedReader br;
		try {
			try {
				br = new BufferedReader(new FileReader(PATH_ID_STORE));
			} catch (IOException ex) {
				new FileWriter(PATH_ID_STORE);
				br = new BufferedReader(new FileReader(PATH_ID_STORE));

			}
			String regIdLine = "";

			while ((regIdLine = br.readLine()) != null) {
				paths.add(regIdLine);
			}
			br.close();
		} catch (IOException ioe) {
		}
		return paths;
	}

	public Map<String, String> readFromFileInReverse() {
		Map<String, String> regIdMap = null;
		BufferedReader br;
		try {
			try {
				br = new BufferedReader(new FileReader(REG_USER_STORE));
			} catch (IOException ex) {
				new FileWriter(REG_USER_STORE);
				br = new BufferedReader(new FileReader(REG_USER_STORE));

			}
			String regIdLine = "";
			regIdMap = new HashMap<String, String>();
			while ((regIdLine = br.readLine()) != null) {
				String[] regArr = regIdLine.split(",");
				regIdMap.put(regArr[0], regArr[1]);
			}
			br.close();
		} catch (IOException ioe) {
		}
		return regIdMap;
	}

	public List<String> readFromFileinList() {
		List<String> regIdList = null;
		BufferedReader br;
		try {
			try {
				br = new BufferedReader(new FileReader(REG_ID_STORE));
			} catch (IOException ex) {
				new FileWriter(REG_ID_STORE);
				br = new BufferedReader(new FileReader(REG_ID_STORE));

			}
			String regIdLine = "";
			regIdList = new ArrayList<String>();
			while ((regIdLine = br.readLine()) != null) {
				String[] regArr = regIdLine.split(",");
				regIdList.add(regArr[1]);

			}
			br.close();
		} catch (IOException ioe) {
		}
		return regIdList;
	}

	@SuppressWarnings("resource")
	public Map<String, String> readFromUserFile() {
		Map<String, String> userIdMap = null;
		BufferedReader br;
		try {
			try {
				br = new BufferedReader(new FileReader(USER_ID_STORE));
			} catch (IOException ex) {
				new FileWriter(USER_ID_STORE);
				br = new BufferedReader(new FileReader(USER_ID_STORE));

			}
			String regIdLine = "";
			userIdMap = new HashMap<String, String>();
			while ((regIdLine = br.readLine()) != null) {
				String[] regArr = regIdLine.split(",");
				userIdMap.put(regArr[0], regArr[1]);
			}
			br.close();
		} catch (IOException ioe) {
		}
		return userIdMap;
	}
    static MyLogger locatorlogger;

    static {
        try {
            MyLogger.setup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final static Logger MYLOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public final static Logger MYLOGGERSOCKET = Logger.getLogger("SOCKETLOGGER");

    public static void main(String[] args) {
		final String userName = "452702272552" + "@gcm.googleapis.com";
		final String password = "AIzaSyA_svGFYF9wMgG74CL1Z6Y6RELlwrL6N2A";

		MainClass ccsClient = new MainClass();
		Thread msgHandlerThread;
		Thread addrWriter;
		Thread trackMsgs;
		Thread trackedMsgsPusherHandler;
		Thread subscriberThreadHandler;
		Thread announcementThreadHandler;
		MYLOGGER.info("Gopi");

		try {
			ccsClient.connect(userName, password);
			MYLOGGER.info("connected to google server");
            msgHandlerThread = new Thread(new MsgHandler(ccsClient));
            MYLOGGER.info("Initialised Msg handler thread");
            addrWriter = new Thread(new AddressWriter(ccsClient));
            trackMsgs = new Thread(new TrackMsgsPusherThread());
            announcementThreadHandler = new Thread(new AnnouncementsThread(ccsClient));
            trackedMsgsPusherHandler = new Thread(new TrackedMsgHandler(ccsClient));
            subscriberThreadHandler = new Thread(new SubscriberThread(ccsClient));
            msgHandlerThread.start(); // in background thread
            addrWriter.start();
            trackMsgs.start();
            trackedMsgsPusherHandler.start();
            subscriberThreadHandler.start();
            announcementThreadHandler.start();
            MYLOGGER.info("Started all required threads");
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}

}
