package com.appiphany.nacc.utils;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import android.os.Process;

public class UncaughtExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
	private String logPath;

	public UncaughtExceptionHandler(String filepath) {
		logPath = filepath;
	}

	public void uncaughtException(Thread thread, Throwable exception) {
		Ln.d("call handle exception");	
		
		synchronized (UncaughtExceptionHandler.class) {
			try {
				PrintWriter pw = new PrintWriter(new FileOutputStream(logPath));     
				exception.printStackTrace(pw);
				pw.flush();
				pw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		// kill the process
		Process.killProcess(Process.myPid());
		System.exit(10);
	}

	/**
	 * <p>
	 * Gets the stack trace from a Throwable as a String.
	 * </p>
	 * 
	 * <p>
	 * The result of this method vary by JDK version as this method uses
	 * {@link Throwable#printStackTrace(java.io.PrintWriter)}. On JDK1.3 and
	 * earlier, the cause exception will not be shown unless the specified
	 * throwable alters printStackTrace.
	 * </p>
	 * 
	 * @param throwable
	 *            the <code>Throwable</code> to be examined
	 * @return the stack trace as generated by the exception's
	 *         <code>printStackTrace(PrintWriter)</code> method
	 */
	public static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
}
