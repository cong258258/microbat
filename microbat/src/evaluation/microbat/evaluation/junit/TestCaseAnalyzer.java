package microbat.evaluation.junit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdi.TimeoutException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import microbat.evaluation.SimulatedMicroBat;
import microbat.evaluation.TraceModelConstructor;
import microbat.evaluation.io.ExcelReporter;
import microbat.evaluation.io.IgnoredTestCaseFiles;
import microbat.evaluation.model.Trial;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;
import microbat.util.JTestUtil;
import microbat.util.JavaUtil;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import mutation.mutator.Mutator;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.dto.ClassLocation;
import sav.strategies.mutanbug.MutationResult;

public class TestCaseAnalyzer {
	
	public static final String TEST_RUNNER = "microbat.evaluation.junit.MicroBatTestRunner";
	private static final String TMP_DIRECTORY = "C:\\microbat_evaluation\\";
	
//	private List<Trial> trials = new ArrayList<>();
//	private List<Trial> overLongTrials = new ArrayList<>();
	private IgnoredTestCaseFiles ignoredTestCaseFiles;
	private ParsedTrials parsedTrials;
	
//	private List<String> errorMsgs = new ArrayList<>();
//	private int trialFileNum = 0;
	private int trialNumPerTestCase = 3;
	
	private double[] unclearRates = {0, 0.005, 0.01, 0.05, 0.1, -1};
//	private double[] unclearRates = {0};
	
	public TestCaseAnalyzer(){
	}
	
