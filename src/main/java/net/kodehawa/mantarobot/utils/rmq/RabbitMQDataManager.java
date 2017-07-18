package net.kodehawa.mantarobot.utils.rmq;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.shard.MantaroShard;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.data.DataManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.RABBITMQ_FAILURE;
import static net.kodehawa.mantarobot.utils.ShutdownCodes.REMOTE_SHUTDOWN;
import static net.kodehawa.mantarobot.utils.rmq.ReturnCodes.*;

@Slf4j
public class RabbitMQDataManager implements DataManager<JSONObject> {

    private final static String MAIN_QUEUE_NAME = "mantaro-nodes";
    private final static String INFO_QUEUE_NAME = "mantaro-info";
    private final static String API_QUEUE_NAME = "mantaro-api-noder";
    @Getter
    public Channel mainrMQChannel;
    @Getter
    public Channel apirMQChannel;
    @Setter
    @Getter
    public int apiCalls;
    @Setter
    @Getter
    public int nodeCalls;
    @Getter
    private Connection rMQConnection;
    @Setter
    @Getter
    private JSONObject lastReceivedPayload = new JSONObject(); //{} if none

    @SneakyThrows
    public RabbitMQDataManager(Config config) {
        if(config.isBeta || config.isPremiumBot) return;

        try {
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost(config.rMQIP);
            factory.setUsername(config.rMQUser);
            factory.setPassword(config.rMQPassword);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setConnectionTimeout(1000);

            rMQConnection = factory.newConnection();
            log.info("Created RabbitMQ connection with properties: " + factory.getClientProperties());
            mainrMQChannel = rMQConnection.createChannel();
            log.info("Acknowledged #" + mainrMQChannel.getChannelNumber() + " on queue: " + MAIN_QUEUE_NAME);
        } catch(IOException | TimeoutException e) {
            SentryHelper.captureException("Something went horribly wrong while setting up the RabbitMQ connection", e, this.getClass());
            System.exit(RABBITMQ_FAILURE);
        }

        Channel apiChannel = rMQConnection.createChannel();
        log.info("Acknowledged #" + apiChannel.getChannelNumber() + " on queue: " + API_QUEUE_NAME);

        Channel infoChannel = rMQConnection.createChannel();
        log.info("Acknowledged #" + infoChannel.getChannelNumber() + " on queue: " + INFO_QUEUE_NAME);

        //--------------------------------  MAIN QUEUE DECLARATION ---------------------------------------------

        mainrMQChannel.exchangeDeclare("topic_action", "topic");
        mainrMQChannel.queueDeclare(MAIN_QUEUE_NAME, false, false, false, null);

        Consumer consumer = new DefaultConsumer(mainrMQChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                JSONObject payload = new JSONObject(message);
                ReturnCodes code = SUCCESS;
                apiCalls++;

                setLastReceivedPayload(payload);
            }
        };

        mainrMQChannel.basicConsume(MAIN_QUEUE_NAME, true, consumer);

        //--------------------------------  API QUEUE DECLARATION ---------------------------------------------

        apiChannel.exchangeDeclare("topic_api", "topic");
        apiChannel.queueDeclare(API_QUEUE_NAME, false, false, false, null);

