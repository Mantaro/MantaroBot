package net.kodehawa.mantarobot.utils.rmq;

import bsh.Interpreter;
import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.dv8tion.jda.core.JDA;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroShard;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.data.DataManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static net.kodehawa.mantarobot.utils.ShutdownCodes.RABBITMQ_FAILURE;
import static net.kodehawa.mantarobot.utils.ShutdownCodes.REMOTE_SHUTDOWN;
import static net.kodehawa.mantarobot.utils.rmq.ReturnCodes.*;

public class RabbitMQDataManager implements DataManager<JSONObject> {

    @Getter
    private Connection rMQConnection;
    @Getter
    public Channel mainrMQChannel;

    private final static String MAIN_QUEUE_NAME = "mantaro-nodes";
    private final static String API_QUEUE_NAME = "mantaro-api-node_" + MantaroBot.getInstance().getMantaroAPI().nodeId;
    @Setter @Getter private JSONObject lastReceivedPayload = new JSONObject(); //{} if none
    @Setter @Getter public int apiCalls;
    @Setter @Getter public int nodeCalls;

    @SneakyThrows
    public RabbitMQDataManager(Config config) {
        if(config.isBeta) return;

        Channel channel = getMainrMQChannel();

        try{
            ConnectionFactory factory = new ConnectionFactory();

            factory.setHost(config.rMQIP);
            factory.setUsername(config.rMQUser);
            factory.setPassword(config.rMQPassword);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setConnectionTimeout(1000);

            rMQConnection = factory.newConnection();
            mainrMQChannel = rMQConnection.createChannel(1);
        } catch (IOException | TimeoutException e){
            SentryHelper.captureException("Something went horribly wrong while setting up the RabbitMQ connection", e, this.getClass());
            System.exit(RABBITMQ_FAILURE);
        }

        Channel apiChannel = rMQConnection.createChannel(2);

        //--------------------------------  MAIN QUEUE DECLARATION ---------------------------------------------

        channel.exchangeDeclare("topic_action", "topic");
        channel.queueDeclare(MAIN_QUEUE_NAME, false, false, false, null);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                JSONObject payload = new JSONObject(message);
                ReturnCodes code = SUCCESS;
                UUID payloadIdentifier = UUID.randomUUID();
                apiCalls++;

                if (payload.has("process_image")){
                    //soon (tm)
                    //This should, and *should* load-balance itself between nodes.
                } else if (payload.has("eval_query")){
                    String toEval = payload.getString("eval_query");
                    Interpreter interpreter = new Interpreter();
                    try {
                        interpreter.set("mantaro", MantaroBot.getInstance());
                        interpreter.set("db", MantaroData.db());

                        interpreter.eval(String.join(
                                "\n",
                                "import *;",
                                toEval
                        ));
                    } catch (Exception e) {

                    }


                } else {
                    code = NOT_VALID;
                }

                if(code != SUCCESS){
                    SentryHelper.breadcrumb("Failed to process MAIN payload! Reason: " + code + " | With payload: " + payload);
                }


                setLastReceivedPayload(payload);
            }
        };

        channel.basicConsume(MAIN_QUEUE_NAME, true, consumer);

        //--------------------------------  API QUEUE DECLARATION ---------------------------------------------

        apiChannel.exchangeDeclare("topic_api", "topic");
        apiChannel.queueDeclare(API_QUEUE_NAME, true, false, false, null);

        Consumer apiConsumer = new DefaultConsumer(apiChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                JSONObject payload = new JSONObject(message);
                ReturnCodes code = SUCCESS;
                UUID payloadIdentifier = UUID.randomUUID();
                int nodeId = MantaroBot.getInstance().getMantaroAPI().nodeId;
                apiCalls++;

                if(payload.has("action")){
                    switch (NodeAction.valueOf(payload.getString("action"))){
                        case SHUTDOWN:
                            MantaroBot.getInstance().getAudioManager().getMusicManagers().forEach((s, musicManager) -> {
                                if (musicManager.getTrackScheduler() != null)
                                    musicManager.getTrackScheduler().stop();
                            });

                            Arrays.stream(MantaroBot.getInstance().getShards()).forEach(MantaroShard::prepareShutdown);
                            Arrays.stream(MantaroBot.getInstance().getShards()).forEach(mantaroShard -> mantaroShard.getJDA().shutdown(true));

                            JSONObject shutdownPayload = new JSONObject();
                            shutdownPayload.put("shutdown", true);
                            shutdownPayload.put("broadcast", true);

                            apiChannel.basicPublish("", API_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, shutdownPayload.toString().getBytes());

                            SentryHelper.breadcrumb("Shutting down node #" + nodeId + "remotely...");

                            try{
                                apiChannel.close();
                                rMQConnection.close();
                                channel.close();
                            } catch (Exception e){}

                            System.exit(REMOTE_SHUTDOWN);

                            break;

                        case RESTART:

                            boolean hardkill = false;

                            if(payload.has("hardkill")){
                                hardkill = payload.getBoolean("hardkill");
                            }

                            channel.basicPublish("", MAIN_QUEUE_NAME, null, createSuccessOutput(
                                    String.format("Attempt to restart node no.%d from API call: %d, %s", nodeId, apiCalls, payloadIdentifier),
                                    true
                            ));

                            try{
                                MantaroBot.getConnectionWatcher().reboot(hardkill);
                            } catch (Exception e){
                                code = CODE_FAILURE;
                            }
                            break;

                        case RESTART_SHARD:
                            if(payload.has("shard_id")){
                                int shardId = payload.getInt("shard_id");
                                boolean force = true;
                                if(payload.has("force")){
                                    force = payload.getBoolean("force");
                                }

                                try {
                                    MantaroBot.getInstance().getShardList().get(shardId).restartJDA(force);

                                    channel.basicPublish("", MAIN_QUEUE_NAME, null, createSuccessOutput(
                                            String.format("Restarted shard no.%d from API call: %d, %s", shardId, apiCalls, payloadIdentifier),
                                            true
                                    ));

                                    StringBuilder builder = new StringBuilder();
                                    for (MantaroShard shard : MantaroBot.getInstance().getShardList()) {
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

                                    channel.basicPublish("", MAIN_QUEUE_NAME, null, formattedShardObject.toString().getBytes());
                                } catch (Exception e) {
                                    SentryHelper.captureExceptionContext(
                                            String.format("Cannot restart shard no.%d from API call: %d, %s (at %d)",
                                                    shardId, apiCalls, payloadIdentifier, System.currentTimeMillis()),
                                            e, this.getClass(),
                                            "Restart worker");

                                    channel.basicPublish("", MAIN_QUEUE_NAME, null, createFailureOutput(
                                            "restart", "warning",
                                            String.format("Cannot restart shard %d from API call: %d, %s", shardId, apiCalls, payloadIdentifier),
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
                } else if(payload.has("get_node")){
                    //TODO: get stuff from the node, broadcast to guilds here.
                } else {
                    code = NOT_VALID;
                }

                if(code != SUCCESS){
                    SentryHelper.breadcrumb("Failed to process NODE payload! Reason: " + code + " | With payload: " + payload);
                }

                setLastReceivedPayload(payload);
            }
        };

        apiChannel.basicConsume(API_QUEUE_NAME, true, apiConsumer);
    }

    @Override
    public void save() {}

    @Override
    public JSONObject get() {
        return lastReceivedPayload;
    }

    public byte[] createFailureOutput(String failureType, String errorType, String message, boolean broadcast){
        JSONObject failure = new JSONObject();
        failure.put("failure", failureType);
        failure.put("type", errorType);
        failure.put("message", message);
        failure.put("broadcast", broadcast);

        return failure.toString().getBytes();
    }

    public byte[] createSuccessOutput(String message, boolean broadcast){
        JSONObject success = new JSONObject();
        success.put("success", "");
        success.put("message", message);
        success.put("broadcast", broadcast);

        return success.toString().getBytes();
    }
}