	public void test(){
		String str = "C:\\Users\\YUNLIN~1\\AppData\\Local\\Temp\\mutatedSource8245811234241496344\\47_25_1\\Main.java";
		File file = new File(str);
		
		try {
			String content = FileUtils.readFileToString(file);
			
			ICompilationUnit unit = JavaUtil.findICompilationUnitInProject("com.Main");
			unit.getBuffer().setContents(content);
			unit.save(new NullProgressMonitor(), true);
			
			IProject project = JavaUtil.getSpecificJavaProjectInWorkspace();
			try {
				project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
			} catch (CoreException e) {
				e.printStackTrace();
			}
			
			System.currentTimeMillis();
			
		} catch (IOException | JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, MutationResult> generateMutationFiles(List<ClassLocation> locationList){
		ClassLocation cl = locationList.get(0);
		String cName = cl.getClassCanonicalName();
		ICompilationUnit unit = JavaUtil.findICompilationUnitInProject(cName);
		URI uri = unit.getResource().getLocationURI();
		String sourceFolderPath = uri.toString();
		cName = cName.replace(".", "/") + ".java";
		
		sourceFolderPath = sourceFolderPath.substring(0, sourceFolderPath.indexOf(cName));
		sourceFolderPath = sourceFolderPath.substring(5, sourceFolderPath.length());
		
		cleanClassInTestPackage(sourceFolderPath, locationList);
		System.currentTimeMillis();
		
		Mutator mutator = new Mutator(sourceFolderPath, TMP_DIRECTORY);
		Map<String, MutationResult> mutations = mutator.mutate(locationList);
		
		return mutations;
	}
	
	private void cleanClassInTestPackage(String sourceFolderPath,
			List<ClassLocation> locationList) {
		Iterator<ClassLocation> iterator = locationList.iterator();
		while(iterator.hasNext()){
			ClassLocation location = iterator.next();
			String className = location.getClassCanonicalName();
			String fileName  = sourceFolderPath + className.replace(".", "/") + ".java";
			
			File file = new File(fileName);
			if(!file.exists()){
				iterator.remove();
			}
		}
	}

	public void runEvaluation() throws JavaModelException{
		ignoredTestCaseFiles = new IgnoredTestCaseFiles();
		parsedTrials = new ParsedTrials();
		
		ExcelReporter reporter = new ExcelReporter(Settings.projectName+".xlsx", this.unclearRates);
		
		IPackageFragmentRoot testRoot = JavaUtil.findTestPackageRootInProject();
		
		for(IJavaElement element: testRoot.getChildren()){
			if(element instanceof IPackageFragment){
				runEvaluation((IPackageFragment)element, reporter);				
			}
		}
		
//		runSingeTrial();
		
//		String className = "org.apache.commons.math.analysis.polynomials.PolynomialFunctionLagrangeFormTest";
//		String methodName = "testLinearFunction";
//		runEvaluationForSingleTestCase(className, methodName, reporter);
	}
	
	private void runSingeTrial(){
		//TODO BUG TimeOutException in JVM
//		String testClassName = "org.apache.commons.math.analysis.interpolation.LinearInterpolatorTest";
//		String testMethodName = "testInterpolateLinear";
//		String mutationFile = "C:\\Users\\YUNLIN~1\\AppData\\Local\\Temp\\"
//				+ "apache-common-math-2.2\\2081_22_1\\MathUtils.java";
//		String mutatedClass = "org.apache.commons.math.util.MathUtils";
		
//		String testClassName = "test.SimpleCalculatorTest";
//		String testMethodName = "testCalculator";
//		String mutationFile = "C:\\microbat_evaluation\\mutation\\73_70_1\\SimpleCalculator.java";
//		double unclearRate = 0;
//		boolean enableLoopInference = true;
		
		String testClassName = "org.apache.commons.math.analysis.BinaryFunctionTest";
		String testMethodName = "testPow";
		String mutationFile = "C:\\microbat_evaluation\\apache-common-math-2.2\\1642_41_1\\FastMath.java";
		double unclearRate = 0;
		boolean enableLoopInference = true;
		
		try {
			runEvaluationForSingleTrial(testClassName, testMethodName, mutationFile, 
					unclearRate, enableLoopInference);
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

	private void runEvaluationForSingleTrial(String testClassName,
			String testMethodName, String mutationFile, double unclearRate, boolean enableLoopInference) 
					throws JavaModelException, MalformedURLException, IOException {
		String testcaseName = testClassName + "#" + testMethodName;
		AppJavaClassPath testcaseConfig = createProjectClassPath(testClassName, testMethodName);
		
		File mutatedFile = new File(mutationFile);
		
		String[] sections = mutationFile.split("\\\\");
		String mutatedLineString = sections[sections.length-2];
		String[] lines = mutatedLineString.split("_");
		int mutatedLine = Integer.valueOf(lines[0]);
		
		CompilationUnit cu = JavaUtil.parseCompilationUnit(mutationFile);
		String mutatedClassName = JavaUtil.getFullNameOfCompilationUnit(cu);
		
		MutateInfo info =
				mutateCode(mutatedClassName, mutatedFile, testcaseConfig, mutatedLine, testcaseName);
		
		if(info.isTooLong){
			System.out.println("mutated trace is over long");
			return;
		}
		
		Trace killingMutatantTrace = info.killingMutateTrace;
		TestCaseRunner checker = new TestCaseRunner();
		
		List<BreakPoint> executingStatements = checker.collectBreakPoints(testcaseConfig);
		if(checker.isOverLong()){
			return;
		}
		
		Trace correctTrace = new TraceModelConstructor().
				constructTraceModel(testcaseConfig, executingStatements);
		
		SimulatedMicroBat microbat = new SimulatedMicroBat();
		ClassLocation mutatedLocation = new ClassLocation(mutatedClassName, null, mutatedLine);
		microbat.prepare(killingMutatantTrace, correctTrace, mutatedLocation, testcaseName, mutationFile);
		Trial trial;
		try {
			trial = microbat.detectMutatedBug(killingMutatantTrace, correctTrace, mutatedLocation, 
					testcaseName, mutatedFile.toString(), unclearRate, enableLoopInference);				
			if(trial != null){
				if(!trial.isBugFound()){
					System.err.println("Cannot find bug in Mutated File: " + mutatedFile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Mutated File: " + mutatedFile);
		}
		
	}

	private void runEvaluation(IPackageFragment pack, ExcelReporter reporter) throws JavaModelException {
		
		for(IJavaElement javaElement: pack.getChildren()){
			if(javaElement instanceof IPackageFragment){
				runEvaluation((IPackageFragment)javaElement, reporter);
			}
			else if(javaElement instanceof ICompilationUnit){
				ICompilationUnit icu = (ICompilationUnit)javaElement;
				CompilationUnit cu = JavaUtil.convertICompilationUnitToASTNode(icu);
				
				List<MethodDeclaration> testingMethods = JTestUtil.findTestingMethod(cu); 
				if(!testingMethods.isEmpty()){
					String className = JavaUtil.getFullNameOfCompilationUnit(cu);
					
					for(MethodDeclaration testingMethod: testingMethods){
						String methodName = testingMethod.getName().getIdentifier();
						try{
							runEvaluationForSingleTestCase(className, methodName, reporter);							
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
					
				}
			}
		}
		
	}
	
	private boolean runEvaluationForSingleTestCase(String className, String methodName, ExcelReporter reporter) 
			throws JavaModelException {
		
		AppJavaClassPath testcaseConfig = createProjectClassPath(className, methodName);
		String testCaseName = className + "#" + methodName;
		
		if(this.ignoredTestCaseFiles.contains(testCaseName)){
			return false;
		}
		
		TestCaseRunner checker = new TestCaseRunner();
		checker.checkValidity(testcaseConfig);
		
		Trace correctTrace = null;
		if(checker.isPassingTest()){
			System.out.println(testCaseName + " is a passed test case");
			List<BreakPoint> executingStatements = checker.collectBreakPoints(testcaseConfig);
			if(checker.isOverLong()){
				return false;
			}
			
			System.out.println("identifying the possible mutated location for " + testCaseName);
			List<ClassLocation> locationList = findMutationLocation(executingStatements);
			
			int thisTrialNum = 0;
			if(!locationList.isEmpty()){
				System.out.println("mutating the tested methods of " + testCaseName);
				Map<String, MutationResult> mutations = generateMutationFiles(locationList);
				System.out.println("mutation done for " + testCaseName);
				
				stop:
				for(String tobeMutatedClass: mutations.keySet()){
					MutationResult result = mutations.get(tobeMutatedClass);
					for(Integer line: result.getMutatedFiles().keySet()){
						List<File> mutatedFileList = result.getMutatedFiles(line);	
						
						for(File mutationFile: mutatedFileList){
							Trial tmpTrial = new Trial();
							tmpTrial.setTestCaseName(testCaseName);
							tmpTrial.setMutatedFile(mutationFile.toString());
							tmpTrial.setMutatedLineNumber(line);
							
							if(parsedTrials.contains(tmpTrial)){
								continue;
							}
							
							EvaluationInfo evalInfo = runEvaluationForSingleTrial(tobeMutatedClass, mutationFile, 
									testcaseConfig, line, testCaseName, correctTrace, executingStatements, 
									reporter, tmpTrial);
							correctTrace = evalInfo.correctTrace;
							if(evalInfo.isSuccess){
								thisTrialNum++;								
								if(thisTrialNum >= trialNumPerTestCase){
									break stop;
								}
							}
						}
					}
				}
			}
			else{
				System.out.println("but " + testCaseName + " cannot be mutated");
				this.ignoredTestCaseFiles.addTestCase(testCaseName);
			}
		}
		else{
			System.out.println(testCaseName + " is a failed test case");
			this.ignoredTestCaseFiles.addTestCase(testCaseName);
			return false;
		}
		
		return false;
	}
	
	class EvaluationInfo{
		boolean isSuccess;
		/**
		 * for performance
		 */
		Trace correctTrace;
		public EvaluationInfo(boolean isSuccess, Trace correctTrace) {
			super();
			this.isSuccess = isSuccess;
			this.correctTrace = correctTrace;
		}
	}
	
	private EvaluationInfo runEvaluationForSingleTrial(String tobeMutatedClass, File mutationFile, AppJavaClassPath testcaseConfig, 
			int line, String testCaseName, Trace correctTrace, List<BreakPoint> executingStatements, 
			ExcelReporter reporter, Trial tmpTrial) throws JavaModelException {
		try {
			MutateInfo mutateInfo = 
					mutateCode(tobeMutatedClass, mutationFile, testcaseConfig, line, testCaseName);
			
			if(mutateInfo == null){
				return new EvaluationInfo(false, correctTrace);
			}
			
			if(mutateInfo.isTimeOut){
				System.out.println("Timeout, mutated file: " + mutationFile);
				System.out.println("skip Time Out test case: " + testCaseName);
				return new EvaluationInfo(false, correctTrace);
			}
			
			Trace killingMutatantTrace = mutateInfo.killingMutateTrace;
			if(killingMutatantTrace != null && killingMutatantTrace.size() > 1){
				if(null == correctTrace){
					System.out.println("Generating correct trace for " + testCaseName);
					correctTrace = new TraceModelConstructor().
							constructTraceModel(testcaseConfig, executingStatements);
				}
				
				ClassLocation mutatedLocation = new ClassLocation(tobeMutatedClass, null, line);
				
				SimulatedMicroBat microbat = new SimulatedMicroBat();
				microbat.prepare(killingMutatantTrace, correctTrace, mutatedLocation, testCaseName, mutationFile.toString());
				
				boolean isValid = true;
				List<Trial> trialList = new ArrayList<>();
				for(int i=0; i<unclearRates.length; i++){
					Trial nonloopTrial = microbat.detectMutatedBug(killingMutatantTrace, correctTrace, 
							mutatedLocation, testCaseName, mutationFile.toString(), unclearRates[i], false);
					Trial loopTrial = microbat.detectMutatedBug(killingMutatantTrace, correctTrace, 
							mutatedLocation, testCaseName, mutationFile.toString(), unclearRates[i], true);
					
					if(loopTrial==null || nonloopTrial==null){
						isValid = false;
						break;
					}
					
					nonloopTrial.setTime(killingMutatantTrace.getConstructTime());
					loopTrial.setTime(killingMutatantTrace.getConstructTime());
					trialList.add(nonloopTrial);
					trialList.add(loopTrial);
				}
				
				if(isValid){
					/**
					 * TODO 
					 * Note that the potential implementation error could be included. The failed
					 * trial with only one step.
					 */
					reporter.export(trialList);
					return new EvaluationInfo(true, correctTrace);
				}
				
			}
			else{
//				System.out.println("No suitable mutants for test case " + testCaseName + "in line " + line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("test case has exception when generating trace:");
			System.err.println(tmpTrial);
		} 
		
		return new EvaluationInfo(false, correctTrace);
	}

	class TraceFilePair{
		Trace mutatedTrace;
		String mutatedFile;
		
		public TraceFilePair(Trace mutatedTrace, String mutatedFile) {
			super();
			this.mutatedTrace = mutatedTrace;
			this.mutatedFile = mutatedFile;
		}
		
		public Trace getMutatedTrace() {
			return mutatedTrace;
		}
		
		public void setMutatedTrace(Trace mutatedTrace) {
			this.mutatedTrace = mutatedTrace;
		}
		
		public String getMutatedFile() {
			return mutatedFile;
		}
		
		public void setMutatedFile(String mutatedFile) {
			this.mutatedFile = mutatedFile;
		}
		
	}

	class MutateInfo{
		Trace killingMutateTrace = null;
		boolean isTooLong = false;
		boolean isKill = false;
		boolean isTimeOut = false;
		
		public MutateInfo(Trace killingMutatnt, boolean isTooLong, boolean isKill, boolean isTimeOut) {
			super();
			this.killingMutateTrace = killingMutatnt;
			this.isTooLong = isTooLong;
			this.isKill = isKill;
			this.isTimeOut = isTimeOut;
		}
		
		
	}
	
	private MutateInfo generateMutateTrace(AppJavaClassPath testcaseConfig, ICompilationUnit iunit, int mutatedLine, 
			String mutatedFile){
		Trace killingMutantTrace = null;
		boolean isTooLong = false;
		boolean isKill = true;
		boolean isTimeOut = false;
		try{
			TestCaseRunner checker = new TestCaseRunner();
			checker.checkValidity(testcaseConfig);
			
			isKill = !checker.isPassingTest() && !checker.hasCompilationError();
			
			if(isKill){
				String testMethod = testcaseConfig.getOptionalTestClass() + "#" + testcaseConfig.getOptionalTestMethod();
				
				System.out.println("generating trace for " + testMethod + " (mutation: " + mutatedFile + ")");
				TraceModelConstructor constructor = new TraceModelConstructor();
				
				List<BreakPoint> executingStatements = checker.collectBreakPoints(testcaseConfig);
				
				if(checker.isOverLong()){
					killingMutantTrace = null;
					isTooLong = true;
				}
				else{
					killingMutantTrace = null;
					long t1 = System.currentTimeMillis();
					killingMutantTrace = constructor.constructTraceModel(testcaseConfig, executingStatements);
					long t2 = System.currentTimeMillis();
					int time = (int) ((t2-t1)/1000);
					killingMutantTrace.setConstructTime(time);
				}
			}
			
		}
		catch(TimeoutException e){
			e.printStackTrace();
			isTimeOut = true;
		}
		
		MutateInfo mutateInfo = new MutateInfo(killingMutantTrace, isTooLong, isKill, isTimeOut);
		return mutateInfo;
	}
	
	private MutateInfo mutateCode(String toBeMutatedClass, File mutationFile, AppJavaClassPath testcaseConfig, 
			int mutatedLine, String testCaseName) 
			throws MalformedURLException, JavaModelException, IOException, NullPointerException {
		
//		Settings.compilationUnitMap.clear();
//		Settings.iCompilationUnitMap.clear();
		
		ICompilationUnit iunit = JavaUtil.findNonCacheICompilationUnitInProject(toBeMutatedClass);
		CompilationUnit unit = JavaUtil.convertICompilationUnitToASTNode(iunit);
		Settings.iCompilationUnitMap.put(toBeMutatedClass, iunit);
		Settings.compilationUnitMap.put(toBeMutatedClass, unit);
		
		String originalCodeText = iunit.getSource();
		
//		System.out.println("checking mutated class " + iunit.getElementName() + " (line: " + mutatedLine + ")");
		String mutatedCodeText = FileUtils.readFileToString(mutationFile);
		
		iunit.getBuffer().setContents(mutatedCodeText);
		iunit.save(new NullProgressMonitor(), true);
		autoCompile();
		
		MutateInfo mutateInfo = null;
		try{
			mutateInfo = generateMutateTrace(testcaseConfig, iunit, mutatedLine, mutationFile.toString());			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		iunit.getBuffer().setContents(originalCodeText);
		iunit.save(new NullProgressMonitor(), true);
		autoCompile();
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		iunit = JavaUtil.findNonCacheICompilationUnitInProject(toBeMutatedClass);
		unit = JavaUtil.convertICompilationUnitToASTNode(iunit);
		Settings.iCompilationUnitMap.put(toBeMutatedClass, iunit);
		Settings.compilationUnitMap.put(toBeMutatedClass, unit);
		
//		Settings.compilationUnitMap.clear();
//		Settings.iCompilationUnitMap.clear();
		
		return mutateInfo;
	}
	
	private void autoCompile() {
		IProject project = JavaUtil.getSpecificJavaProjectInWorkspace();
		try {
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
	}

	private List<ClassLocation> findMutationLocation(List<BreakPoint> executingStatements) {
		List<ClassLocation> locations = new ArrayList<>();
		
		for(BreakPoint point: executingStatements){
			
			String className = point.getDeclaringCompilationUnitName();
			if(!JTestUtil.isInTestCase(className)){
				ClassLocation location = new ClassLocation(className, 
						null, point.getLineNumber());
				locations.add(location);
//				try {
//					if(!JTestUtil.isLocationInTestPackage(location)){
//						locations.add(location);		
//					}
//				} catch (JavaModelException e) {
//					e.printStackTrace();
//				}
			}
			
			
		}
		
		return locations;
	}
	
	private AppJavaClassPath createProjectClassPath(String className, String methodName){
		AppJavaClassPath classPath = MicroBatUtil.constructClassPaths();
		
		String userDir = System.getProperty("user.dir");
		String junitDir = userDir + File.separator + "dropins" + File.separator + "junit_lib";
		
		String junitPath = junitDir + File.separator + "junit.jar";
		String hamcrestCorePath = junitDir + File.separator + "org.hamcrest.core.jar";
		String testRunnerPath = junitDir  + File.separator + "testrunner.jar";
		
		classPath.addClasspath(junitPath);
		classPath.addClasspath(hamcrestCorePath);
		classPath.addClasspath(testRunnerPath);
		
		classPath.addClasspath(junitDir);
		
		classPath.setOptionalTestClass(className);
		classPath.setOptionalTestMethod(methodName);
		
		classPath.setLaunchClass(TEST_RUNNER);
		
		return classPath;
		
		
//		File file = new File(classPath.getClasspathStr());
//		List<URL> cpList = new ArrayList<>();
//		for(String cPath: classPath.getClasspaths()){
//			File file = new File(cPath);
//			URL url;
//			try {
//				url = file.toURI().toURL();
//				cpList.add(url);
//			} catch (MalformedURLException e) {
//				e.printStackTrace();
//			}
//		}
//		URLClassLoader urlcl  = URLClassLoader.newInstance(cpList.toArray(new URL[0]));
//		return urlcl;
	}
}
