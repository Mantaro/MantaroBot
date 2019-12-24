/*
 * Copyright (C) 2016-2019 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.shard;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.prometheus.client.Gauge;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.DirectAudioController;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.ratelimit.IBucket;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.music.listener.VoiceChannelListener;
import net.kodehawa.mantarobot.commands.utils.birthday.BirthdayTask;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.MantaroListener;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.listeners.entities.CachedMessage;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.Prometheus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.kodehawa.mantarobot.data.MantaroData.config;
import static net.kodehawa.mantarobot.utils.Utils.httpClient;
import static net.kodehawa.mantarobot.utils.Utils.pretty;

/**
 * Represents a Discord shard.
 * This class and contains all the logic necessary to build, start and configure shards.
 * The logic for configuring sharded instances of the bot is on {@link net.kodehawa.mantarobot.core.MantaroCore}.
 * <p>
 * This also handles posting stats to dbots/dbots.org/carbonitex. Because uh... no other class was fit for it.
 */
public class MantaroShard implements JDA {
    public static final Function<JDA, Integer> QUEUE_SIZE = jda -> {
        int sum = 0;
        for(final IBucket bucket : ((JDAImpl) jda).getRequester().getRateLimiter().getRouteBuckets()) {
            sum += bucket.getRequests().size();
        }
        
        return sum;
    };
    public static final BiFunction<JDA, String, Integer> ROUTE_QUEUE_SIZE = (jda, route) -> {
        for(final IBucket bucket : ((JDAImpl) jda).getRequester().getRateLimiter().getRouteBuckets()) {
            if(bucket.getRoute().equals(route))
                return bucket.getRequests().size();
        }
        
        return 0;
    };
    public static final Function<JDA, List<Pair<String, Integer>>> GET_BUCKETS_WITH_QUEUE = jda -> {
        final List<Pair<String, Integer>> routes = new ArrayList<>();
        final List<IBucket> buckets = ((JDAImpl) jda).getRequester().getRateLimiter().getRouteBuckets();
        for(final IBucket bucket : buckets) {
            if(bucket.getRequests().size() > 0) {
                routes.add(Pair.of(bucket.getRoute(), bucket.getRequests().size()));
            }
        }
        
        return routes;
    };
    private static final VoiceChannelListener VOICE_CHANNEL_LISTENER = new VoiceChannelListener();
    private static final Config config = MantaroData.config().get();
    //Christmas date
    private static final Calendar christmas = new Calendar.Builder().setDate(Calendar.getInstance().get(Calendar.YEAR), Calendar.DECEMBER, 25).build();
    //New year date
    private static final Calendar newYear = new Calendar.Builder().setDate(Calendar.getInstance().get(Calendar.YEAR), Calendar.JANUARY, 1).build();
    private static final Gauge ratelimitBucket = new Gauge.Builder().name("ratelimitBucket").help("shard queue size").labelNames("shardId").create();
    public final MantaroEventManager manager;
    private final Logger log;
    private final CommandListener commandListener;
    private final MantaroListener mantaroListener;
    private final int shardId;
    private final int totalShards;
    //Message cache of 2500 cached messages per shard. If it reaches 2500 it will delete the first one stored, and continue being 2500.
    private final Cache<String, Optional<CachedMessage>> messageCache = CacheBuilder.newBuilder().concurrencyLevel(5).maximumSize(2500).build();
    private final String callbackPoolIdentifierString;
    private final String ratelimitPoolIdentifierString;
    private final ExecutorService threadPool;
    private final ExecutorService commandPool;
    private final SessionController sessionController;
    private BirthdayTask birthdayTask = new BirthdayTask();
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder().setNameFormat("Mantaro-ShardExecutor Thread-%d").build());
    private JDA jda;
    
    /**
     * Builds a new instance of a MantaroShard.
     *
     * @param shardId          The id of the newly-created shard.
     * @param totalShards      The total quantity of shards that the bot will startup with.
     * @param manager          The event manager.
     * @param commandProcessor The {@link ICommandProcessor} used to process upcoming Commands.
     * @throws LoginException
     * @throws InterruptedException
     */
    public MantaroShard(int shardId, int totalShards, MantaroEventManager manager, ICommandProcessor commandProcessor,
                        SessionController controller) throws LoginException, InterruptedException {
        this.callbackPoolIdentifierString = "callback-pool-shard-" + shardId;
        this.ratelimitPoolIdentifierString = "ratelimit-pool-shard-" + shardId;
        this.shardId = shardId;
        this.totalShards = totalShards;
        this.manager = manager;
        this.sessionController = controller;
        
        ThreadFactory normalTPNamedFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("MantaroShard-Executor[" + shardId + "/" + totalShards + "] Thread-%d")
                        .build();
        
        ThreadFactory commandTPNamedFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("MantaroShard-Command[" + shardId + "/" + totalShards + "] Thread-%d")
                        .build();
        
        threadPool = Executors.newCachedThreadPool(normalTPNamedFactory);
        commandPool = Executors.newCachedThreadPool(commandTPNamedFactory);
        
        Prometheus.THREAD_POOL_COLLECTOR.add("mantaro-shard-" + shardId + "-birthday-executor", executorService);
        Prometheus.THREAD_POOL_COLLECTOR.add("mantaro-shard-" + shardId + "-thread-pool", threadPool);
        Prometheus.THREAD_POOL_COLLECTOR.add("mantaro-shard-" + shardId + "-command-pool", commandPool);
        
        log = LoggerFactory.getLogger("MantaroShard-" + shardId);
        mantaroListener = new MantaroListener(shardId, this);
        commandListener = new CommandListener(shardId, this, commandProcessor);
        
        start(false);
        
        //Gauge the current ratelimit bucket queue size per shard.
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> ratelimitBucket.labels(String.valueOf(shardId))
                              .set(QUEUE_SIZE.apply(jda)), 1, 1, TimeUnit.MINUTES
        );
    }
    
    /**
     * Starts a new Shard.
     * This method builds a {@link JDA} instance and then attempts to start it up.
     * This locks until the shard finds a status of AWAITING_LOGIN_CONFIRMATION + 5 seconds.
     * <p>
     * The newly-started shard will have auto reconnect enabled, a core pool size of 18 and a new NAS instance. The rest is defined either on global or instance
     * variables.
     *
     * @param force Whether we will call {@link JDA#shutdown()} or {@link JDA#shutdownNow()}
     * @throws LoginException
     * @throws InterruptedException
     */
    public void start(boolean force) throws LoginException, InterruptedException {
        if(jda != null) {
            log.info("Attempting to drop shard {}...", shardId);
            prepareShutdown();
            
            if(!force)
                jda.shutdown();
            else
                jda.shutdownNow();
            
            log.info("Dropped shard #{} successfully!", shardId);
            removeListeners();
        }
        
        ThreadPoolExecutor callbackPool;
        ScheduledThreadPoolExecutor ratelimitPool;
        synchronized(this) {
            callbackPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(15);
            ratelimitPool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(config.ratelimitPoolSize);
            Prometheus.THREAD_POOL_COLLECTOR.remove(callbackPoolIdentifierString);
            Prometheus.THREAD_POOL_COLLECTOR.add(callbackPoolIdentifierString, callbackPool);
            Prometheus.THREAD_POOL_COLLECTOR.remove(ratelimitPoolIdentifierString);
            Prometheus.THREAD_POOL_COLLECTOR.add(ratelimitPoolIdentifierString, ratelimitPool);
        }
        
        JDABuilder jdaBuilder = new JDABuilder(AccountType.BOT)
                                        .setToken(config().get().token)
                                        .setAutoReconnect(true)
                                        .setRateLimitPool(ratelimitPool, true)
                                        .setCallbackPool(callbackPool, true)
                                        .setEventManager(manager)
                                        .setSessionController(sessionController)
                                        .setBulkDeleteSplittingEnabled(false)
                                        .useSharding(shardId, totalShards)
                                        .addEventListeners(MantaroBot.getInstance().getLavalink()) //try here then down there ig
                                        .setVoiceDispatchInterceptor(MantaroBot.getInstance().getLavalink().getVoiceInterceptor())
                                        .setDisabledCacheFlags(EnumSet.of(CacheFlag.ACTIVITY, CacheFlag.EMOTE))
                                        .setActivity(Activity.playing("Hold on to your seatbelts!"));
        
        if(shardId < getTotalShards() - 1) {
            jda = jdaBuilder.build();
        } else {
            //Block until all shards start up properly.
            jda = jdaBuilder.build().awaitReady();
        }
        
        //Assume everything is alright~
        addListeners();
    }
    
    private void addListeners() {
        log.debug("Added all listeners for shard {}", shardId);
        //jda.addEventListener(MantaroBot.getInstance().getLavalink());
        jda.addEventListener(mantaroListener, commandListener, VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(), ReactionOperations.listener());
    }
    
    private void removeListeners() {
        log.debug("Removed all listeners for shard {}", shardId);
        jda.removeEventListener(mantaroListener, commandListener, VOICE_CHANNEL_LISTENER, InteractiveOperations.listener(), ReactionOperations.listener());
    }
    
    /**
     * Starts the birthday task wait until tomorrow. When 00:00 arrives, this will call {@link BirthdayTask#handle(int)} every 24 hours.
     * Every shard has one birthday task.
     *
     * @param millisecondsUntilTomorrow The amount of milliseconds until 00:00.
     */
    public void startBirthdayTask(long millisecondsUntilTomorrow) {
        log.debug("Started birthday task for shard {}, scheduled to run in {} ms more", shardId, millisecondsUntilTomorrow);
        
        executorService.scheduleWithFixedDelay(() -> birthdayTask.handle(shardId),
                millisecondsUntilTomorrow, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Updates Mantaro's "splash".
     * Splashes are random gags like "now seen in theaters!" that show on Mantaro's status.
     * This has been on Mantaro since 2016, so it's part of its "personality" as a bot.
     */
    public void updateStatus() {
        Runnable changeStatus = () -> {
            //insert $CURRENT_YEAR meme here
            if(DateUtils.isSameDay(christmas, Calendar.getInstance())) {
                getJDA().getPresence().setActivity(Activity.playing(String.format("%shelp | %s | [%d]", config().get().prefix[0], "Merry Christmas!", getId())));
                return;
            } else if(DateUtils.isSameDay(newYear, Calendar.getInstance())) {
                getJDA().getPresence().setActivity(Activity.playing(String.format("%shelp | %s | [%d]", config().get().prefix[0], "Happy New Year!", getId())));
                return;
            }
            
            AtomicInteger users = new AtomicInteger(0), guilds = new AtomicInteger(0);
            if(MantaroBot.getInstance() != null) {
                Arrays.stream(MantaroBot.getInstance().getShardedMantaro().getShards()).filter(Objects::nonNull).filter(mantaroShard -> mantaroShard.getJDA() != null).map(MantaroShard::getJDA).forEach(jda -> {
                    users.addAndGet((int) jda.getUserCache().size());
                    guilds.addAndGet((int) jda.getGuildCache().size());
                });
            }
            
            JSONObject reply;
            
            try {
                Request request = new Request.Builder()
                                          .url(config.apiTwoUrl + "/mantaroapi/bot/splashes/random")
                                          .addHeader("Authorization", config.getApiAuthKey())
                                          .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                                          .get()
                                          .build();
                
                Response response = httpClient.newCall(request).execute();
                String body = response.body().string();
                response.close();
                
                reply = new JSONObject(body);
            } catch(Exception e) {
                //I had to, lol.
                reply = new JSONObject().put("splash", "With a missing status!");
            }
            
            String newStatus = reply.getString("splash")
                                       //Replace fest.
                                       .replace("%ramgb%", String.valueOf(((long) (Runtime.getRuntime().maxMemory() * 1.2D)) >> 30L))
                                       .replace("%usercount%", users.toString())
                                       .replace("%guildcount%", guilds.toString())
                                       .replace("%shardcount%", String.valueOf(getTotalShards()))
                                       .replace("%prettyusercount%", pretty(users.get()))
                                       .replace("%prettyguildcount%", pretty(guilds.get()));
            
            getJDA().getPresence().setActivity(Activity.playing(String.format("%shelp | %s | [%d]", config().get().prefix[0], newStatus, getId())));
            log.debug("Changed status to: " + newStatus);
        };
        
        changeStatus.run();
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Splash Thread"))
                .scheduleAtFixedRate(changeStatus, 10, 10, TimeUnit.MINUTES);
    }
    
    /**
     * @return The current {@link MantaroEventManager} for this specific instance.
     */
    public MantaroEventManager getShardEventManager() {
        return manager;
    }
    
    public int getId() {
        return shardId;
    }
    
    public JDA getJDA() {
        return jda;
    }
    
    private int getTotalShards() {
        return totalShards;
    }
    
    //This used to be bigger...
    public void prepareShutdown() {
        jda.removeEventListener(jda.getRegisteredListeners().toArray());
    }
    
    @Override
    public String toString() {
        return "MantaroShard [" + (getId()) + " / " + totalShards + "]";
    }
    
    public Cache<String, Optional<CachedMessage>> getMessageCache() {
        return this.messageCache;
    }
    
    public MantaroEventManager getManager() {
        return this.manager;
    }
    
    public ExecutorService getThreadPool() {
        return this.threadPool;
    }
    
    public ExecutorService getCommandPool() {
        return this.commandPool;
    }
    
    //delegates
    
    @Override
    @Nonnull
    public Status getStatus() {
        return jda.getStatus();
    }
    
    @Override
    public long getGatewayPing() {
        return jda.getGatewayPing();
    }
    
    @Override
    @Nonnull
    public RestAction<Long> getRestPing() {
        return jda.getRestPing();
    }
    
    @Override
    @Nonnull
    public JDA awaitStatus(@Nonnull Status status) throws InterruptedException {
        return jda.awaitStatus(status);
    }
    
    @Override
    @Nonnull
    public JDA awaitStatus(@Nonnull Status status, @Nonnull Status... failOn) throws InterruptedException {
        return jda.awaitStatus(status, failOn);
    }
    
    @Override
    @Nonnull
    public JDA awaitReady() throws InterruptedException {
        return jda.awaitReady();
    }
    
    @Override
    @Nonnull
    public ScheduledExecutorService getRateLimitPool() {
        return jda.getRateLimitPool();
    }
    
    @Override
    @Nonnull
    public ScheduledExecutorService getGatewayPool() {
        return jda.getGatewayPool();
    }
    
    @Override
    @Nonnull
    public ExecutorService getCallbackPool() {
        return jda.getCallbackPool();
    }
    
    @Override
    @Nonnull
    public OkHttpClient getHttpClient() {
        return jda.getHttpClient();
    }
    
    @Override
    @Nonnull
    public DirectAudioController getDirectAudioController() {
        return jda.getDirectAudioController();
    }
    
    @Override
    public void setEventManager(@Nullable IEventManager manager) {
        jda.setEventManager(manager);
    }
    
    @Override
    public void addEventListener(@Nonnull Object... listeners) {
        jda.addEventListener(listeners);
    }
    
    @Override
    public void removeEventListener(@Nonnull Object... listeners) {
        jda.removeEventListener(listeners);
    }
    
    @Override
    @Nonnull
    public List<Object> getRegisteredListeners() {
        return jda.getRegisteredListeners();
    }
    
    @Override
    @CheckReturnValue
    @Nonnull
    public GuildAction createGuild(@Nonnull String name) {
        return jda.createGuild(name);
    }
    
    @Override
    @Nonnull
    public CacheView<AudioManager> getAudioManagerCache() {
        return jda.getAudioManagerCache();
    }
    
    @Override
    @Nonnull
    public List<AudioManager> getAudioManagers() {
        return jda.getAudioManagers();
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<User> getUserCache() {
        return jda.getUserCache();
    }
    
    @Override
    @Nonnull
    public List<User> getUsers() {
        return jda.getUsers();
    }
    
    @Override
    @Nullable
    public User getUserById(@Nonnull String id) {
        return jda.getUserById(id);
    }
    
    @Override
    @Nullable
    public User getUserById(long id) {
        return jda.getUserById(id);
    }
    
    @Override
    @Nullable
    public User getUserByTag(@Nonnull String tag) {
        return jda.getUserByTag(tag);
    }
    
    @Override
    @Nullable
    public User getUserByTag(@Nonnull String username, @Nonnull String discriminator) {
        return jda.getUserByTag(username, discriminator);
    }
    
    @Override
    @Nonnull
    public List<User> getUsersByName(@Nonnull String name, boolean ignoreCase) {
        return jda.getUsersByName(name, ignoreCase);
    }
    
    @Override
    @Nonnull
    public List<Guild> getMutualGuilds(@Nonnull User... users) {
        return jda.getMutualGuilds(users);
    }
    
    @Override
    @Nonnull
    public List<Guild> getMutualGuilds(@Nonnull Collection<User> users) {
        return jda.getMutualGuilds(users);
    }
    
    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<User> retrieveUserById(@Nonnull String id) {
        return jda.retrieveUserById(id);
    }
    
    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<User> retrieveUserById(long id) {
        return jda.retrieveUserById(id);
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<Guild> getGuildCache() {
        return jda.getGuildCache();
    }
    
    @Override
    @Nonnull
    public List<Guild> getGuilds() {
        return jda.getGuilds();
    }
    
    @Override
    @Nullable
    public Guild getGuildById(@Nonnull String id) {
        return jda.getGuildById(id);
    }
    
    @Override
    @Nullable
    public Guild getGuildById(long id) {
        return jda.getGuildById(id);
    }
    
    @Override
    @Nonnull
    public List<Guild> getGuildsByName(@Nonnull String name, boolean ignoreCase) {
        return jda.getGuildsByName(name, ignoreCase);
    }
    
    @Override
    @Nonnull
    public Set<String> getUnavailableGuilds() {
        return jda.getUnavailableGuilds();
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<Role> getRoleCache() {
        return jda.getRoleCache();
    }
    
    @Override
    @Nonnull
    public List<Role> getRoles() {
        return jda.getRoles();
    }
    
    @Override
    @Nullable
    public Role getRoleById(@Nonnull String id) {
        return jda.getRoleById(id);
    }
    
    @Override
    @Nullable
    public Role getRoleById(long id) {
        return jda.getRoleById(id);
    }
    
    @Override
    @Nonnull
    public List<Role> getRolesByName(@Nonnull String name, boolean ignoreCase) {
        return jda.getRolesByName(name, ignoreCase);
    }
    
    @Override
    @Nullable
    public GuildChannel getGuildChannelById(@Nonnull String id) {
        return jda.getGuildChannelById(id);
    }
    
    @Override
    @Nullable
    public GuildChannel getGuildChannelById(long id) {
        return jda.getGuildChannelById(id);
    }
    
    @Override
    @Nullable
    public GuildChannel getGuildChannelById(@Nonnull ChannelType type, @Nonnull String id) {
        return jda.getGuildChannelById(type, id);
    }
    
    @Override
    @Nullable
    public GuildChannel getGuildChannelById(@Nonnull ChannelType type, long id) {
        return jda.getGuildChannelById(type, id);
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<Category> getCategoryCache() {
        return jda.getCategoryCache();
    }
    
    @Override
    @Nullable
    public Category getCategoryById(@Nonnull String id) {
        return jda.getCategoryById(id);
    }
    
    @Override
    @Nullable
    public Category getCategoryById(long id) {
        return jda.getCategoryById(id);
    }
    
    @Override
    @Nonnull
    public List<Category> getCategories() {
        return jda.getCategories();
    }
    
    @Override
    @Nonnull
    public List<Category> getCategoriesByName(@Nonnull String name, boolean ignoreCase) {
        return jda.getCategoriesByName(name, ignoreCase);
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<StoreChannel> getStoreChannelCache() {
        return jda.getStoreChannelCache();
    }
    
    @Override
    @Nullable
    public StoreChannel getStoreChannelById(@Nonnull String id) {
        return jda.getStoreChannelById(id);
    }
    
    @Override
    @Nullable
    public StoreChannel getStoreChannelById(long id) {
        return jda.getStoreChannelById(id);
    }
    
    @Override
    @Nonnull
    public List<StoreChannel> getStoreChannels() {
        return jda.getStoreChannels();
    }
    
    @Override
    @Nonnull
    public List<StoreChannel> getStoreChannelsByName(@Nonnull String name, boolean ignoreCase) {
        return jda.getStoreChannelsByName(name, ignoreCase);
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<TextChannel> getTextChannelCache() {
        return jda.getTextChannelCache();
    }
    
    @Override
    @Nonnull
    public List<TextChannel> getTextChannels() {
        return jda.getTextChannels();
    }
    
    @Override
    @Nullable
    public TextChannel getTextChannelById(@Nonnull String id) {
        return jda.getTextChannelById(id);
    }
    
    @Override
    @Nullable
    public TextChannel getTextChannelById(long id) {
        return jda.getTextChannelById(id);
    }
    
    @Override
    @Nonnull
    public List<TextChannel> getTextChannelsByName(@Nonnull String name, boolean ignoreCase) {
        return jda.getTextChannelsByName(name, ignoreCase);
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
        return jda.getVoiceChannelCache();
    }
    
    @Override
    @Nonnull
    public List<VoiceChannel> getVoiceChannels() {
        return jda.getVoiceChannels();
    }
    
    @Override
    @Nullable
    public VoiceChannel getVoiceChannelById(@Nonnull String id) {
        return jda.getVoiceChannelById(id);
    }
    
    @Override
    @Nullable
    public VoiceChannel getVoiceChannelById(long id) {
        return jda.getVoiceChannelById(id);
    }
    
    @Override
    @Nonnull
    public List<VoiceChannel> getVoiceChannelsByName(@Nonnull String name, boolean ignoreCase) {
        return jda.getVoiceChannelsByName(name, ignoreCase);
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<PrivateChannel> getPrivateChannelCache() {
        return jda.getPrivateChannelCache();
    }
    
    @Override
    @Nonnull
    public List<PrivateChannel> getPrivateChannels() {
        return jda.getPrivateChannels();
    }
    
    @Override
    @Nullable
    public PrivateChannel getPrivateChannelById(@Nonnull String id) {
        return jda.getPrivateChannelById(id);
    }
    
    @Override
    @Nullable
    public PrivateChannel getPrivateChannelById(long id) {
        return jda.getPrivateChannelById(id);
    }
    
    @Override
    @Nonnull
    public SnowflakeCacheView<Emote> getEmoteCache() {
        return jda.getEmoteCache();
    }
    
    @Override
    @Nonnull
    public List<Emote> getEmotes() {
        return jda.getEmotes();
    }
    
    @Override
    @Nullable
    public Emote getEmoteById(@Nonnull String id) {
        return jda.getEmoteById(id);
    }
    
    @Override
    @Nullable
    public Emote getEmoteById(long id) {
        return jda.getEmoteById(id);
    }
    
    @Override
    @Nonnull
    public List<Emote> getEmotesByName(@Nonnull String name, boolean ignoreCase) {
        return jda.getEmotesByName(name, ignoreCase);
    }
    
    @Override
    @Nonnull
    public IEventManager getEventManager() {
        return jda.getEventManager();
    }
    
    @Override
    @Nonnull
    public SelfUser getSelfUser() {
        return jda.getSelfUser();
    }
    
    @Override
    @Nonnull
    public Presence getPresence() {
        return jda.getPresence();
    }
    
    @Override
    @Nonnull
    public ShardInfo getShardInfo() {
        return jda.getShardInfo();
    }
    
    @Override
    @Nonnull
    public String getToken() {
        return jda.getToken();
    }
    
    @Override
    public long getResponseTotal() {
        return jda.getResponseTotal();
    }
    
    @Override
    public int getMaxReconnectDelay() {
        return jda.getMaxReconnectDelay();
    }
    
    @Override
    public void setAutoReconnect(boolean reconnect) {
        jda.setAutoReconnect(reconnect);
    }
    
    @Override
    public void setRequestTimeoutRetry(boolean retryOnTimeout) {
        jda.setRequestTimeoutRetry(retryOnTimeout);
    }
    
    @Override
    public boolean isAutoReconnect() {
        return jda.isAutoReconnect();
    }
    
    @Override
    public boolean isBulkDeleteSplittingEnabled() {
        return jda.isBulkDeleteSplittingEnabled();
    }
    
    @Override
    public void shutdown() {
        jda.shutdown();
    }
    
    @Override
    public void shutdownNow() {
        jda.shutdownNow();
    }
    
    @Override
    @Nonnull
    public AccountType getAccountType() {
        return jda.getAccountType();
    }
    
    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<ApplicationInfo> retrieveApplicationInfo() {
        return jda.retrieveApplicationInfo();
    }
    
    @Override
    @Nonnull
    public String getInviteUrl(@Nullable Permission... permissions) {
        return jda.getInviteUrl(permissions);
    }
    
    @Override
    @Nonnull
    public String getInviteUrl(@Nullable Collection<Permission> permissions) {
        return jda.getInviteUrl(permissions);
    }
    
    @Override
    @Nullable
    public ShardManager getShardManager() {
        return jda.getShardManager();
    }
    
    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Webhook> retrieveWebhookById(@Nonnull String webhookId) {
        return jda.retrieveWebhookById(webhookId);
    }
    
    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Webhook> retrieveWebhookById(long webhookId) {
        return jda.retrieveWebhookById(webhookId);
    }
    
    @Override
    @CheckReturnValue
    @Nonnull
    public AuditableRestAction<Integer> installAuxiliaryPort() {
        return jda.installAuxiliaryPort();
    }
}
