package microbat.instrumentation.cfgcoverage.runtime;

import microbat.instrumentation.runtime.TracingState;

public interface ICoverageTracer {

	void _reachNode(String methodId, int nodeIdx);

	void enterMethod(String methodId, String paramTypeSignsCode, String paramNamesCode, Object[] params, boolean isEntryPoint);

	void _exitMethod(String methodId, boolean isEntryPoint);

	void _onIfACmp(Object value1, Object value2, String methodId, int nodeIdx);

	void _onIfICmp(int value1, int value2, String methodId, int nodeIdx);

	void _onIf(int value, String methodId, int nodeIdx);

	void _onIfNull(Object value, String methodId, int nodeIdx);

	void setState(TracingState recording);

	TracingState getState();

	boolean doesNotNeedToRecord(String methodId);


}
