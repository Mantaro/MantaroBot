package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;
import lombok.Setter;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;

@Data
public class UserData {
    private String birthday;
    private int reminderN;
    private String timezone;
    private String premiumKey;
    private boolean hasReceivedFirstKey;

    public void write(Output out) {
        out.writeInt(reminderN);
        out.writeUTF(birthday, true);
        out.writeUTF(timezone, true);
        out.writeUTF(premiumKey, true);
        out.writeBoolean(hasReceivedFirstKey);
    }

    public void read(Input in) {
        reminderN = in.readInt();
        birthday = in.readUTF(true);
        timezone = in.readUTF(true);
        premiumKey = in.readUTF(true);
        hasReceivedFirstKey = in.readBoolean();
    }
}