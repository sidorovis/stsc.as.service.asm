
package stsc.as.service.asm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import liquibase.exception.LiquibaseException;
import stsc.algorithms.Input;
import stsc.algorithms.indices.primitive.stock.Diff;
import stsc.algorithms.indices.primitive.stock.Level;
import stsc.algorithms.indices.primitive.stock.Sma;
import stsc.algorithms.primitive.eod.OpenWhileSignalAlgorithm;
import stsc.common.Day;
import stsc.common.algorithms.AlgorithmType;
import stsc.database.migrations.optimizer.OptimizerDatabaseSettings;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerDoubleDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExecution;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExperiment;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerIntegerDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerStringDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerSubExecutionDomen;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerDoubleMetric;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerEquityCurveValue;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerIntegerMetric;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerMetricsTuple;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerTradingStrategy;
import stsc.database.service.storages.optimizer.OptimizerDatabaseStorage;
import stsc.general.statistic.MetricType;
import stsc.storage.mocks.StockStorageMock;

public class ExperimentSolverTest {

	@Test
	public void testExperimentSolver() throws SQLException, LiquibaseException, IOException, URISyntaxException, ParseException {
		final OptimizerDatabaseSettings databaseSettings = OptimizerDatabaseSettings.test().migrate();
		final OptimizerDatabaseStorage storage = new OptimizerDatabaseStorage(databaseSettings);

		final ExperimentSolver experimentSolver = new ExperimentSolver(storage, StockStorageMock.getStockStorage());

		createExperimentAtDatabase(storage);
		experimentSolver.findAndSolveExperiment();
		findExperimentResults(storage);
	}

