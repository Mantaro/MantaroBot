package net.kodehawa.mantarobot.log;

import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.core.Mantaro;
import net.kodehawa.mantarobot.exception.ExceptionHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Log {

	private static final Log instance = new Log();

	public static Log instance() {
		return instance;
	}

	private final Date date = new Date();
	private final ExceptionHandler exceptionHandler = new ExceptionHandler();
	private final DateFormat hour = new SimpleDateFormat("HH:mm:ss");
	private final String hour1 = hour.format(date);

	private void handle(Class clazz, String content) {
		if (!Mantaro.instance().getState().equals(State.PRELOAD) && !Mantaro.instance().getState().equals(State.LOADING)) {
			TextChannel txc = Mantaro.instance().getSelf().getTextChannelById("266231083341840385");
			txc.sendMessage("[" + hour1 + "] " + "[" + clazz.getSimpleName() + "] " + content).queue();
		}
	}

	private void handle(String content) {
		if (!Mantaro.instance().getState().equals(State.PRELOAD) && !Mantaro.instance().getState().equals(State.LOADING)) {
			TextChannel txc = Mantaro.instance().getSelf().getTextChannelById("266231083341840385");
			txc.sendMessage("[" + hour1 + "] " + content).queue();
		}
	}

	/**
	 * Most used type of log. Tells you what class called it.
	 * State it's explicitly told here.
	 *
	 * @param content The content of the log message.
	 * @param type    The type of log.
	 * @param clazz   The class its called from.
	 * @param state   The bot state. Explicit.
	 */
	public void print(State state, String content, Class clazz, Type type) {
		System.out.println("[" + hour1 + "] " + "[" + type.toString() + "] " + "[" + state + "] " + "[Mantaro" + "/" + clazz.getSimpleName() + "]: " + content);
		this.handle(clazz, content);
	}

	/**
	 * For use when something goes wrong.
	 *
	 * @param content The content of the message.
	 * @param clazz   The class from where the message is coming from.
	 * @param type    Type of log. (net.kodehawa.mantarobot.log.Type)
	 * @param ex      The exception thrown.
	 */
	public void print(String content, Class clazz, Type type, Exception ex) {
		System.out.println("[" + hour1 + "] " + "[" + type.toString() + "] " + "[Mantaro" + "/" + clazz.getSimpleName() + "]: " + content);
		exceptionHandler.handle(content, type, Mantaro.instance().getState(), ex);
		this.handle(clazz, content);
	}

	/**
	 * For use when something goes wrong.
	 * In this case state is explicitly declared.
	 *
	 * @param state   The bot state. Explicitly declared.
	 * @param content The content of the message.
	 * @param clazz   The class from where the message is coming from.
	 * @param type    Type of log. (net.kodehawa.mantarobot.log.Type)
	 * @param ex      The exception thrown.
	 */
	public void print(State state, String content, Class clazz, Type type, Exception ex) {
		System.out.println("[" + hour1 + "] " + "[" + type.toString() + "] " + "[" + state + "] " + "[Mantaro" + "/" + clazz.getSimpleName() + "]: " + content);
		exceptionHandler.handle(content, type, Mantaro.instance().getState(), ex);
		this.handle(content);
	}

	/**
	 * Simplest type of log. Doesn't tell you what class called it.
	 *
	 * @param content The content of the log message.
	 * @param type    The type of log.
	 */
	public void print(String content, Type type) {
		System.out.println("[" + hour1 + "] " + "[" + Mantaro.instance().getState() + "] " + "[" + type.toString() + "] [Mantaro]: " + content);
		this.handle(content);
	}

	/**
	 * Most used type of log. Tells you what class called it.
	 *
	 * @param content The content of the log message.
	 * @param type    The type of log.
	 * @param clazz   The class its called from.
	 */
	public void print(String content, Class clazz, Type type) {
		System.out.println("[" + hour1 + "] " + "[" + type.toString() + "] " + "[Mantaro" + "/" + clazz.getSimpleName() + "]: " + content);
		this.handle(clazz, content);
	}
}