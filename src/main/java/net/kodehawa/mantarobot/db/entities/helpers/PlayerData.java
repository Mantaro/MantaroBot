package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;

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
	private Long marriedSince = null;
	private String marriedWith = null;

	@Transient
	public boolean isMarried() {
		return marriedWith != null;
	}

	@Transient
	public String marryDate(){
		if(getMarriedSince() == null) return null;
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
		final Date date = new Date(getMarriedSince());
		return sdf.format(date);
	}

	@Transient
	public String anniversary(){
		if(getMarriedSince() == null) return null;
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
		Calendar cal = new GregorianCalendar();
		cal.setTime(new Date(getMarriedSince()));
		cal.add(Calendar.YEAR, 1);
		return sdf.format(cal.getTime());
	}
}