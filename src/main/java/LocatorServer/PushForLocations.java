package LocatorServer;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

public class PushForLocations {
    public Map<String,String> data;
    public int time_to_live;
    public String from;
    public String message_id;
    public String category;
}
