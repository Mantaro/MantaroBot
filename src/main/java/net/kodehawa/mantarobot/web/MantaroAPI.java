package net.kodehawa.mantarobot.web;

import br.com.brjdevs.java.utils.async.Async;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.SentryHelper;
import okhttp3.*;
import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MantaroAPI {
    //The token we will use to interface with the API
    public static String sessionToken;
    public APIStatus STATUS = APIStatus.ON_HALT;
    //Node ID is assigned by the API after receiving a HEARTBEAT request from this specific node. If the node is already registered, the server
    //will retrieve the saved value from NODE_LIST. In case a NODE_UNKNOWN_RECEIVE is handled by the API, all node ids will be reassigned by the server
    //and sent back here.
    public int nodeId = 0;
    //Unique identifier of this specific node. Will be saved on NODE_LIST until the server sends a NODE_SHUTDOWN signal or 10 HEARTBEAT requests
    //time out. In the last case I should get notified that it did a boom and that I should start running in circles because the probability of me
    //being home when this happens is around zero.
    //Note: The number of random version 4 UUIDs *which need to be generated* in order to have a 50% probability of at least one collision is 2.71 quintillion
    //This ID is compared when doing node requests!
    public UUID nodeUniqueIdentifier = UUID.randomUUID();
    //The total number of nodes received. This should be received under request after initial setup.
    public int nodesTotal = 1;
    private long lastPing = 0;
    private OkHttpClient httpClient = new OkHttpClient();

    public void startService() {
        Runnable checker = () -> {
            try {
                STATUS = APIStatus.RECEIVING_DATA;
                long start = System.currentTimeMillis();
                Request r = new Request.Builder()
                        .url(String.format("http://%s/", MantaroData.config().get().apiUrl))
                        .build();

                httpClient.newCall(r).execute().close();
                long end = System.currentTimeMillis();
                lastPing = end - start;
                STATUS = APIStatus.ONLINE;

                RequestBody identifyBody = RequestBody.create(MediaType.parse("application/json"),
                        new JSONObject()
                                .put("nodeId", nodeId)
                                .put("nodeIdentifier", nodeUniqueIdentifier
                                ).toString());

                Request identify = new Request.Builder()
                        .url(String.format("http://%s/api/nodev1/identify", MantaroData.config().get().apiUrl))
                        .header("Authorization", sessionToken)
                        .post(identifyBody)
                        .build();
                httpClient.newCall(identify).execute().close();
            } catch(Exception e) {
                STATUS = APIStatus.OFFLINE;
            }
        };
        Async.task("Mantaro API checker", checker, 10, TimeUnit.SECONDS);
    }

    public boolean configure() {
        try {
            STATUS = APIStatus.INITIAL_SETUP;

            //get token
            Request tokenGet = new Request.Builder()
                    .url(String.format("http://%s/login", MantaroData.config().get().apiUrl))
                    .post(RequestBody.create(MediaType.parse("text/plain"), MantaroData.config().get().apiLoginCreds))
                    .build();

            Response response = httpClient.newCall(tokenGet).execute();
            sessionToken = new JSONObject(response.body().string()).getString("token");
            log.info("Logged in into the API!");
            response.close();

            long start = System.currentTimeMillis();
            //ping
            Request r = new Request.Builder()
                    .url(String.format("http://%s/", MantaroData.config().get().apiUrl))
                    .build();

            httpClient.newCall(r).execute().close();
            long end = System.currentTimeMillis();
            //pong, pls no lag.
            lastPing = end - start;
            System.out.println(sessionToken);
            Request nodeidr = new Request.Builder()
                    .url(String.format("http://%s/api/nodev1/next", MantaroData.config().get().apiUrl))
                    .header("Authorization", sessionToken)
                    .build();

            Response response1 = httpClient.newCall(nodeidr).execute();
            String reply = response1.body().string();
            System.out.println(reply);
            nodeId = new JSONObject(reply).getInt("id");
            response1.close();

            log.info("Received desired node id: {} ", nodeId);

            RequestBody identifyBody = RequestBody.create(MediaType.parse("application/json"),
                    new JSONObject()
                            .put("nodeId", nodeId)
                            .put("nodeIdentifier", nodeUniqueIdentifier
                            ).toString());

            Request identify = new Request.Builder()
                    .url(String.format("http://%s/api/nodev1/identify", MantaroData.config().get().apiUrl))
                    .header("Authorization", sessionToken)
                    .post(identifyBody)
                    .build();
            httpClient.newCall(identify).execute().close();

            STATUS = APIStatus.ONLINE;
            return true;
        } catch(Exception e) {
            //No need to set the status to OFFLINE since we already are gonna make the node exit.
            //Expecting maximum explosions at this point.
            SentryHelper.captureExceptionContext("Cannot contact Mantaro API. Startup will be cancelled", e, this.getClass(), "MAPI Configurer");
            e.printStackTrace();
            return false;
        }
    }

    public void getNodeTotal() {
        Runnable checker = () -> {
            try {
                Request nodeidr = new Request.Builder()
                        .url(String.format("http://%s/api/nodev1/next", MantaroData.config().get().apiUrl))
                        .header("Authorization", sessionToken)
                        .build();

                Response response = httpClient.newCall(nodeidr).execute();
                nodesTotal = new JSONObject(response.body().string()).getInt("id");
                response.close();
            } catch(Exception e) {
                e.printStackTrace();
                return;
            }
        };
        Async.task("Total node checker", checker, 5, TimeUnit.MINUTES);
    }

    public long getAPIPing() {
        return lastPing;
    }
}