	private void createExperimentAtDatabase(final OptimizerDatabaseStorage storage) throws SQLException, ParseException {
		final OrmliteOptimizerExperiment experiment = new OrmliteOptimizerExperiment("testExperiment", "somedata");
		experiment.setPeriodFrom(Day.createDate("01-01-2010"));
		experiment.setPeriodTo(Day.createDate("01-01-2016"));
		storage.getExperimentsStorage().saveExperiment(experiment);
		
		int executionIndex = 0;

		final OrmliteOptimizerExecution a1 = new OrmliteOptimizerExecution(experiment.getId(), executionIndex++);
		a1.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		a1.setAlgorithmName(Input.class.getName());
		a1.setExecutionName("a1");
		storage.getExperimentsStorage().saveExecution(a1);

		final OrmliteOptimizerExecution a2 = new OrmliteOptimizerExecution(experiment.getId(), executionIndex++);
		a2.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		a2.setAlgorithmName(Input.class.getName());
		a2.setExecutionName("a2");
		storage.getExperimentsStorage().saveExecution(a2);

		final OrmliteOptimizerStringDomen e = new OrmliteOptimizerStringDomen(a2.getId());
		e.setParameterName("e");
		e.setParameterDomen("high|low|close");
		storage.getExperimentsStorage().saveStringParameter(e);

		final OrmliteOptimizerExecution sma = new OrmliteOptimizerExecution(experiment.getId(), executionIndex++);
		sma.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		sma.setAlgorithmName(Sma.class.getName());
		sma.setExecutionName("sma");
		storage.getExperimentsStorage().saveExecution(sma);

		final OrmliteOptimizerIntegerDomen smaN = new OrmliteOptimizerIntegerDomen(sma.getId());
		smaN.setParameterName("N");
		smaN.setFrom(5);
		smaN.setStep(7);
		smaN.setTo(16);
		storage.getExperimentsStorage().saveIntegerParameter(smaN);

		final OrmliteOptimizerSubExecutionDomen smaSub = new OrmliteOptimizerSubExecutionDomen(sma.getId());
		smaSub.setParameterName("internal");
		smaSub.setParameterDomen("a2");
		storage.getExperimentsStorage().saveSubExecutionParameter(smaSub);

		final OrmliteOptimizerExecution d = new OrmliteOptimizerExecution(experiment.getId(), executionIndex++);
		d.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		d.setAlgorithmName(Diff.class.getName());
		d.setExecutionName("diff");
		storage.getExperimentsStorage().saveExecution(d);

		final OrmliteOptimizerSubExecutionDomen sub1 = new OrmliteOptimizerSubExecutionDomen(d.getId());
		sub1.setParameterName("a1");
		sub1.setParameterDomen("a1");
		storage.getExperimentsStorage().saveSubExecutionParameter(sub1);

		final OrmliteOptimizerSubExecutionDomen sub2 = new OrmliteOptimizerSubExecutionDomen(d.getId());
		sub2.setParameterName("a2");
		sub2.setParameterDomen("sma");
		storage.getExperimentsStorage().saveSubExecutionParameter(sub2);

		final OrmliteOptimizerExecution level = new OrmliteOptimizerExecution(experiment.getId(), executionIndex++);
		level.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		level.setAlgorithmName(Level.class.getName());
		level.setExecutionName("level");
		storage.getExperimentsStorage().saveExecution(level);

		final OrmliteOptimizerDoubleDomen f = new OrmliteOptimizerDoubleDomen(level.getId());
		f.setParameterName("f");
		f.setFrom(0.1);
		f.setStep(0.25);
		f.setTo(0.9);
		storage.getExperimentsStorage().saveDoubleParameter(f);

		final OrmliteOptimizerSubExecutionDomen pd = new OrmliteOptimizerSubExecutionDomen(level.getId());
		pd.setParameterName("diff");
		pd.setParameterDomen("diff");
		storage.getExperimentsStorage().saveSubExecutionParameter(pd);

		final OrmliteOptimizerExecution openWhile = new OrmliteOptimizerExecution(experiment.getId(), executionIndex++);
		openWhile.setAlgorithmType(AlgorithmType.EOD_VALUE.getValue());
		openWhile.setAlgorithmName(OpenWhileSignalAlgorithm.class.getName());
		openWhile.setExecutionName("trading_algorithm");
		storage.getExperimentsStorage().saveExecution(openWhile);

		final OrmliteOptimizerSubExecutionDomen pl = new OrmliteOptimizerSubExecutionDomen(openWhile.getId());
		pl.setParameterName("level");
		pl.setParameterDomen("level");
		storage.getExperimentsStorage().saveSubExecutionParameter(pl);

		experiment.setCommited();
		storage.getExperimentsStorage().saveExperiment(experiment);
	}

	private void findExperimentResults(final OptimizerDatabaseStorage storage) throws SQLException {
		final List<OrmliteOptimizerTradingStrategy> loadTradingStrategies = storage.getTradingStrategiesStorage().loadTradingStrategies();
		loadTradingStrategies.forEach((ts) -> {
			try {
				final List<OrmliteOptimizerMetricsTuple> metricsTuples = storage.getTradingStrategiesStorage().loadMetricsTuples(ts);
				Assert.assertEquals(1, metricsTuples.size());
				final List<OrmliteOptimizerDoubleMetric> loadDoubleMetrics = storage.getTradingStrategiesStorage().loadDoubleMetrics(metricsTuples.get(0));
				final List<OrmliteOptimizerIntegerMetric> loadIntegerMetrics = storage.getTradingStrategiesStorage().loadIntegerMetrics(metricsTuples.get(0));
				Assert.assertEquals(MetricType.values().length, loadDoubleMetrics.size() + loadIntegerMetrics.size());
				final List<OrmliteOptimizerEquityCurveValue> loadEquityCurveValues = storage.getTradingStrategiesStorage()
						.loadEquityCurveValues(metricsTuples.get(0));
				Assert.assertEquals(1463, loadEquityCurveValues.size());
			} catch (Exception e) {
			}
		});
		Assert.assertEquals(24, loadTradingStrategies.size());
	}

}
