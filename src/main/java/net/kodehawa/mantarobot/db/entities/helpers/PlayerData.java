package net.kodehawa.mantarobot.db.entities.helpers;

import lombok.Data;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;

import java.beans.Transient;
import java.text.SimpleDateFormat;
import java.util.*;

@Data
public class PlayerData {
	public long experience = 0;
	private String description = null;
	private Long marriedSince = null;
	private String marriedWith = null;
	private long lockedUntil = 0;
	private List<Badge> badges = new ArrayList<>();
	private long gamesWon = 0;

	@Transient
	public boolean isMarried() {
		return marriedWith != null;
	}

	@Transient
	public String marryDate() {
		if(getMarriedSince() == null) return null;
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
		final Date date = new Date(getMarriedSince());
		return sdf.format(date);
	}

	@Transient
	public String anniversary() {
		if(getMarriedSince() == null) return null;
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
		Calendar cal = new GregorianCalendar();
		cal.setTime(new Date(getMarriedSince()));
		cal.add(Calendar.YEAR, 1);
		return sdf.format(cal.getTime());
	}

	@Transient
	public boolean hasBadge(Badge b){
		return badges.contains(b);
	}

	@Transient
	public boolean addBadge(Badge b){
		if(hasBadge(b)){
			return false;
		}

		badges.add(b);
		return true;
	}

	@Transient
	public boolean removeBadge(Badge b){
		if(!hasBadge(b)){
			return false;
		}

		badges.remove(b);
		return true;
	}
}