package net.kodehawa.mantarobot.web.service;

import br.com.brjdevs.java.utils.async.Async;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.data.MantaroData;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MantaroAPIChecker {
    private long lastPing = 0;
    public APIStatus STATUS = APIStatus.ONLINE;

    public void startService(){
        Runnable checker = () -> {
            try{
                long start = System.currentTimeMillis();
                Unirest.get(String.format("http://%s/api/", MantaroData.config().get().apiUrl)).asString().getBody();
                long end = System.currentTimeMillis();
                lastPing = end - start;
                STATUS = APIStatus.ONLINE;
            } catch (UnirestException e){
                STATUS = APIStatus.OFFLINE;
            }
        };
        Async.task("Mantaro API status check", checker, 30, TimeUnit.SECONDS);
    }

    public long getAPIPing() {
        return lastPing;
    }
}
