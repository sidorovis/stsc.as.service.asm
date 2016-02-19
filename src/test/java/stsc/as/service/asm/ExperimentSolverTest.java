package stsc.as.service.asm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

import org.junit.Test;

import liquibase.exception.LiquibaseException;
import stsc.database.migrations.optimizer.OptimizerDatabaseSettings;
import stsc.database.service.storages.optimizer.OptimizerDatabaseStorage;
import stsc.storage.mocks.StockStorageMock;

public class ExperimentSolverTest {

	@Test
	public void testExperimentSolver() throws SQLException, LiquibaseException, IOException, URISyntaxException {
		final OptimizerDatabaseSettings databaseSettings = OptimizerDatabaseSettings.test().migrate();
		final OptimizerDatabaseStorage storage = new OptimizerDatabaseStorage(databaseSettings);

		final ExperimentSolver experimentSolver = new ExperimentSolver(storage, StockStorageMock.getStockStorage());

		createExperimentAtDatabase(storage);
		experimentSolver.findAndSolveExperiment();
		findExperimentResults(storage);
	}

	private void createExperimentAtDatabase(OptimizerDatabaseStorage storage) {
		// TODO Auto-generated method stub

	}

	private void findExperimentResults(OptimizerDatabaseStorage storage) {
		// TODO Auto-generated method stub

	}

}
