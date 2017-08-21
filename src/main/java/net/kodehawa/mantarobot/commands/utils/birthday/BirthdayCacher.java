package net.kodehawa.mantarobot.commands.utils.birthday;

import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Cursor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.rethinkdb.RethinkDB.r;

/**
 * Caches the birthday date of all users seen on bot startup and adds them to a local ConcurrentHashMap.
 * This will later be used on {@link BirthdayTask}
 */
public class BirthdayCacher {
    public volatile boolean isDone;
    public Map<String, String> cachedBirthdays = new ConcurrentHashMap<>();

    public BirthdayCacher(ManagedDatabase db){
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        executorService.submit(() -> {
            try{
                Cursor<Map> m = r.table("users")
                        .orderBy()
                        .optArg("index", "birthday")
                        .map(player -> player.pluck("id", "birthday"))
                        .run(MantaroData.conn(), OptArgs.of("read_mode", "outdated"));
                for(Map r : m){
                    cachedBirthdays.putIfAbsent(String.valueOf(r.get("id")), String.valueOf(r.get("birthday")));
                }

                m.close();
                isDone = true;
            } catch (Exception e){
                e.printStackTrace();
            }
        });
    }
}