        apirMQChannel = apiChannel;
        Consumer apiConsumer = new DefaultConsumer(apiChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                JSONObject payload = new JSONObject(message);
                ReturnCodes code = SUCCESS;
                int nodeId = MantaroBot.getInstance().getMantaroAPI().nodeId;
                apiCalls++;

                if(payload.has("action")) {
                    if(payload.has("node_identifier")) {
                        if(!payload.getString("node_identifier").equals(MantaroBot.getInstance().getMantaroAPI().nodeUniqueIdentifier.toString())) {
                            System.out.println("Received a request but the identfier doesn't match... maybe for another node?");
                            return;
                        }
                    }

                    switch(NodeAction.valueOf(payload.getString("action"))) {

                        case SHUTDOWN:
                            //TODO re-enable
                            /*MantaroBot.getInstance().getAudioManager().getMusicManagers().forEach((s, musicManager) -> {
                                if (musicManager.getTrackScheduler() != null)
                                    musicManager.getTrackScheduler().stop();
                            });*/

                            Arrays.stream(MantaroBot.getInstance().getShardedMantaro().getShards()).forEach(MantaroShard::prepareShutdown);
                            Arrays.stream(MantaroBot.getInstance().getShardedMantaro().getShards()).forEach(mantaroShard -> mantaroShard.getJDA().shutdownNow());

                            JSONObject shutdownPayload = new JSONObject();
                            shutdownPayload.put("shutdown", true);
                            shutdownPayload.put("broadcast", true);

                            apiChannel.basicPublish("", API_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, shutdownPayload.toString().getBytes());

                            SentryHelper.breadcrumb("Shutting down node #" + nodeId + "remotely...");

                            try {
                                apiChannel.close();
                                rMQConnection.close();
                                mainrMQChannel.close();
                            } catch(Exception e) {
                            }

                            System.exit(REMOTE_SHUTDOWN);

                            break;

                        case RESTART:
                            boolean hardkill = false;

                            if(payload.has("hardkill")) {
                                hardkill = payload.getBoolean("hardkill");
                            }

                            mainrMQChannel.basicPublish("", MAIN_QUEUE_NAME, null, createSuccessOutput(
                                    String.format("Attempt to restart node no.%d from API call: %d", nodeId, apiCalls),
                                    true
                            ));

                            try {
                                MantaroBot.getConnectionWatcher().reboot(hardkill);
                            } catch(Exception e) {
                                code = CODE_FAILURE;
                            }
                            break;

                        case RESTART_SHARD:
                            if(payload.has("shard_id")) {
                                int shardId = payload.getInt("shard_id");
                                boolean force = true;
                                if(payload.has("force")) {
                                    force = payload.getBoolean("force");
                                }

                                try {
                                    MantaroBot.getInstance().getShardList().get(shardId).restartJDA(force);

                                    mainrMQChannel.basicPublish("", MAIN_QUEUE_NAME, null, createSuccessOutput(
                                            String.format("Restarted shard no.%d from API call: %d", shardId, apiCalls),
                                            true
                                    ));

                                    StringBuilder builder = new StringBuilder();
                                    for(MantaroShard shard : MantaroBot.getInstance().getShardList()) {
                                        JDA jda = shard.getJDA();
                                        builder.append(String.format(
                                                "%-15s" + " | STATUS: %-9s" + " | U: %-5d" + " | G: %-4d" + " | L: %-7s" + " | MC: %-2d",
                                                jda.getShardInfo() == null ? "Shard [0 / 1]" : jda.getShardInfo(),
                                                jda.getStatus(),
                                                jda.getUsers().size(),
                                                jda.getGuilds().size(),
                                                shard.getEventManager().getLastJDAEventTimeDiff() + " ms",
                                                jda.getVoiceChannels().stream().filter(voiceChannel -> voiceChannel.getMembers().contains(voiceChannel.getGuild().getSelfMember())).count()
                                        ));
                                    }

                                    JSONObject formattedShardObject = new JSONObject();
                                    formattedShardObject.put("node", nodeId);
                                    formattedShardObject.put("current_node_shards_formatted", builder.toString());
                                    formattedShardObject.put("broadcast", false);

                                    mainrMQChannel.basicPublish("", MAIN_QUEUE_NAME, null, formattedShardObject.toString().getBytes());
                                } catch(Exception e) {
                                    SentryHelper.captureExceptionContext(
                                            String.format("Cannot restart shard no.%d from API call: %d (at %d)",
                                                    shardId, apiCalls, System.currentTimeMillis()),
                                            e, this.getClass(),
                                            "Restart worker");

                                    mainrMQChannel.basicPublish("", MAIN_QUEUE_NAME, null, createFailureOutput(
                                            "restart", "warning",
                                            String.format("Cannot restart shard %d from API call: %d", shardId, apiCalls),
                                            true
                                    ));

                                    code = CODE_FAILURE;
                                }
                            } else {
                                code = MISSING_OBJECT;
                            }
                            break;

                        case ROLLING_RESTART:
                            break;
                    }
                } else if(payload.has("get_node")) {
                    //TODO: get stuff from the node, broadcast to guilds here.
                } else {
                    code = NOT_VALID;
                }

                if(code != SUCCESS) {
                    SentryHelper.breadcrumb("Failed to process NODE payload! Reason: " + code + " | With payload: " + payload);
                }

                setLastReceivedPayload(payload);
            }
        };

        apiChannel.basicConsume(API_QUEUE_NAME, true, apiConsumer);
    }

    @Override
    public void save() {
    }

    @Override
    public JSONObject get() {
        return lastReceivedPayload;
    }

    public byte[] createFailureOutput(String failureType, String errorType, String message, boolean broadcast) {
        JSONObject failure = new JSONObject();
        failure.put("failure", failureType);
        failure.put("type", errorType);
        failure.put("message", message);
        failure.put("broadcast", broadcast);

        return failure.toString().getBytes();
    }

    public byte[] createSuccessOutput(String message, boolean broadcast) {
        JSONObject success = new JSONObject();
        success.put("success", "");
        success.put("message", message);
        success.put("broadcast", broadcast);

        return success.toString().getBytes();
    }
}
