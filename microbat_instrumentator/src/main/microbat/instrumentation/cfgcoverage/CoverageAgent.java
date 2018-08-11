package microbat.instrumentation.cfgcoverage;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.CommandLine;
import microbat.instrumentation.IAgent;
import microbat.instrumentation.cfgcoverage.CoverageAgentParams.CoverageCollectionType;
import microbat.instrumentation.cfgcoverage.graph.CoverageGraphConstructor;
import microbat.instrumentation.cfgcoverage.graph.CoverageSFlowGraph;
import microbat.instrumentation.cfgcoverage.instr.CoverageInstrumenter;
import microbat.instrumentation.cfgcoverage.instr.CoverageTransformer;
import microbat.instrumentation.cfgcoverage.instr.MethodInstructionsInfo;
import microbat.instrumentation.cfgcoverage.output.CoverageOutputWriter;
import microbat.instrumentation.cfgcoverage.runtime.AgentRuntimeData;
import microbat.instrumentation.cfgcoverage.runtime.value.ValueExtractor;
import microbat.instrumentation.filter.FilterChecker;
import sav.common.core.utils.StopTimer;
import sav.strategies.dto.AppJavaClassPath;

public class CoverageAgent implements IAgent {
	private CoverageAgentParams agentParams;
	private CoverageInstrumenter instrumenter;
	private List<String> testcases = new ArrayList<String>();
	private CoverageTransformer coverageTransformer;
	private StopTimer timer;
	private ICoverageTracerHandler tracerHandler;
	
	public CoverageAgent(CommandLine cmd) {
		this.agentParams = new CoverageAgentParams(cmd);
		coverageTransformer = new CoverageTransformer(agentParams);
		instrumenter = coverageTransformer.getInstrumenter();
		switch (agentParams.getCoverageType()) {
		case BRANCH_COVERAGE:
			tracerHandler = new BranchCoverageTracerHandler();
			break;
		case UNCIRCLE_CFG_COVERAGE:
			tracerHandler = new CFGCoverageHandler();
			break;
		}
	}

	@Override
	public void startup(long vmStartupTime, long agentPreStartup) {
		timer = new AgentStopTimer("Tracing program for coverage", vmStartupTime, agentPreStartup);
		timer.newPoint("initGraph");
		AppJavaClassPath appClasspath = agentParams.initAppClasspath();
		FilterChecker.setup(appClasspath, null, null);
		ValueExtractor.variableLayer = agentParams.getVarLayer();
		CoverageGraphConstructor constructor = new CoverageGraphConstructor();
		CoverageSFlowGraph coverageFlowGraph = constructor.buildCoverageGraph(appClasspath,
				agentParams.getTargetMethod(), agentParams.getCdgLayer(), agentParams.getInclusiveMethodIds(),
				agentParams.getCoverageType() == CoverageCollectionType.UNCIRCLE_CFG_COVERAGE);
		AgentRuntimeData.coverageFlowGraph = coverageFlowGraph;
		MethodInstructionsInfo.initInstrInstructions(coverageFlowGraph);
		instrumenter.setEntryPoint(coverageFlowGraph.getStartNode().getStartNodeId().getMethodId());
		timer.newPoint("Execution");
	}

	@Override
	public void shutdown() throws Exception {
		timer.newPoint("Saving coverage");
		AgentLogger.debug("Saving coverage...");
		CoverageOutput coverageOutput = tracerHandler.getCoverageOutput();
		if (agentParams.getDumpFile() != null) {
			coverageOutput.saveToFile(agentParams.getDumpFile());
		}
		AgentLogger.debug(timer.getResultString());
	}
	
	public static void _storeCoverage(OutputStream outStream, Boolean reset) {
		AgentLogger.debug("Saving coverage...");
		CoverageAgent coverageAgent = (CoverageAgent) Agent.getAgent();
		CoverageOutput coverageOutput = coverageAgent.tracerHandler.getCoverageOutput();
		CoverageOutputWriter coverageOutputWriter = new CoverageOutputWriter(outStream);
		try {
			synchronized (coverageOutput.getCoverageGraph()) {
				coverageOutputWriter.writeCfgCoverage(coverageOutput.getCoverageGraph());
				coverageOutputWriter.writeInputData(coverageOutput.getInputData());
				coverageOutputWriter.flush();
			}
		} catch (IOException e) {
			AgentLogger.error(e);
			e.printStackTrace();
		} finally {
			try {
//				coverageOutputWriter.close();
			} catch(Exception e) {
				// do nothing
			}
		}
		if (reset) {
			coverageAgent.tracerHandler.reset();
		}
	}

	@Override
	public synchronized void startTest(String junitClass, String junitMethod) {
		int testIdx = testcases.size();
		String testcase = InstrumentationUtils.getMethodId(junitClass, junitMethod);
		AgentLogger.debug(String.format("Start testcase %s, testIdx=%s", testcase, testIdx));
		testcases.add(testcase);
		AgentRuntimeData.currentTestIdxMap.put(Thread.currentThread().getId(), testIdx);
		AgentRuntimeData.coverageFlowGraph.addCoveredTestcase(testcase, testIdx);
	}
	
	@Override
	public void exitTest(String testResultMsg, String junitClass, String junitMethod, long threadId) {
		AgentLogger.debug(String.format("End testcase %s, testIdx=%s, thread=%s",
				InstrumentationUtils.getMethodId(junitClass, junitMethod),
				AgentRuntimeData.currentTestIdxMap.get(threadId), threadId));
	}

	@Override
	public void finishTest(String junitClass, String junitMethod) {
		// do nothing for now.
	}
	
	@Override
	public ClassFileTransformer getTransformer() {
		return coverageTransformer;
	}

	@Override
	public void retransformBootstrapClasses(Instrumentation instrumentation, Class<?>[] retransformableClasses)
			throws Exception {
		// do nothing for now.
	}

	@Override
	public boolean isInstrumentationActive() {
		return true;
	}

	public static interface ICoverageTracerHandler {

		CoverageOutput getCoverageOutput();

		void reset();
		
	}
}
