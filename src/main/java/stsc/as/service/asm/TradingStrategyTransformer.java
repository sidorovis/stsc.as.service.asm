
package stsc.as.service.asm;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import stsc.common.algorithms.AlgorithmType;
import stsc.common.algorithms.EodExecutionInstance;
import stsc.common.algorithms.MutableAlgorithmConfiguration;
import stsc.common.algorithms.StockExecutionInstance;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerDoubleDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExecution;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerExperiment;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerIntegerDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerStringDomen;
import stsc.database.service.schemas.optimizer.experiments.OrmliteOptimizerSubExecutionDomen;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerDoubleArgument;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerDoubleMetric;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerEquityCurveValue;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerExecutionInstance;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerIntegerArgument;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerIntegerMetric;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerMetricsTuple;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerStringArgument;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerSubExecutionArgument;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerTradingStrategy;
import stsc.database.service.storages.optimizer.OptimizerDatabaseStorage;
import stsc.database.service.storages.optimizer.OptimizerExperimentsDatabaseStorage;
import stsc.database.service.storages.optimizer.OptimizerTradingStrategiesDatabaseStorage;
import stsc.general.statistic.EquityCurve;
import stsc.general.statistic.MetricType;
import stsc.general.statistic.Metrics;
import stsc.general.strategy.TradingStrategy;

/**
 * This class transform {@link TradingStrategy} from internal model to database model and store that class to database.
 */
final class TradingStrategyTransformer {

	final private OptimizerExperimentsDatabaseStorage experimentsDatabaseStorage;
	final private OptimizerTradingStrategiesDatabaseStorage tradingStrategiesStorage;
	final private OrmliteOptimizerExperiment experiment;

	public TradingStrategyTransformer(final OptimizerDatabaseStorage optimizerDatabaseStorage, final OrmliteOptimizerExperiment ormliteOptimizerExperiment) {
		this.experimentsDatabaseStorage = optimizerDatabaseStorage.getExperimentsStorage();
		this.tradingStrategiesStorage = optimizerDatabaseStorage.getTradingStrategiesStorage();
		this.experiment = ormliteOptimizerExperiment;
	}

	public void transformAndStore(final TradingStrategy ts) throws SQLException {
		final OrmliteOptimizerTradingStrategy ormliteTradingStrategy = new OrmliteOptimizerTradingStrategy(experiment.getId());
		ormliteTradingStrategy.setPeriodFrom(ts.getSettings().getInit().getPeriod().getFrom());
		ormliteTradingStrategy.setPeriodTo(ts.getSettings().getInit().getPeriod().getTo());
		tradingStrategiesStorage.saveTradingStrategy(ormliteTradingStrategy);

		final List<OrmliteOptimizerExecution> executions = experimentsDatabaseStorage.loadExecutions(experiment);

		final List<StockExecutionInstance> stockExecutions = ts.getSettings().getInit().getExecutionsStorage().getStockExecutions();
		int index = 0;
		for (StockExecutionInstance se : stockExecutions) {
			transformAndStore(executions, ormliteTradingStrategy.getId(), se, AlgorithmType.STOCK_VALUE.getValue(), index++);
		}
		final List<EodExecutionInstance> eodExecutions = ts.getSettings().getInit().getExecutionsStorage().getEodExecutions();
		for (EodExecutionInstance se : eodExecutions) {
			transformAndStore(executions, ormliteTradingStrategy.getId(), se, AlgorithmType.EOD_VALUE.getValue(), index++);
		}
		transformAndStore(ormliteTradingStrategy.getId(), ts.getMetrics());
	}

	private void transformAndStore(final int tradingStrategyId, final Metrics metrics) throws SQLException {
		final OrmliteOptimizerMetricsTuple metricsTuple = new OrmliteOptimizerMetricsTuple(tradingStrategyId);
		tradingStrategiesStorage.saveMetricsTuple(metricsTuple);
		for (Entry<MetricType, Integer> v : metrics.getIntegerMetrics().entrySet()) {
			final OrmliteOptimizerIntegerMetric integerMetric = new OrmliteOptimizerIntegerMetric(metricsTuple.getId());
			integerMetric.setMetricType(v.getKey().name());
			integerMetric.setMetricValue(v.getValue());
			tradingStrategiesStorage.saveIntegerMetric(integerMetric);
		}
		for (Entry<MetricType, Double> v : metrics.getDoubleMetrics().entrySet()) {
			final OrmliteOptimizerDoubleMetric doubleMetric = new OrmliteOptimizerDoubleMetric(metricsTuple.getId());
			doubleMetric.setMetricType(v.getKey().name());
			doubleMetric.setMetricValue(v.getValue());
			tradingStrategiesStorage.saveDoubleMetric(doubleMetric);
		}
		final EquityCurve equityCurveInMoney = metrics.getEquityCurveInMoney();
		for (int i = 0; i < equityCurveInMoney.size(); ++i) {
			final OrmliteOptimizerEquityCurveValue equityCurveValue = new OrmliteOptimizerEquityCurveValue(metricsTuple.getId());
			equityCurveValue.setTimestamp(equityCurveInMoney.get(i).date);
			equityCurveValue.setValue(equityCurveInMoney.get(i).value);
			tradingStrategiesStorage.saveEquityCurveValue(equityCurveValue);
		}
	}

