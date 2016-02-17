package stsc.as.service.asm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;

import stsc.common.algorithms.BadAlgorithmException;
import stsc.common.service.ApplicationHelper;
import stsc.common.service.StopableApp;
import stsc.common.storage.StockStorage;
import stsc.database.migrations.optimizer.OptimizerDatabaseSettings;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExperiment;
import stsc.database.service.storages.optimizer.OptimizerDatabaseStorage;
import stsc.database.service.storages.optimizer.OptimizerTradingStrategiesDatabaseStorage;
import stsc.distributed.common.types.SimulatorSettingsExternalizable;
import stsc.distributed.spark.grid.GridSparkStarter;
import stsc.general.simulator.SimulatorConfiguration;
import stsc.general.simulator.multistarter.BadParameterException;
import stsc.general.simulator.multistarter.grid.SimulatorSettingsGridList;
import stsc.general.strategy.TradingStrategy;
import stsc.storage.mocks.StockStorageMock;

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

	final OptimizerDatabaseStorage optimizerDatabaseStorage;

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
			final Optional<OrmliteOptimizerExperiment> experiment = optimizerDatabaseStorage.getExperimentsStorage().bookExperiment();
			if (experiment.isPresent()) {
				solveExperiment(experiment.get());
			}
			Thread.sleep(sleepMicroseconds);
		}
	}

	private void solveExperiment(OrmliteOptimizerExperiment experiment) {
		try {
			executeSolver(experiment);
			experiment.setProcessed();
			optimizerDatabaseStorage.getExperimentsStorage().saveExperiment(experiment);
		} catch (Exception e) {
			try {
				optimizerDatabaseStorage.getExperimentsStorage().deleteLock(experiment);
			} catch (SQLException de) {
				logger.error("while deleting lock for ", experiment, de);
			}
		}
	}

	private void executeSolver(OrmliteOptimizerExperiment ormliteOptimizerExperiment) throws SQLException, BadParameterException, BadAlgorithmException {
		final ExperimentTransformer experimentTransformer = new ExperimentTransformer(optimizerDatabaseStorage.getExperimentsStorage());
		final SimulatorSettingsGridList experiment = experimentTransformer.transform(stockStorage, ormliteOptimizerExperiment);

		final ArrayList<SimulatorSettingsExternalizable> externalizableExperiment = new ArrayList<>();
		for (SimulatorConfiguration ss : experiment) {
			externalizableExperiment.add(new SimulatorSettingsExternalizable(ss));
		}

		final GridSparkStarter gridSparkStarter = new GridSparkStarter();
		final List<TradingStrategy> tradingStrategies = gridSparkStarter.searchOnSpark(externalizableExperiment);

		for (TradingStrategy ts : tradingStrategies) {
			saveTradingStrategy(ts);
		}
	}

	private void saveTradingStrategy(TradingStrategy ts) throws SQLException {
		final OptimizerTradingStrategiesDatabaseStorage storage = optimizerDatabaseStorage.getTradingStrategiesStorage();
		final TradingStrategyTransformer transformer = new TradingStrategyTransformer(storage);
		transformer.transformAndStore(ts);
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
