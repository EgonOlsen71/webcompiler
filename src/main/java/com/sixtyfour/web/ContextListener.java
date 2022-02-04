package com.sixtyfour.web;

import java.io.FileReader;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Reads the config (once! If you want to modify it, you have to restart the
 * server) and starts a thread to transmit this servers IP to another one if
 * desired.
 * 
 * @author EgonOlsen
 *
 */
@WebListener
public class ContextListener implements ServletContextListener {

	private static final String CONFIG_PATH = "/webdata/wiconf.ini";

	private boolean exit;

	private String token = "changeMe!";
	private String refreshUrl = "https://jpct.de/mospeed/ipstore?token=";
	private boolean doRefresh = true;

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {

		Logger.log("Reading configuration...");
		Properties conf = new Properties();
		try {
			conf.load(new FileReader(CONFIG_PATH));
			token = conf.getProperty("token");
			refreshUrl = conf.getProperty("refresh.url");
			String ref = conf.getProperty("refresh.enabled");
			doRefresh = Boolean.valueOf(ref != null ? ref : "false");
			Logger.log("Config: " + token + "/" + refreshUrl + "/" + doRefresh);
		} catch (Exception e) {
			Logger.log("Failed to read configuration, using defaults!", e);
		}

		if (doRefresh) {
			new Thread() {
				@Override
				public void run() {
					do {
						Logger.log("Refreshing IP...");
						try (Scanner scanny = new Scanner(new URL(refreshUrl + token).openStream(), "UTF-8")) {
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
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		exit = true;
		Logger.log("Shutting down!");
	}
}
