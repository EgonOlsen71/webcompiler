package com.sixtyfour.web;

import java.net.URL;
import java.util.Scanner;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * 
 * @author EgonOlsen
 *
 */
@WebListener
public class ContextListener implements ServletContextListener {

	private boolean exit;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		new Thread() {
			@Override
			public void run() {
				do {
					Logger.log("Refreshing IP...");
					try (Scanner scanny = new Scanner(new URL("https://jpct.de/mospeed/ipstore?token=19786b73eac3c6fa5c2fe037e4be2edc").openStream(), "UTF-8")) {
						String txt = scanny.next();
						Logger.log("Refreshed: " + txt);
					} catch (Throwable e) {
						Logger.log("Failed to update IP!", e);
					}
					
					try {
						Thread.sleep(1000 * 60 * 15);
					} catch (InterruptedException e) {
						//
					}
				} while (!exit);
			}
		}.start();
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		exit = true;
		Logger.log("Shutting down!");
	}
}
