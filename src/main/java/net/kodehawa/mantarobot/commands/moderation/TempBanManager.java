package net.kodehawa.mantarobot.commands.moderation;

import net.kodehawa.mantarobot.MantaroBot;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class TempBanManager {
    private final Map<String, Long> UNBANS;
    private boolean unbansUpdated = false;

    public TempBanManager() {
        this(new HashMap<>());
    }

    public TempBanManager(Map<String, Long> unbans) {
        UNBANS = Collections.synchronizedMap(unbans);

        Thread thread = new Thread(this::threadcode, "Tempbans Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void addTempban(String id, Long milis) {
        UNBANS.put(id, milis);
        unbansUpdated = true;
        synchronized(this) {
            notify();
        }
    }

    public void removeTempban(String id) {

        if(UNBANS.containsKey(id)) {
            UNBANS.remove(id);
            unbansUpdated = true;
            synchronized(this) {
                notify();
            }
        }
    }

    private void threadcode() {
        try {
            //noinspection InfiniteLoopStatement
            while(true) {
                if(UNBANS.isEmpty()) {
                    try {
                        synchronized(this) {
                            wait();
                            unbansUpdated = false;
                        }
                    } catch(InterruptedException ignored) {
                    }
                }

                //noinspection OptionalGetWithoutIsPresent
                Map.Entry<String, Long> unbanFirstEntry = UNBANS.entrySet().stream().sorted(Comparator.comparingLong(Map.Entry::getValue)).findFirst().get();

                try {
                    long timeout = unbanFirstEntry.getValue() - System.currentTimeMillis();
                    if(timeout > 0) {
                        synchronized(this) {
                            wait(timeout);
                        }
                    }
                } catch(InterruptedException ignored) {
                }

                if(!unbansUpdated) {
                    String[] params = unbanFirstEntry.getKey().split(":");
                    UNBANS.remove(unbanFirstEntry.getKey());
                    MantaroBot.getInstance().getGuildById(params[0]).getController().unban(params[1]).queue();
                    UNBANS.remove(unbanFirstEntry.getKey());
                    if(MantaroBot.getInstance().getGuildById(params[0]) == null) return;
                    ModLog.logUnban(MantaroBot.getInstance().getGuildById(params[0]).getSelfMember(), params[1], "The temporary ban ended.");
                } else unbansUpdated = false; //and the loop will restart and resolve it
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}