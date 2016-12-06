package net.kodehawa.mantarobot.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolHelper {
	
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
	private static volatile ThreadPoolHelper threadhelper = new ThreadPoolHelper();
    
	public void startThread(String task, Runnable thread) {
		executor.execute(thread);
	}
	
	public void purge(){
		executor.purge();
	}
	
	public void startThread(String task, ThreadPoolExecutor exec, Runnable thread){
		exec.execute(thread);
	}
	
	public static ThreadPoolHelper instance(){
		return threadhelper;
	}
}
