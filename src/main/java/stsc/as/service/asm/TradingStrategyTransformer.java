
package stsc.as.service.asm;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import stsc.common.algorithms.AlgorithmType;
import stsc.common.algorithms.EodExecution;
import stsc.common.algorithms.MutableAlgorithmConfiguration;
import stsc.common.algorithms.StockExecution;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerDoubleArgument;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerDoubleMetric;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerExecutionInstance;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerIntegerArgument;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerIntegerMetric;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerMetricsTuple;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerStringArgument;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerSubExecutionArgument;
import stsc.database.service.schemas.optimizer.trading.strategies.OrmliteOptimizerTradingStrategy;
import stsc.database.service.storages.optimizer.OptimizerTradingStrategiesDatabaseStorage;
import stsc.general.statistic.MetricType;
import stsc.general.statistic.Metrics;
import stsc.general.strategy.TradingStrategy;

public final class TradingStrategyTransformer {

	final private OptimizerTradingStrategiesDatabaseStorage storage;

	public TradingStrategyTransformer(final OptimizerTradingStrategiesDatabaseStorage storage) {
		this.storage = storage;
	}

	public void transformAndStore(final TradingStrategy ts) throws SQLException {
		final OrmliteOptimizerTradingStrategy ormliteTradingStrategy = new OrmliteOptimizerTradingStrategy();
		ormliteTradingStrategy.setPeriodFrom(ts.getSettings().getInit().getPeriod().getFrom());
		ormliteTradingStrategy.setPeriodTo(ts.getSettings().getInit().getPeriod().getTo());
		storage.saveTradingStrategy(ormliteTradingStrategy);
		final List<StockExecution> stockExecutions = ts.getSettings().getInit().getExecutionsStorage().getStockExecutions();
		int index = 0;
		for (StockExecution se : stockExecutions) {
			transformAndStore(ormliteTradingStrategy.getId(), se, AlgorithmType.STOCK_VALUE.name(), index++);
		}
		final List<EodExecution> eodExecutions = ts.getSettings().getInit().getExecutionsStorage().getEodExecutions();
		for (EodExecution se : eodExecutions) {
			transformAndStore(ormliteTradingStrategy.getId(), se, AlgorithmType.EOD_VALUE.name(), index++);
		}
		transformAndStore(ormliteTradingStrategy.getId(), ts.getMetrics());
	}

	private void transformAndStore(Integer tradingStrategyId, Metrics metrics) throws SQLException {
		final OrmliteOptimizerMetricsTuple metricsTuple = new OrmliteOptimizerMetricsTuple(tradingStrategyId);
		storage.saveMetricsTuple(metricsTuple);
		for (Entry<MetricType, Integer> v : metrics.getIntegerMetrics().entrySet()) {
			final OrmliteOptimizerIntegerMetric integerMetric = new OrmliteOptimizerIntegerMetric(metricsTuple.getId());
			integerMetric.setMetricType(v.getKey().name());
			integerMetric.setMetricValue(v.getValue());
			storage.saveIntegerMetric(integerMetric);
		}
		for (Entry<MetricType, Double> v : metrics.getDoubleMetrics().entrySet()) {
			final OrmliteOptimizerDoubleMetric doubleMetric = new OrmliteOptimizerDoubleMetric(metricsTuple.getId());
			doubleMetric.setMetricType(v.getKey().name());
			doubleMetric.setMetricValue(v.getValue());
			storage.saveDoubleMetric(doubleMetric);
		}
	}

	private void transformAndStore(int tradingStrategyId, StockExecution se, String algorithmType, int index) throws SQLException {
		final OrmliteOptimizerExecutionInstance ormliteExecutionInstance = new OrmliteOptimizerExecutionInstance(tradingStrategyId);
		ormliteExecutionInstance.setAlgorithmName(se.getAlgorithmName());
		ormliteExecutionInstance.setExecutionInstanceName(se.getExecutionName());
		ormliteExecutionInstance.setIndexNumber(index);
		ormliteExecutionInstance.setAlgorithmType(algorithmType);
		storage.saveExecutionInstance(ormliteExecutionInstance);
		transformAndStoreSettings(ormliteExecutionInstance.getId(), se.getSettings());
	}

	private void transformAndStore(int tradingStrategyId, EodExecution se, String algorithmType, int index) throws SQLException {
		final OrmliteOptimizerExecutionInstance ormliteExecutionInstance = new OrmliteOptimizerExecutionInstance(tradingStrategyId);
		ormliteExecutionInstance.setAlgorithmName(se.getAlgorithmName());
		ormliteExecutionInstance.setExecutionInstanceName(se.getExecutionName());
		ormliteExecutionInstance.setIndexNumber(index);
		ormliteExecutionInstance.setAlgorithmType(algorithmType);
		storage.saveExecutionInstance(ormliteExecutionInstance);
		transformAndStoreSettings(ormliteExecutionInstance.getId(), se.getSettings());
	}

	private void transformAndStoreSettings(Integer id, MutableAlgorithmConfiguration settings) throws SQLException {
		for (Map.Entry<String, Integer> v : settings.getIntegers().entrySet()) {
			final OrmliteOptimizerIntegerArgument argument = new OrmliteOptimizerIntegerArgument(id);
			argument.setParameterName(v.getKey());
			argument.setParameterValue(v.getValue());
			storage.saveIntegerArgument(argument);
		}
		for (Map.Entry<String, Double> v : settings.getDoubles().entrySet()) {
			final OrmliteOptimizerDoubleArgument argument = new OrmliteOptimizerDoubleArgument(id);
			argument.setParameterName(v.getKey());
			argument.setParameterValue(v.getValue());
			storage.saveDoubleArgument(argument);
		}
		for (Map.Entry<String, String> v : settings.getStrings().entrySet()) {
			final OrmliteOptimizerStringArgument argument = new OrmliteOptimizerStringArgument(id);
			argument.setParameterName(v.getKey());
			argument.setParameterValue(v.getValue());
			storage.saveStringArgument(argument);
		}
		for (String v : settings.getSubExecutions()) {
			final OrmliteOptimizerSubExecutionArgument argument = new OrmliteOptimizerSubExecutionArgument(id);
			argument.setSubExecutionName(v);
			storage.saveSubExecutionArgument(argument);
		}
	}
}
