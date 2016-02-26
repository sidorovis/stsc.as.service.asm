package stsc.as.service.asm;

import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;

import stsc.common.FromToPeriod;
import stsc.common.algorithms.AlgorithmType;
import stsc.common.storage.StockStorage;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerDoubleDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExecution;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExperiment;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerIntegerDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerStringDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerSubExecutionDomen;
import stsc.database.service.storages.optimizer.OptimizerExperimentsDatabaseStorage;
import stsc.general.simulator.multistarter.AlgorithmSettingsIteratorFactory;
import stsc.general.simulator.multistarter.BadParameterException;
import stsc.general.simulator.multistarter.MpDouble;
import stsc.general.simulator.multistarter.MpInteger;
import stsc.general.simulator.multistarter.MpString;
import stsc.general.simulator.multistarter.MpSubExecution;
import stsc.general.simulator.multistarter.grid.SimulatorSettingsGridFactory;
import stsc.general.simulator.multistarter.grid.SimulatorSettingsGridList;

/**
 * {@link ExperimentTransformer} transform {@link OrmliteOptimizerExperiment} (database experiment model) to internal experiment model (
 * {@link SimulatorSettingsGridList} ).
 */
final class ExperimentTransformer {

	final private OptimizerExperimentsDatabaseStorage optimizerExperimentsDatabaseStorage;

	ExperimentTransformer(final OptimizerExperimentsDatabaseStorage optimizerExperimentsDatabaseStorage) {
		this.optimizerExperimentsDatabaseStorage = optimizerExperimentsDatabaseStorage;
	}

	// translator

	SimulatorSettingsGridList transform(final StockStorage stockStorage, OrmliteOptimizerExperiment ormliteExperiment)
			throws SQLException, BadParameterException {
		final FromToPeriod period = new FromToPeriod(ormliteExperiment.getPeriodFrom(), ormliteExperiment.getPeriodTo());
		final SimulatorSettingsGridFactory factory = new SimulatorSettingsGridFactory(stockStorage, period);

		final List<OrmliteOptimizerExecution> loadExecutions = optimizerExperimentsDatabaseStorage.loadExecutions(ormliteExperiment);

		for (OrmliteOptimizerExecution execution : loadExecutions) {
			if (execution.getAlgorithmType().equals(AlgorithmType.STOCK_VALUE.getValue())) {
				final AlgorithmSettingsIteratorFactory algorithmFactory = transformExecution(execution);
				factory.addStock(execution.getExecutionName(), execution.getAlgorithmName(), algorithmFactory.getGridIterator());
			} else if (execution.getAlgorithmType().equals(AlgorithmType.EOD_VALUE.getValue())) {
				final AlgorithmSettingsIteratorFactory algorithmFactory = transformExecution(execution);
				factory.addEod(execution.getExecutionName(), execution.getAlgorithmName(), algorithmFactory.getGridIterator());
			} else {
				throw new InvalidParameterException("Wrong algorithm type (" + execution.getAlgorithmType() + ") for execution: " + execution.getId() + " : "
						+ execution.getExecutionName());
			}
		}

		return factory.getList();
	}

	private AlgorithmSettingsIteratorFactory transformExecution(OrmliteOptimizerExecution execution) throws SQLException, BadParameterException {
		AlgorithmSettingsIteratorFactory algorithmFactory = new AlgorithmSettingsIteratorFactory();

		final List<OrmliteOptimizerDoubleDomen> loadDoubleParameters = optimizerExperimentsDatabaseStorage.loadDoubleParameters(execution);
		for (OrmliteOptimizerDoubleDomen dp : loadDoubleParameters) {
			algorithmFactory.add(new MpDouble(dp.getParameterName(), dp.getFrom(), dp.getTo(), dp.getStep()));
		}
		final List<OrmliteOptimizerIntegerDomen> loadIntegerParameters = optimizerExperimentsDatabaseStorage.loadIntegerParameters(execution);
		for (OrmliteOptimizerIntegerDomen dp : loadIntegerParameters) {
			algorithmFactory.add(new MpInteger(dp.getParameterName(), dp.getFrom(), dp.getTo(), dp.getStep()));
		}
		final List<OrmliteOptimizerStringDomen> loadStringParameters = optimizerExperimentsDatabaseStorage.loadStringParameters(execution);
		for (OrmliteOptimizerStringDomen dp : loadStringParameters) {
			algorithmFactory.add(new MpString(dp.getParameterName(), Lists.newArrayList(dp.getParameterDomen().split("\\|"))));
		}
		final List<OrmliteOptimizerSubExecutionDomen> loadSubExecutionParameters = optimizerExperimentsDatabaseStorage
				.loadSubExecutionParameters(execution);
		for (OrmliteOptimizerSubExecutionDomen dp : loadSubExecutionParameters) {
			algorithmFactory.add(new MpSubExecution(dp.getParameterName(), Lists.newArrayList(dp.getParameterDomen().split("\\|"))));
		}
		return algorithmFactory;
	}

}
