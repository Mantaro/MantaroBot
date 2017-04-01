package net.kodehawa.mantarobot.connectionwatcher;

import br.com.brjdevs.network.PacketRegistry;
import br.com.brjdevs.network.Server;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectionWatcher {
    public static final Logger LOGGER = LoggerFactory.getLogger("ConnectionWatcher");

    private static ConnectionWatcher instance;

    public final Server server;
    public final JDA jda;
    private final String consoleChannelId;
    private final List<String> jvmargs;
    private final List<String> owners;
    private int reboots = 0;
    private Process mantaroProcess;

    private ConnectionWatcher(String token, int port, String consoleChannelId, String prefix, List<String> jvmargs, List<String> owners) throws Exception {
        LOGGER.info("Starting connection watcher...");
        jda = new JDABuilder(AccountType.BOT)
                .setToken(token)
                .addListener(new CommandListener(prefix, owners))
                .setAudioEnabled(false)
                .buildAsync();
        this.jvmargs = jvmargs;
        this.owners = owners;
        this.consoleChannelId = consoleChannelId;
        if(port > 0) {
            PacketRegistry pr = new PacketRegistry();
            server = new Server(new InetSocketAddress(port), pr, new WebsocketMessageHandler());
            server.start();
        } else {
            server = null;
        }
    }

    public int getReboots() {
        return reboots;
    }

    public List<String> getOwners() {
        return new ArrayList<>(owners);
    }

    public List<String> getJvmArgs() {
        return new ArrayList<>(jvmargs);
    }

    public TextChannel getConsole() {
        return jda.getTextChannelById(consoleChannelId);
    }

    public void stopMantaro(boolean hardKill) {
        if(hardKill)
            mantaroProcess.destroyForcibly();
        else
            mantaroProcess.destroy();
        mantaroProcess = null;
    }

    public void launchMantaro(boolean hardKill) throws IOException {
        if(mantaroProcess != null) {
            LOGGER.info("Killing mantaro, forcibly: " + hardKill);
            stopMantaro(hardKill);
            reboots++;
        }
        LOGGER.info("Starting mantaro process");
        mantaroProcess = new ProcessBuilder()
                .command(jvmargs)
                .inheritIO()
                .start();
    }

    public static void main(String... args) throws Throwable {
        File config = new File("connectionwatcher.json");
        if(!config.exists()) {
            JSONObject obj = new JSONObject();
            obj.put("token", "token");
            obj.put("port", -1);
            obj.put("prefix", "~");
            obj.put("log", "id");
            obj.put("jvmargs", new JSONArray().put("-Xmx8G").put("-Xms128M").put("-Xmn256M"));
            obj.put("owners", new JSONArray());
            FileOutputStream fos = new FileOutputStream(config);
            ByteArrayInputStream bais = new ByteArrayInputStream(obj.toString(4).getBytes(Charset.defaultCharset()));
            byte[] buffer = new byte[1024];
            int read;
            while((read = bais.read(buffer)) != -1)
                fos.write(buffer, 0, read);
            fos.close();
            LOGGER.error("Could not find config file at " + config.getAbsolutePath() + ", creating a new one...");
            LOGGER.error("Generated new config file at " + config.getAbsolutePath() + ".");
            LOGGER.error("Please, fill the file with valid properties.");
            System.exit(-1);
        }
        JSONObject obj;
        {
            FileInputStream fis = new FileInputStream(config);
            byte[] buffer = new byte[1024];
            int read;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((read = fis.read(buffer)) != -1)
                baos.write(buffer, 0, read);
            obj = new JSONObject(new String(baos.toByteArray(), Charset.defaultCharset()));
        }
        String token = obj.getString("token");
        int port = obj.getInt("port");
        String consoleChannelId = obj.getString("log");
        String prefix = obj.getString("prefix");
        List<String> jvmargs = new ArrayList<>();
        jvmargs.add("java");
        obj.getJSONArray("jvmargs").forEach(a->jvmargs.add(String.valueOf(a)));
        List<String> owners = new ArrayList<>();
        obj.getJSONArray("owners").forEach(a->owners.add(String.valueOf(a)));
        instance = new ConnectionWatcher(token, port, consoleChannelId, prefix, jvmargs.stream().map(s->s.replace("%port%", String.valueOf(port))).collect(Collectors.toList()), owners);
    }

    public static ConnectionWatcher getInstance() {
        return instance;
    }
}
