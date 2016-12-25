package net.kodehawa.mantarobot.log;

public enum Type {

	INFO, WARNING, CRITICAL;

	@Override
	public String toString() {
		String result = null;
		switch(this){
			case INFO:
				result = "Info";
				break;
			case WARNING:
				result = "Warning";
				break;
			case CRITICAL:
				result = "Critical";
				break;
		}
		return result;
	}
}