	private void transformAndStore(List<OrmliteOptimizerExecution> executions, int tradingStrategyId, StockExecutionInstance se, String algorithmType,
			int index) throws SQLException {
		final OrmliteOptimizerExecution execution = executions.get(index);
		final OrmliteOptimizerExecutionInstance ormliteExecutionInstance = new OrmliteOptimizerExecutionInstance(tradingStrategyId);
		ormliteExecutionInstance.setAlgorithmName(se.getAlgorithmName());
		ormliteExecutionInstance.setExecutionInstanceName(se.getExecutionName());
		ormliteExecutionInstance.setIndexNumber(index);
		ormliteExecutionInstance.setAlgorithmType(algorithmType);
		tradingStrategiesStorage.saveExecutionInstance(ormliteExecutionInstance);
		transformAndStoreSettings(execution, ormliteExecutionInstance.getId(), se.getSettings());
	}

	private void transformAndStore(List<OrmliteOptimizerExecution> executions, int tradingStrategyId, EodExecutionInstance se, String algorithmType, int index) throws SQLException {
		final OrmliteOptimizerExecution execution = executions.get(index);
		final OrmliteOptimizerExecutionInstance ormliteExecutionInstance = new OrmliteOptimizerExecutionInstance(tradingStrategyId);
		ormliteExecutionInstance.setAlgorithmName(se.getAlgorithmName());
		ormliteExecutionInstance.setExecutionInstanceName(se.getExecutionName());
		ormliteExecutionInstance.setIndexNumber(index);
		ormliteExecutionInstance.setAlgorithmType(algorithmType);
		tradingStrategiesStorage.saveExecutionInstance(ormliteExecutionInstance);
		transformAndStoreSettings(execution, ormliteExecutionInstance.getId(), se.getSettings());
	}

	private void transformAndStoreSettings(final OrmliteOptimizerExecution execution, Integer executionInstanceId, MutableAlgorithmConfiguration settings)
			throws SQLException {
		final List<OrmliteOptimizerIntegerDomen> loadIntegerParameters = experimentsDatabaseStorage.loadIntegerParameters(execution);
		for(OrmliteOptimizerIntegerDomen p : loadIntegerParameters) {
			final int value = settings.getIntegers().get(p.getParameterName());
			final OrmliteOptimizerIntegerArgument argument = new OrmliteOptimizerIntegerArgument(executionInstanceId);
			argument.setIntegerDomenId(p.getId());
			final int index = (value - p.getFrom()) / (p.getStep());
			argument.setArgumentIndex(index);
			tradingStrategiesStorage.saveIntegerArgument(argument);
		}
		final List<OrmliteOptimizerDoubleDomen> loadDoubleParameters = experimentsDatabaseStorage.loadDoubleParameters(execution);
		for(OrmliteOptimizerDoubleDomen p : loadDoubleParameters) {
			final double value = settings.getDoubles().get(p.getParameterName());
			final OrmliteOptimizerDoubleArgument argument = new OrmliteOptimizerDoubleArgument(executionInstanceId);
			argument.setDoubleDomenId(p.getId());
			final int index = (int)((value - p.getFrom()) / (p.getStep()));
			argument.setArgumentIndex(index);
			tradingStrategiesStorage.saveDoubleArgument(argument);
		}
		final List<OrmliteOptimizerStringDomen> loadStringParameters = experimentsDatabaseStorage.loadStringParameters(execution);
		for(OrmliteOptimizerStringDomen p : loadStringParameters) {
			final String value = settings.getStrings().get(p.getParameterName());
			final OrmliteOptimizerStringArgument argument = new OrmliteOptimizerStringArgument(executionInstanceId);
			argument.setStringDomenId(p.getId());
			final int index = Arrays.asList(p.getParameterDomen().split("\\|")).indexOf(value);
			argument.setArgumentIndex(index);
			tradingStrategiesStorage.saveStringArgument(argument);
		}
		final List<OrmliteOptimizerSubExecutionDomen> loadSubExecutionParameters = experimentsDatabaseStorage.loadSubExecutionParameters(execution);
		int subExecutionIndex = 0;
		for(OrmliteOptimizerSubExecutionDomen p : loadSubExecutionParameters) {
			final String value = settings.getSubExecutions().get(subExecutionIndex++);
			final OrmliteOptimizerSubExecutionArgument argument = new OrmliteOptimizerSubExecutionArgument(executionInstanceId);
			argument.setSubExecutionDomenId(p.getId());
			final int index = Arrays.asList(p.getParameterDomen().split("\\|")).indexOf(value);
			argument.setArgumentIndex(index);
			tradingStrategiesStorage.saveSubExecutionArgument(argument);
		}
	}
}
