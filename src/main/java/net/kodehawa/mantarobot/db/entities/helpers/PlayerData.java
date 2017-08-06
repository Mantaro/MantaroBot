package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;
import net.kodehawa.mantarobot.db.redis.Input;
import net.kodehawa.mantarobot.db.redis.Output;

import java.beans.Transient;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

@Data
public class PlayerData {
    public long experience = 0;
    private String description = null;
    private long lockedUntil = 0;
    private Long marriedSince = null;
    private String marriedWith = null;

    @Transient
    public boolean isMarried() {
        return marriedWith != null;
    }

    @Transient
    public String marryDate() {
        if(marriedSince == null || marriedSince == 0) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        final Date date = new Date(getMarriedSince());
        return sdf.format(date);
    }

    @Transient
    public String anniversary() {
        if(marriedSince == null || marriedSince == 0) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date(getMarriedSince()));
        cal.add(Calendar.YEAR, 1);
        return sdf.format(cal.getTime());
    }

    public void write(Output out) {
        out.writeLong(experience);
        out.writeLong(lockedUntil);
        out.writeLong(marriedSince, true);
        out.writeUTF(marriedWith, true);
        out.writeUTF(description, true);
    }


    public void read(Input in) {
        experience = in.readLong();
        lockedUntil = in.readLong();
        marriedSince = in.readLong(true);
        marriedWith = in.readUTF(true);
        description = in.readUTF(true);
    }
}