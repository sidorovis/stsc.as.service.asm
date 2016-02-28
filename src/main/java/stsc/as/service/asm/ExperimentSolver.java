package stsc.as.service.asm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;

import stsc.common.algorithms.BadAlgorithmException;
import stsc.common.storage.StockStorage;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExperiment;
import stsc.database.service.storages.optimizer.OptimizerDatabaseStorage;
import stsc.distributed.common.types.SimulatorSettingsExternalizable;
import stsc.distributed.spark.grid.GridSparkStarter;
import stsc.general.simulator.Execution;
import stsc.general.simulator.multistarter.BadParameterException;
import stsc.general.simulator.multistarter.grid.SimulatorSettingsGridList;
import stsc.general.strategy.TradingStrategy;

/**
 * This class contain full cycle for automatic selector module: <br/>
 * 1. look for ready experiment at database and book them; <br/>
 * 2. solve experiment; <br/>
 * 3. store experiment results (trading strategies) to database.<br/>
 */
final class ExperimentSolver {

	static {
		System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "./config/log4j2.xml");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	private final Logger logger = LogManager.getLogger(ExperimentSolver.class.getName());

	private final OptimizerDatabaseStorage optimizerDatabaseStorage;
	private final StockStorage stockStorage;

	public ExperimentSolver(final OptimizerDatabaseStorage optimizerDatabaseStorage, final StockStorage stockStorage) {
		this.optimizerDatabaseStorage = optimizerDatabaseStorage;
		this.stockStorage = stockStorage;
	}

	public void findAndSolveExperiment() throws SQLException {
		final Optional<OrmliteOptimizerExperiment> experiment = optimizerDatabaseStorage.getExperimentsStorage().bookExperiment();
		if (experiment.isPresent()) {
			solveExperiment(experiment.get());
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

	private void executeSolver(final OrmliteOptimizerExperiment ormliteOptimizerExperiment) throws SQLException, BadParameterException, BadAlgorithmException {
		final ExperimentTransformer experimentTransformer = new ExperimentTransformer(optimizerDatabaseStorage.getExperimentsStorage());
		final SimulatorSettingsGridList experiment = experimentTransformer.transform(stockStorage, ormliteOptimizerExperiment);

		final ArrayList<SimulatorSettingsExternalizable> externalizableExperiment = new ArrayList<>();
		for (Execution ss : experiment) {
			externalizableExperiment.add(new SimulatorSettingsExternalizable(ss));
		}

		final GridSparkStarter gridSparkStarter = new GridSparkStarter();
		final List<TradingStrategy> tradingStrategies = gridSparkStarter.searchOnSpark(externalizableExperiment);

		for (TradingStrategy ts : tradingStrategies) {
			saveTradingStrategy(ormliteOptimizerExperiment, ts);
		}
	}

	private void saveTradingStrategy(OrmliteOptimizerExperiment ormliteOptimizerExperiment, TradingStrategy ts) throws SQLException {
		final TradingStrategyTransformer transformer = new TradingStrategyTransformer(optimizerDatabaseStorage, ormliteOptimizerExperiment);
		transformer.transformAndStore(ts);
	}

}
