package net.kodehawa.mantarobot.web;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import java.util.UUID;

@Slf4j
public class MantaroAPI {
    private long lastPing = 0;
    public APIStatus STATUS = APIStatus.ONLINE;
    private OkHttpClient httpClient = new OkHttpClient();

    //Node ID is assigned by the API after receiving a HEARTBEAT request from this specific node. If the node is already registered, the server
    //will retrieve the saved value from NODE_LIST. In case a NODE_UNKNOWN_RECEIVE is handled by the API, all node ids will be reassigned by the server
    //and sent back here.
    public int nodeId = 0;


    //Unique identifier of this specific node. Will be saved on NODE_LIST until the server sends a NODE_SHUTDOWN signal or 10 HEARTBEAT requests
    //time out. In the last case I should get notified that it did a boom and that I should start running in circles because the probability of me
    //being home when this happens is around zero.
    //Note: The number of random version 4 UUIDs *which need to be generated* in order to have a 50% probability of at least one collision is 2.71 quintillion
    public UUID nodeUniqueIdentifier = UUID.randomUUID();

    //The total number of nodes received. This should be received under request after initial setup.
    public int nodesTotal = 1;

    public void startService(){
        /*Runnable checker = () -> {
            try{
                STATUS = APIStatus.RECEIVING_DATA;
                long start = System.currentTimeMillis();
                Unirest.get(String.format("http://%s/api/", MantaroData.config().get().apiUrl)).asString().getBody();
                long end = System.currentTimeMillis();
                lastPing = end - start;
                STATUS = APIStatus.ONLINE;

                Unirest.post(
                        String.format("http://%s/api/nodev1/heartbeat/", MantaroData.config().get().apiUrl))
                        .header("Content-Type", "application/json")
                        .body(new JSONObject()
                                .put("nodeId", nodeId)
                                .put("nodeIdentifier", nodeUniqueIdentifier
                                ))
                        .asString().getBody();
            } catch (UnirestException e){
                STATUS = APIStatus.OFFLINE;
            }
        };
        Async.task("Mantaro API heartbeat", checker, 15, TimeUnit.SECONDS);*/
    }

    public boolean configure(){
        /*try{
            //At this point, we're checking if we can initialize nodes and send the node data.
            STATUS = APIStatus.INITIAL_SETUP;
            long start = System.currentTimeMillis();
            //ping
            Unirest.get(String.format("http://%s/api/", MantaroData.config().get().apiUrl)).asString().getBody();
            long end = System.currentTimeMillis();
            //pong, pls no lag.
            lastPing = end - start;

            nodeId = Integer.parseInt(Unirest.get(String.format("http://%s/api/nodev1/nodeid/", MantaroData.config().get().apiUrl)).asString().getBody()) + 1;
            log.info("Received desired node id: {} ", nodeId);

            Unirest.post(
                    String.format("http://%s/api/nodev1/heartbeat/", MantaroData.config().get().apiUrl))
                    .header("Content-Type", "application/json")
                    .body(new JSONObject()
                            .put("nodeId", nodeId)
                            .put("nodeIdentifier", nodeUniqueIdentifier
                          ))
                    .asString();

            STATUS = APIStatus.ONLINE;

            return true;
        } catch (UnirestException e){
            //No need to set the status to OFFLINE since we already are gonna make the node exit.
            //Expecting maximum explosions at this point.
            SentryHelper.captureExceptionContext("Cannot contact Mantaro API. Startup will be cancelled", e, this.getClass(), "MAPI Configurer");
            e.printStackTrace();
            return false;
        }*/
        return true;
    }

    public void getNodeTotal(){
        /*Runnable checker = () -> {
            try{
                nodesTotal = Integer.parseInt(Unirest.get(String.format("http://%s/api/nodev1/nodeid", MantaroData.config().get().apiUrl)).asString().getBody());
            } catch (UnirestException e){
                e.printStackTrace();
                return;
            }
        };
        Async.task("Total node checker", checker, 5, TimeUnit.MINUTES);*/
    }

    public long getAPIPing() {
        return lastPing;
    }
}
