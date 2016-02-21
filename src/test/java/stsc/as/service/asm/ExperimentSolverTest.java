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
import stsc.algorithms.primitive.eod.OpenWhileSignalAlgorithm;
import stsc.common.Day;
import stsc.common.algorithms.AlgorithmType;
import stsc.database.migrations.optimizer.OptimizerDatabaseSettings;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerDoubleParameter;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExecution;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExperiment;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerStringParameter;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerSubExecutionParameter;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerTradingStrategy;
import stsc.database.service.storages.optimizer.OptimizerDatabaseStorage;
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

		final OrmliteOptimizerExecution a1 = new OrmliteOptimizerExecution(experiment.getId(), 0);
		a1.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		a1.setAlgorithmName(Input.class.getName());
		a1.setExecutionName("a1");
		storage.getExperimentsStorage().saveExecution(a1);

		final OrmliteOptimizerExecution a2 = new OrmliteOptimizerExecution(experiment.getId(), 1);
		a2.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		a2.setAlgorithmName(Input.class.getName());
		a2.setExecutionName("a2");
		storage.getExperimentsStorage().saveExecution(a2);

		final OrmliteOptimizerStringParameter e = new OrmliteOptimizerStringParameter(a2.getId());
		e.setParameterName("e");
		e.setParameterDomen("open");
		storage.getExperimentsStorage().saveStringParameter(e);

		final OrmliteOptimizerExecution d = new OrmliteOptimizerExecution(experiment.getId(), 2);
		d.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		d.setAlgorithmName(Diff.class.getName());
		d.setExecutionName("diff");
		storage.getExperimentsStorage().saveExecution(d);

		final OrmliteOptimizerSubExecutionParameter sub1 = new OrmliteOptimizerSubExecutionParameter(d.getId());
		sub1.setParameterName("a1");
		sub1.setParameterDomen("a1");
		storage.getExperimentsStorage().saveSubExecutionParameter(sub1);

		final OrmliteOptimizerSubExecutionParameter sub2 = new OrmliteOptimizerSubExecutionParameter(d.getId());
		sub2.setParameterName("a2");
		sub2.setParameterDomen("a2");
		storage.getExperimentsStorage().saveSubExecutionParameter(sub2);

		final OrmliteOptimizerExecution level = new OrmliteOptimizerExecution(experiment.getId(), 3);
		level.setAlgorithmType(AlgorithmType.STOCK_VALUE.getValue());
		level.setAlgorithmName(Level.class.getName());
		level.setExecutionName("level");
		storage.getExperimentsStorage().saveExecution(level);

		final OrmliteOptimizerDoubleParameter f = new OrmliteOptimizerDoubleParameter(level.getId());
		f.setParameterName("f");
		f.setFrom(0.1);
		f.setStep(0.1);
		f.setTo(0.9);
		storage.getExperimentsStorage().saveDoubleParameter(f);

		final OrmliteOptimizerSubExecutionParameter pd = new OrmliteOptimizerSubExecutionParameter(level.getId());
		pd.setParameterName("diff");
		pd.setParameterDomen("diff");
		storage.getExperimentsStorage().saveSubExecutionParameter(pd);

		final OrmliteOptimizerExecution openWhile = new OrmliteOptimizerExecution(experiment.getId(), 4);
		openWhile.setAlgorithmType(AlgorithmType.EOD_VALUE.getValue());
		openWhile.setAlgorithmName(OpenWhileSignalAlgorithm.class.getName());
		openWhile.setExecutionName("trading_algorithm");
		storage.getExperimentsStorage().saveExecution(openWhile);

		final OrmliteOptimizerSubExecutionParameter pl = new OrmliteOptimizerSubExecutionParameter(openWhile.getId());
		pl.setParameterName("level");
		pl.setParameterDomen("level");
		storage.getExperimentsStorage().saveSubExecutionParameter(pl);

		experiment.setCommited();
		storage.getExperimentsStorage().saveExperiment(experiment);
	}

	private void findExperimentResults(final OptimizerDatabaseStorage storage) throws SQLException {
		final List<OrmliteOptimizerTradingStrategy> loadTradingStrategies = storage.getTradingStrategiesStorage().loadTradingStrategies();
		Assert.assertEquals(8, loadTradingStrategies.size());
	}

}
