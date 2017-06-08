package net.kodehawa.mantarobot.web;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@AllArgsConstructor
public class CommandsEntity {
    public Map<Integer, Map<String, AtomicInteger>> total;
    public Map<Integer, Map<String, AtomicInteger>> today;
    public Map<Integer, Map<String, AtomicInteger>> hourly;
    public Map<Integer, Map<String, AtomicInteger>> now;
}

