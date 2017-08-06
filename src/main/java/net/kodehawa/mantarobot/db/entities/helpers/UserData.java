package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;

@Data
public class UserData {
    private String birthday;
    private int reminderN;
    private String timezone;

    public void write(Output out) {
        out.writeInt(reminderN);
        out.writeUTF(birthday, true);
        out.writeUTF(timezone, true);
    }

    public void read(Input in) {
        reminderN = in.readInt();
        birthday = in.readUTF(true);
        timezone = in.readUTF(true);
    }
}
