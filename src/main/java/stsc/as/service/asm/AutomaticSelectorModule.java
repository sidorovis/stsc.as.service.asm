package stsc.as.service.asm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.TimeZone;
import java.util.logging.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;

import stsc.common.service.ApplicationHelper;
import stsc.common.service.StopableApp;

/**
 * This is as service like (executable) class that automatically read experiment settings from database and start-up experiment on selected distributed system
 * (spark, hadoop like).
 * 
 */
final class AutomaticSelectorModule implements StopableApp {

	static {
		System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "./config/log4j2.xml");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	private final Logger logger = LogManager.getLogger(AutomaticSelectorModule.class.getName());

	private volatile boolean stopped = false;
	
	private volatile int sleepMicroseconds = 1000;

	private AutomaticSelectorModule() throws FileNotFoundException, IOException, SQLException {
	}

	@Override
	public void start() throws Exception {
		while (!stopped) {
			
			Thread.sleep(sleepMicroseconds);
		}
	}

	@Override
	public void stop() throws Exception {
		this.stopped = true;
	}

	@Override
	public void log(Level logLevel, String message) {
		logger.error("log: " + logLevel.getName() + ", message:" + message);
	}

	public static void main(String[] args) {
		try {
			final StopableApp app = new AutomaticSelectorModule();
			ApplicationHelper.createHelper(app, (e) -> {
				e.printStackTrace();
				return true;
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
