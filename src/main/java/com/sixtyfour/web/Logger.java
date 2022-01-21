package com.sixtyfour.web;

import java.util.Date;

/**
 * 
 * @author EgonOlsen
 *
 */
public class Logger {

	public static void log(String txt) {
		System.out.println(new Date() + " - " + txt);
	}
	
	public static void log(String txt, Throwable t) {
		System.out.println(new Date() + " - " + txt);
		t.printStackTrace();
	}

}
