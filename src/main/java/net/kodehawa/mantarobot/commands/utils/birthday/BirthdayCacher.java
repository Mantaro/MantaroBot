package net.kodehawa.mantarobot.commands.utils.birthday;

import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Cursor;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.rethinkdb.RethinkDB.r;

/**
 * Caches the birthday date of all users seen on bot startup and adds them to a local ConcurrentHashMap.
 * This will later be used on {@link BirthdayTask}
 */
@Slf4j
public class BirthdayCacher {
    public volatile boolean isDone;
    public Map<String, String> cachedBirthdays = new ConcurrentHashMap<>();

    public BirthdayCacher(){
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        log.info("Caching birthdays...");
        executorService.submit(() -> {
            try{
                Cursor<Map> m = r.table("users")
                        .orderBy()
                        .optArg("index", r.desc("premiumUntil"))
                        .run(MantaroData.conn(), OptArgs.of("read_mode", "outdated"));

                List<Map> m1 = m.toList();

                for(Map r : m1){
                    //Blame rethinkdb for the casting hell thx
                    String birthday = (String)((HashMap)r.get("data")).get("birthday");
                    if(birthday != null && !birthday.isEmpty()){
                        log.debug("-> PROCESS: {}", r);
                        cachedBirthdays.putIfAbsent(String.valueOf(r.get("id")), birthday);
                    }
                }

                log.debug("-> [CACHE] Birthdays: {}", cachedBirthdays);

                m.close();
                isDone = true;
            } catch (Exception e){
                e.printStackTrace();
            }
        });
    }
}
