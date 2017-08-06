package net.kodehawa.mantarobot.utils.data;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.utils.SentryHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class SimpleFileDataManager implements DataManager<List<String>> {
    public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r\\n?|\\r?\\n");
    private final List<String> data = new ArrayList<>();
    private final Path path;

    @SneakyThrows
    public SimpleFileDataManager(String file) {
        this.path = Paths.get(file);
        if(!this.path.toFile().exists()) {
            log.info("Could not find config file at " + this.path.toFile().getAbsolutePath() + ", creating a new one...");
            if(this.path.toFile().createNewFile()) {
                log.info("Generated new config file at " + this.path.toFile().getAbsolutePath() + ".");
                FileIOUtils.write(this.path, this.data.stream().collect(Collectors.joining()));
                log.info("Please, fill the file with valid properties.");
            } else {
                SentryHelper.captureMessage("Could not create config file at " + file, this.getClass());
            }
        }

        Collections.addAll(data, NEWLINE_PATTERN.split(FileIOUtils.read(this.path)));
        data.removeIf(s -> s.startsWith("//"));
    }

    @Override
    public List<String> get() {
        return data;
    }

    @Override
    @SneakyThrows
    public void save() {
        FileIOUtils.write(path, this.data.stream().collect(Collectors.joining("\n")));
    }
}
