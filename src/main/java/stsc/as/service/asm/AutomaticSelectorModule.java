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
import stsc.common.storage.StockStorage;
import stsc.database.migrations.optimizer.OptimizerDatabaseSettings;
import stsc.database.service.storages.optimizer.OptimizerDatabaseStorage;
import stsc.storage.mocks.StockStorageMock;

/**
 * This is as service (executable) class that do next: <br/>
 * 1. automatically read experiment settings from database; <br/>
 * 2. start-up experiment on selected distributed system (spark, hadoop like); <br/>
 * 3. store results back to database.
 */
final class AutomaticSelectorModule implements StopableApp {

	static {
		System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "./config/log4j2.xml");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	private final Logger logger = LogManager.getLogger(AutomaticSelectorModule.class.getName());

	private volatile boolean stopped = false;

	private final OptimizerDatabaseStorage optimizerDatabaseStorage;

	private volatile int sleepMicroseconds = 1000;

	private final StockStorage stockStorage = StockStorageMock.getStockStorage();

	private AutomaticSelectorModule(String filepath) throws FileNotFoundException, IOException, SQLException {
		final OptimizerDatabaseSettings databaseSettings = (filepath != null) ? //
				new OptimizerDatabaseSettings(filepath)
				: //
				new OptimizerDatabaseSettings("./config/optimizer_production.properties");
		this.optimizerDatabaseStorage = new OptimizerDatabaseStorage(databaseSettings);
	}

	@Override
	public void start() throws Exception {
		while (!stopped) {
			final ExperimentSolver experimentSolver = new ExperimentSolver(optimizerDatabaseStorage, stockStorage);
			experimentSolver.findAndSolveExperiment();
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
			final String filepath = (args.length > 0) ? args[0] : null;
			final StopableApp app = new AutomaticSelectorModule(filepath);
			ApplicationHelper.createHelper(app, (e) -> {
				e.printStackTrace();
				return true;
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
