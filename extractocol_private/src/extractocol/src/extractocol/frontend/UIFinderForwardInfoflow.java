package extractocol.frontend;

import extractocol.Constants;
import extractocol.backend.request.heapManagement.SourceforTaint;
import extractocol.common.UIfinder.EHlist;
import extractocol.common.apk.UnzipAPK;
import extractocol.common.trackers.tools.HeapToVEL;
import extractocol.frontend.basic.ExtractocolLogger;
import extractocol.frontend.basic.MyCallGraphBuilder;
import extractocol.frontend.helper.StubMethodHandler;
import extractocol.frontend.output.InitialTaintResultContainer;
import extractocol.frontend.output.basic.TaintResultEntry;
import extractocol.frontend.problem.MyBackwardsInfoflowProblem;
import extractocol.frontend.problem.MyInfoflowProblem;
import extractocol.frontend.solver.MyBackwardsInfoflowSolver;
import extractocol.frontend.solver.MyInfoflowSolver;
import heros.solver.CountingThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.FlowSensitiveAliasStrategy;
import soot.jimple.infoflow.aliasing.IAliasingStrategy;
import soot.jimple.infoflow.aliasing.PtsBasedAliasStrategy;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.codeOptimization.DeadCodeEliminator;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.data.*;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.data.pathBuilders.IAbstractionPathBuilder;
import soot.jimple.infoflow.data.pathBuilders.IPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler2;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.problems.BackwardsInfoflowProblem;
import soot.jimple.infoflow.problems.InfoflowProblem;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.IMemoryManager;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.fastSolver.BackwardsInfoflowSolver;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.source.data.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.options.Options;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class UIFinderForwardInfoflow extends AbstractInfoflow {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private InfoflowResults results = null;
    private IInfoflowCFG iCfg;

    private Set<ResultsAvailableHandler> onResultsAvailable = new HashSet<ResultsAvailableHandler>();
    private TaintPropagationHandler taintPropagationHandler = null;
    private TaintPropagationHandler backwardsPropagationHandler = null;

    private long maxMemoryConsumption = -1;

    private Set<Stmt> collectedSources = null;
    private Set<Stmt> collectedSinks = null;

    /** Added by JM*/
    private Set<String> sourceSet = null; // To avoid sub-classes of the class in the source

    /**
     * Creates a new instance of the InfoFlow class for analyzing plain Java code without any references to APKs or the Android SDK.
     */
    public UIFinderForwardInfoflow() {
        super();
    }

    /**
     * Creates a new instance of the Infoflow class for analyzing Android APK files.
     * @param androidPath If forceAndroidJar is false, this is the base directory
     * of the platform files in the Android SDK. If forceAndroidJar is true, this
     * is the full path of a single android.jar file.
     * @param forceAndroidJar True if a single platform JAR file shall be forced,
     * false if Soot shall pick the appropriate platform version
     */
    public UIFinderForwardInfoflow(String androidPath, boolean forceAndroidJar) {
        super(null, androidPath, forceAndroidJar);
        this.pathBuilderFactory = new DefaultPathBuilderFactory();
    }

    /**
     * Creates a new instance of the Infoflow class for analyzing Android APK files.
     * @param androidPath If forceAndroidJar is false, this is the base directory
     * of the platform files in the Android SDK. If forceAndroidJar is true, this
     * is the full path of a single android.jar file.
     * @param forceAndroidJar True if a single platform JAR file shall be forced,
     * false if Soot shall pick the appropriate platform version
     * @param icfgFactory The interprocedural CFG to be used by the InfoFlowProblem
     * @param pathBuilderFactory The factory class for constructing a path builder
     * algorithm
     */
    public UIFinderForwardInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory,
                    IPathBuilderFactory pathBuilderFactory) {
        super(icfgFactory, androidPath, forceAndroidJar);
        this.pathBuilderFactory = pathBuilderFactory == null ? new DefaultPathBuilderFactory()
                : pathBuilderFactory;
    }

    @Override
    public void computeInfoflow(String appPath, String libPath,
                                IEntryPointCreator entryPointCreator,
                                ISourceSinkManager sourcesSinks) {
        if (sourcesSinks == null) {
            logger.error("Sources are empty!");
            return;
        }

        // BK: Need not to rebuild cg again if the cg has been constructed before
        // (cgBuilt is set to false as default.)
        //if(!HeapHandling.hasCGBeenBuilt())
        MySootInitializer(appPath, libPath, entryPointCreator, androidPath, true);

        //Options.v().setPhaseOption("cg.spark", "pre-jimplify:true"); // Added by BK
        //initializeSoot(appPath, libPath, entryPointCreator.getRequiredClasses());

        // entryPoints are the entryPoints required by Soot to calculate Graph - if there is no main method,
        // we have to create a new main method and use it as entryPoint and store our real entryPoints
        //Scene.v().setEntryPoints(Collections.singletonList(entryPointCreator.createDummyMain()));
        // Run the analysis


        // JM
        // We need a additionalSeeds -> seed is a instantiation of Event Handler implementer.
        Set<String> seeds = new HashSet<String> ();

        // Finding a instantiation of Event Handler implementer.
        seeds.add(EHlist.ParentEHs.get(0));

        // Run the analysis
        runAnalysis(sourcesSinks, seeds);
    }

    /** Set multiple dex files for Soot initialization
     *
     * @author Byungkwon Choi
     * @param appPath Path of the app APK file
     * @return True if it loads and sets multiple dex files successfully
     */
    private static boolean LoadMultiDexFiles(String appPath){
        if (appPath == null)
            return false;

        List<String> processDirs = new LinkedList<String>();
        //processDirs.add(appPath);
        String basicPath = appPath.substring(0, appPath.lastIndexOf("/"));

        // Unzip the APK file to extract multiple dex files
        try	{
            UnzipAPK.UnzipAPK(appPath, basicPath);
        } catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }

        // Set the extracted dex files for Soot initialization
        for (int i = 2; i < 50; i++)
        {
            String dexPath = basicPath + Constants.getMultiDexName(basicPath, i);

            if (new File(dexPath).exists()){
                ExtractocolLogger.Info("Read additional dex file: " + dexPath);
                processDirs.add(dexPath);
            }else
                break;
        }

        if(processDirs.size() > 0)
            Options.v().set_process_dir(processDirs);

        return true;
    }

    /** Initialize Soot and get call graph
     *
     * @author Byungkwon Choi
     * @param apkFileLocation Path of the app APK file
     * @param path Path of Android Jar
     * @param entryPointCreator
     * @return True if the call graph is successfully generated
     */
    public static boolean MySootInitializer(String apkFileLocation, String path, IEntryPointCreator entryPointCreator, String AndroidPath, boolean buildingCG) {

        // 1. Load classes & retrieve active bodies (by BK)
        if(MyCallGraphBuilder.needToRetrieveActiveBodies()) {
            // 1-1. Load classes
            soot.G.reset();

            // Initialization
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_soot_classpath(apkFileLocation + File.pathSeparator + path);

            // Load multiple dex files for Soot initialization
            if(!LoadMultiDexFiles(apkFileLocation))
                return false;

            // APK name is dummy -> we also try to load 3rd party libs.
            if (apkFileLocation.endsWith("dummy.apk")) {
                ArrayList<String> processdir = new ArrayList<String>();
                processdir.add("../../SerializationFiles/rxjava-core-0.9.0.jar");
                Options.v().set_process_dir(processdir);
            }

            Options.v().set_android_jars(AndroidPath);

            Options.v().set_whole_program(true);
            Options.v().set_allow_phantom_refs(true);
            Options.v().setPhaseOption("cg.spark", "on"); // Spark
            //Options.v().setPhaseOption("cg.cha", "on"); // CHA
            // RTA
			/*Options.v().setPhaseOption("cg.spark", "on");
			Options.v().setPhaseOption("cg.spark", "rta:true");
			Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
			Options.v().setPhaseOption("cg.spark", "string-constants:true");*/

            // Options.v().setPhaseOption("cg", "verbose:true"); // for debugging
            Options.v().set_output_format(Options.output_format_none);

            for (String className : entryPointCreator.getRequiredClasses())
                Scene.v().addBasicClass(className, SootClass.BODIES);

            // Load necessary calsses
            Scene.v().loadNecessaryClasses();

            // 1-2. Retrieve active bodies
            MyCallGraphBuilder.retrieveActiveBodies();
        }


        // 2. Build call graph
        CallGraph cg;

        if(buildingCG){
            cg = MyCallGraphBuilder.buildAndGet();
        }else{
            cg = MyCallGraphBuilder.getCG();
        }

        Scene.v().setCallGraph(cg);


        return true;
    }

    @Override
    public void computeInfoflow(String appPath, String libPath, String entryPoint,
                                ISourceSinkManager sourcesSinks) {
        if (sourcesSinks == null) {
            logger.error("Sources are empty!");
            return;
        }

        initializeSoot(appPath, libPath,
                SootMethodRepresentationParser.v().parseClassNames
                        (Collections.singletonList(entryPoint), false).keySet(), entryPoint);

        if (!Scene.v().containsMethod(entryPoint)){
            logger.error("Entry point not found: " + entryPoint);
            return;
        }
        SootMethod ep = Scene.v().getMethod(entryPoint);
        if (ep.isConcrete())
            ep.retrieveActiveBody();
        else {
            logger.debug("Skipping non-concrete method " + ep);
            return;
        }
        Scene.v().setEntryPoints(Collections.singletonList(ep));
        Options.v().set_main_class(ep.getDeclaringClass().getName());

        // Compute the additional seeds if they are specified
        Set<String> seeds = Collections.emptySet();
        if (entryPoint != null && !entryPoint.isEmpty())
            seeds = Collections.singleton(entryPoint);
        ipcManager.updateJimpleForICC();

        // Run the analysis
        runAnalysis(sourcesSinks, seeds);
    }

    /**
     * Conducts a taint analysis on an already initialized callgraph
     * @param sourcesSinks The sources and sinks to be used
     */
    protected void runAnalysis(final ISourceSinkManager sourcesSinks) {
        runAnalysis(sourcesSinks, null);
    }

    /**
     * Conducts a taint analysis on an already initialized callgraph
     * @param sourcesSinks The sources and sinks to be used
     * @param additionalSeeds Additional seeds at which to create A ZERO fact
     * even if they are not sources
     */
    private void runAnalysis(final ISourceSinkManager sourcesSinks, final Set<String> additionalSeeds) {
        // Clear the data from previous runs
        maxMemoryConsumption = -1;
        results = null;

        // Some configuration options do not really make sense in combination
        if (config.getEnableStaticFieldTracking()
                && InfoflowConfiguration.getAccessPathLength() == 0)
            throw new RuntimeException("Static field tracking must be disabled "
                    + "if the access path length is zero");
        if (InfoflowConfiguration.getAccessPathLength() < 0)
            throw new RuntimeException("The access path length may not be negative");

        // Clear the base registrations from previous runs
        AccessPathFactory.v().clearBaseRegister();

        // Build the callgraph
        long beforeCallgraph = System.nanoTime();
        constructCallgraph();
        logger.info("Callgraph construction took " + (System.nanoTime() - beforeCallgraph) / 1E9
                + " seconds");

        // Perform constant propagation and remove dead code
        if (config.getCodeEliminationMode() != InfoflowConfiguration.CodeEliminationMode.NoCodeElimination) {
            long currentMillis = System.nanoTime();
            eliminateDeadCode(sourcesSinks);
            logger.info("Dead code elimination took " + (System.nanoTime() - currentMillis) / 1E9
                    + " seconds");
        }

        if (config.getCallgraphAlgorithm() != InfoflowConfiguration.CallgraphAlgorithm.OnDemand)
            logger.info("Callgraph has {} edges", Scene.v().getCallGraph().size());

        if (!config.isTaintAnalysisEnabled()) {
            return;
        }
        logger.info("Starting Taint Analysis");
        iCfg = icfgFactory.buildBiDirICFG(config.getCallgraphAlgorithm(),
                config.getEnableExceptionTracking());

        int numThreads = Runtime.getRuntime().availableProcessors();
        CountingThreadPoolExecutor executor = createExecutor(numThreads);

        // Initialize the memory manager
        FlowDroidMemoryManager.PathDataErasureMode erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseAll;
        if (pathBuilderFactory.isContextSensitive())
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.KeepOnlyContextData;
        if (pathBuilderFactory.supportsPathReconstruction())
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseNothing;
        IMemoryManager<Abstraction> memoryManager = new FlowDroidMemoryManager(false,
                erasureMode);

        // Initialize the data flow manager
        InfoflowManager manager = new InfoflowManager(config, null, iCfg, sourcesSinks,
                taintWrapper, hierarchy);

            MyBackwardsInfoflowProblem backProblem = null;
            InfoflowManager backwardsManager = null;
            MyInfoflowSolver backSolver = null;
            final IAliasingStrategy aliasingStrategy;
            switch (getConfig().getAliasingAlgorithm()) {
                case FlowSensitive:
                    backwardsManager = new InfoflowManager(config, null,
                            new BackwardsInfoflowCFG(iCfg), sourcesSinks, taintWrapper, hierarchy);
                    backProblem = new MyBackwardsInfoflowProblem(backwardsManager);
                    backSolver = new MyBackwardsInfoflowSolver(backProblem, executor);
                    backSolver.setMemoryManager(memoryManager);
                    backSolver.setJumpPredecessors(!pathBuilderFactory.supportsPathReconstruction());
//				backSolver.setEnableMergePointChecking(true);

                    aliasingStrategy = new FlowSensitiveAliasStrategy(iCfg, backSolver);
                    break;
            case PtsBased:
                backProblem = null;
                backSolver = null;
                aliasingStrategy = new PtsBasedAliasStrategy(iCfg);
                break;
            default:
                throw new RuntimeException("Unsupported aliasing algorithm");
        }

        // Get the zero fact
        Abstraction zeroValue = backProblem != null
                ? backProblem.createZeroValue() : null;
        MyInfoflowProblem forwardProblem  = new MyInfoflowProblem(manager,
                aliasingStrategy, zeroValue);

        // Set the options
        MyInfoflowSolver forwardSolver = new MyInfoflowSolver(forwardProblem, executor);
        aliasingStrategy.setForwardSolver(forwardSolver);
        manager.setForwardSolver(forwardSolver);
        if (backwardsManager != null)
            backwardsManager.setForwardSolver(forwardSolver);

        forwardSolver.setMemoryManager(memoryManager);
        forwardSolver.setJumpPredecessors(!pathBuilderFactory.supportsPathReconstruction());
//		forwardSolver.setEnableMergePointChecking(true);

        forwardProblem.setTaintPropagationHandler(taintPropagationHandler);
        forwardProblem.setTaintWrapper(taintWrapper);
        if (nativeCallHandler != null)
            forwardProblem.setNativeCallHandler(nativeCallHandler);

        if (backProblem != null) {
            backProblem.setForwardSolver(forwardSolver);
            backProblem.setTaintPropagationHandler(backwardsPropagationHandler);
            backProblem.setTaintWrapper(taintWrapper);
            if (nativeCallHandler != null)
                backProblem.setNativeCallHandler(nativeCallHandler);
            backProblem.setActivationUnitsToCallSites(forwardProblem);
        }

        // Print our configuration
        config.printSummary();
        if (config.getFlowSensitiveAliasing() && !aliasingStrategy.isFlowSensitive())
            logger.warn("Trying to use a flow-sensitive aliasing with an "
                    + "aliasing strategy that does not support this feature");

        // We have to look through the complete program to find sources
        // which are then taken as seeds.
        int sinkCount = 0;
        logger.info("Looking for sources and sinks...");

        /** Added by JM */
        Collection<SootMethod> smSet = getMethodsForSeeds(iCfg);
        for(SootMethod sm: smSet)
            iCfg.notifyMethodChanged(sm);

        /** Added by JM */
        StubMethodHandler.EvaluateStubMethodMap(smSet);

        scanMethodForSourcesSinksForHeap(forwardProblem);

//         We optionally also allow additional seeds to be specified
        if (additionalSeeds != null)
            for (String meth : additionalSeeds) {
                SootMethod m = Scene.v().getMethod(meth);
                if (!m.hasActiveBody()) {
                    logger.warn("Seed method {} has no active body", m);
                    continue;
                }
				/*forwardProblem.addInitialSeeds(m.getActiveBody().getUnits().getFirst(),
						Collections.singleton(forwardProblem.zeroValue()));*/ /** Commented out by BK */
                forwardProblem.addInitialSeeds(m.getActiveBody().getUnits().getFirst(),
                        Collections.singleton(forwardProblem.zeroValue()));
            }

        // Report on the sources and sinks we have found
        if (!forwardProblem.hasInitialSeeds()) {
            logger.error("No sources found, aborting analysis");
            return;
        }

        logger.info("Source lookup done, found {} sources and {} sinks.", forwardProblem.getInitialSeeds().size(),
                sinkCount);

        // Initialize the taint wrapper if we have one
        if (taintWrapper != null)
            taintWrapper.initialize(manager);
        if (nativeCallHandler != null)
            nativeCallHandler.initialize(manager);

        // Register the handler for interim results
        TaintPropagationResults propagationResults = forwardProblem.getResults();
        final CountingThreadPoolExecutor resultExecutor = createExecutor(numThreads);
        final IAbstractionPathBuilder builder = pathBuilderFactory.createPathBuilder(
                resultExecutor, iCfg);

        if (config.getIncrementalResultReporting()) {
            // Create the path builder
            this.results = new InfoflowResults();
            propagationResults.addResultAvailableHandler(new TaintPropagationResults.OnTaintPropagationResultAdded() {

                @Override
                public boolean onResultAvailable(AbstractionAtSink abs) {
                    builder.addResultAvailableHandler(new IAbstractionPathBuilder.OnPathBuilderResultAvailable() {

                        @Override
                        public void onResultAvailable(ResultSourceInfo source, ResultSinkInfo sink) {
                            // Notify our external handlers
                            for (ResultsAvailableHandler handler : onResultsAvailable) {
                                if (handler instanceof ResultsAvailableHandler2) {
                                    ResultsAvailableHandler2 handler2 = (ResultsAvailableHandler2) handler;
                                    handler2.onSingleResultAvailable(source, sink);
                                }
                            }
                            results.addResult(sink, source);
                        }

                    });

                    // Compute the result paths
                    builder.computeTaintPaths(Collections.singleton(abs));
                    return true;
                }

            });
        }

        forwardSolver.solve();
        maxMemoryConsumption = Math.max(maxMemoryConsumption, getUsedMemory());

        // Not really nice, but sometimes Heros returns before all
        // executor tasks are actually done. This way, we give it a
        // chance to terminate gracefully before moving on.
        int terminateTries = 0;
        while (terminateTries < 10) {
            if (executor.getActiveCount() != 0 || !executor.isTerminated()) {
                terminateTries++;
                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                    logger.error("Could not wait for executor termination", e);
                }
            }
            else
                break;
        }
        if (executor.getActiveCount() != 0 || !executor.isTerminated())
            logger.error("Executor did not terminate gracefully");

        // Print taint wrapper statistics
        if (taintWrapper != null) {
            logger.info("Taint wrapper hits: " + taintWrapper.getWrapperHits());
            logger.info("Taint wrapper misses: " + taintWrapper.getWrapperMisses());
        }

        Set<AbstractionAtSink> res = propagationResults.getResults();

        // We need to prune access paths that are entailed by another one
        for (Iterator<AbstractionAtSink> absAtSinkIt = res.iterator(); absAtSinkIt.hasNext(); ) {
            AbstractionAtSink curAbs = absAtSinkIt.next();
            for (AbstractionAtSink checkAbs : res)
                if (checkAbs != curAbs
                        && checkAbs.getSinkStmt() == curAbs.getSinkStmt()
                        && checkAbs.getAbstraction().isImplicit() == curAbs.getAbstraction().isImplicit()
                        && checkAbs.getAbstraction().getSourceContext() == curAbs.getAbstraction().getSourceContext())
                    if (checkAbs.getAbstraction().getAccessPath().entails(
                            curAbs.getAbstraction().getAccessPath())) {
                        absAtSinkIt.remove();
                        break;
                    }
        }

        logger.info("IFDS problem with {} forward and {} backward edges solved, "
                        + "processing {} results...", forwardSolver.propagationCount,
                backSolver == null ? 0 : backSolver.propagationCount,
                res == null ? 0 : res.size());

        // Force a cleanup. Everything we need is reachable through the
        // results set, the other abstractions can be killed now.
        maxMemoryConsumption = Math.max(maxMemoryConsumption, getUsedMemory());
        forwardSolver.cleanup();
        if (backSolver != null) {
            backSolver.cleanup();
            backSolver = null;
            backProblem = null;
        }
        forwardSolver = null;
        forwardProblem = null;
        Runtime.getRuntime().gc();

        if (!config.getIncrementalResultReporting()) {
            builder.computeTaintPaths(res);
            if (this.results == null)
                this.results = builder.getResults();
            else
                this.results.addAll(builder.getResults());
        }

        // Wait for the path builders to terminate
        try {
            resultExecutor.awaitCompletion();
        } catch (InterruptedException e) {
            logger.error("Could not wait for executor termination", e);
        }

        if (config.getIncrementalResultReporting()) {
            // After the last intermediate result has been computed, we need to
            // re-process those abstractions that received new neighbors in the
            // meantime
            builder.runIncrementalPathCompuation();

            try {
                resultExecutor.awaitCompletion();
            } catch (InterruptedException e) {
                logger.error("Could not wait for executor termination", e);
            }
        }
        resultExecutor.shutdown();

        if (results == null || results.getResults().isEmpty())
            logger.warn("No results found.");
        else for (ResultSinkInfo sink : results.getResults().keySet()) {
            logger.info("The sink {} in method {} was called with values from the following sources:",
                    sink, iCfg.getMethodOf(sink.getSink()).getSignature() );
            for (ResultSourceInfo source : results.getResults().get(sink)) {
                logger.info("- {} in method {}",source, iCfg.getMethodOf(source.getSource()).getSignature());
                if (source.getPath() != null) {
                    logger.info("\ton Path: ");
                    for (Unit p : source.getPath()) {
                        logger.info("\t -> " + iCfg.getMethodOf(p));
                        logger.info("\t\t -> " + p);
                    }
                }
            }
        }

        for (ResultsAvailableHandler handler : onResultsAvailable)
            handler.onResultsAvailable(iCfg, results);

        if (config.getWriteOutputFiles())
            PackManager.v().writeOutput();

        maxMemoryConsumption = Math.max(maxMemoryConsumption, getUsedMemory());
        System.out.println("Maximum memory consumption: " + maxMemoryConsumption / 1E6 + " MB");
    }

    /**
     * Gets the memory used by FlowDroid at the moment
     * @return FlowDroid's current memory consumption in bytes
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Runs all code optimizers
     * @param sourcesSinks The SourceSinkManager
     */
    private void eliminateDeadCode(ISourceSinkManager sourcesSinks) {
        ICodeOptimizer dce = new DeadCodeEliminator();
        dce.initialize(config);
        dce.run(iCfg, Scene.v().getEntryPoints(), sourcesSinks, taintWrapper);
    }

    /**
     * Creates a new executor object for spawning worker threads
     * @param numThreads The number of threads to use
     * @return The generated executor
     */
    private CountingThreadPoolExecutor createExecutor(int numThreads) {
        return new CountingThreadPoolExecutor
                (config.getMaxThreadNum() == -1 ? numThreads
                        : Math.min(config.getMaxThreadNum(), numThreads),
                        Integer.MAX_VALUE, 30, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>());
    }

    /** Added by JM*/
    public void setSourceSinkProvider(ISourceSinkDefinitionProvider _sourceSinkProvider){
        this.sourceSet = new HashSet<String>();

        Set<SourceSinkDefinition> defSet = _sourceSinkProvider.getSources();
        for(SourceSinkDefinition ssd: defSet)
            this.sourceSet.add(ssd.toString());
    }

    private Collection<SootMethod> getMethodsForSeeds(IInfoflowCFG icfg) {
        List<SootMethod> seeds = new LinkedList<SootMethod>();
        // If we have a callgraph, we retrieve the reachable methods. Otherwise,
        // we have no choice but take all application methods as an approximation
        if (Scene.v().hasCallGraph()) {
            List<MethodOrMethodContext> eps = new ArrayList<MethodOrMethodContext>(Scene.v().getEntryPoints());
            ReachableMethods reachableMethods = new ReachableMethods(Scene.v().getCallGraph(), eps.iterator(), null);
            reachableMethods.update();

			/*for (Iterator<MethodOrMethodContext> iter = reachableMethods.listener(); iter.hasNext();)
				seeds.add(iter.next().method());*/
            /** Added by BK */
            for (SootClass sc : Scene.v().getClasses())
                for (SootMethod sm : sc.getMethods())
                    if (sm.isConcrete()){
                        if(!sm.hasActiveBody()) {
                            try {
                                sm.retrieveActiveBody();
                            } catch (Exception e) {
                                System.err.println("Err_Method: " + sm.toString());
                                e.printStackTrace();
                                continue;
                            }
                        }

                        if(!(sm.getActiveBody() instanceof JimpleBody))
                            System.err.println("Not Jimple: " + sm.toString());
                        seeds.add(sm);
                    }
        }
        else {
            long beforeSeedMethods = System.nanoTime();
            Set<SootMethod> doneSet = new HashSet<SootMethod>();
            for (SootMethod sm : Scene.v().getEntryPoints())
                getMethodsForSeedsIncremental(sm, doneSet, seeds, icfg);
            logger.info("Collecting seed methods took {} seconds", (System.nanoTime() - beforeSeedMethods) / 1E9);
        }
        return seeds;
    }

    /** Scans the given method to find sources for tracking heap objects
     *
     * @author Jeongmin Kim
     * @param forwardProblem
     * @return Number of sources;
     */
    private void scanMethodForSourcesSinksForHeap(MyInfoflowProblem forwardProblem){
        // Find the target method
        for(Unit ut : Scene.v().getMethod(EHlist.ParentEHs.get(0)).getActiveBody().getUnits()){
            // Find the target unit (assignment unit)
            if (ut.toString().equals(EHlist.EHObjs.get(0)))
            {
                Abstraction zero = forwardProblem.zeroValue();
                Set<Abstraction> seedSet = new HashSet<Abstraction>();

                // The unit must be an assignment unit.
                if(ut instanceof InvokeStmt){
                    //Stmt s = (Stmt) ut;
                    InvokeStmt is = (InvokeStmt) ut;

                    // Generate the seed abstraction for the right Op of the unit
                    //Abstraction baseAbs = zero.deriveNewAbstraction(zero.getAccessPath().copyWithNewValue(((AssignStmt) ut).getRightOp()), s);
                    AccessPath baseAP = AccessPathFactory.v().createAccessPath(((SpecialInvokeExpr)is.getInvokeExpr()).getBase(), false);
                    Abstraction baseAbs = zero.deriveNewAbstraction(baseAP, is);


                    TaintResultEntry tre = new TaintResultEntry(EHlist.ParentEHs.get(0), ut.toString(),
                            MyCallGraphBuilder.getUnitHash(ut, Scene.v().getMethod(EHlist.ParentEHs.get(0))));

                    tre.addTaintMethod(iCfg.getMethodOf(ut).getSignature(), ((SpecialInvokeExpr)is.getInvokeExpr()).getBase().toString());
                    baseAbs.setID(tre.getID());
                    baseAbs.setIsInitialSeed(true);

                    seedSet.add(baseAbs);
                    // Register the corresponding TaintResultEntry as a seed
                    InitialTaintResultContainer.addSeed(tre);

                    // Add it as seed of InfoflowProblem
                    forwardProblem.addInitialSeeds(ut, seedSet);
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSources.add(is);
                    ExtractocolLogger.Log("DP found (for heap): " + ut + " in " + EHlist.ParentEHs.get(0));
                }
            }
        }
    }

    private void getMethodsForSeedsIncremental(SootMethod sm,
                                               Set<SootMethod> doneSet, List<SootMethod> seeds, IInfoflowCFG icfg) {
        assert Scene.v().hasFastHierarchy();
        if (!sm.isConcrete() || !sm.getDeclaringClass().isApplicationClass() || !doneSet.add(sm))
            return;
        seeds.add(sm);
        for (Unit u : sm.retrieveActiveBody().getUnits()) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr())
                for (SootMethod callee : icfg.getCalleesOfCallAt(stmt))
                    getMethodsForSeedsIncremental(callee, doneSet, seeds, icfg);
        }
    }

    /**
     * Scans the given method for sources and sinks contained in it. Sinks are
     * just counted, sources are added to the InfoflowProblem as seeds.
     * @param sourcesSinks The SourceSinkManager to be used for identifying
     * sources and sinks
     * @param forwardProblem The InfoflowProblem in which to register the
     * sources as seeds
     * @param m The method to scan for sources and sinks
     * @return The number of sinks found in this method
     */
    private int scanMethodForSourcesSinks(
            final ISourceSinkManager sourcesSinks,
            MyInfoflowProblem forwardProblem,
            SootMethod m) {
        if (getConfig().getLogSourcesAndSinks() && collectedSources == null) {
            collectedSources = new HashSet<>();
            collectedSinks = new HashSet<>();
        }

        int sinkCount = 0;
        if (m.hasActiveBody()) {
            // Check whether this is a system class we need to ignore
            final String className = m.getDeclaringClass().getName();
            if (config.getIgnoreFlowsInSystemPackages()
                    && SystemClassHandler.isClassInSystemPackage(className))
                return sinkCount;

            // Look for a source in the method. Also look for sinks. If we
            // have no sink in the program, we don't need to perform any
            // analysis
            PatchingChain<Unit> units = m.getActiveBody().getUnits();
            for (Unit u : units) {
                Stmt s = (Stmt) u;
                if (sourcesSinks.getSourceInfo(s, iCfg) != null) {
                    forwardProblem.addInitialSeeds(u, Collections.singleton(forwardProblem.zeroValue()));
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSources.add(s);
                    logger.debug("Source found: {}", u);
                }
                if (sourcesSinks.isSink(s, iCfg, null)) {
                    sinkCount++;
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSinks.add(s);
                    logger.debug("Sink found: {}", u);
                }
            }

        }
        return sinkCount;
    }

    @Override
    public InfoflowResults getResults() {
        return results;
    }

    @Override
    public boolean isResultAvailable() {
        if (results == null) {
            return false;
        }
        return true;
    }

    /**
     * Adds a handler that is called when information flow results are available
     * @param handler The handler to add
     */
    public void addResultsAvailableHandler(ResultsAvailableHandler handler) {
        this.onResultsAvailable.add(handler);
    }

    /**
     * Sets a handler which is invoked whenever a taint is propagated
     * @param handler The handler to be invoked when propagating taints
     */
    public void setTaintPropagationHandler(TaintPropagationHandler handler) {
        this.taintPropagationHandler = handler;
    }

    /**
     * Sets a handler which is invoked whenever an alias is propagated backwards
     * @param handler The handler to be invoked when propagating aliases
     */
    public void setBackwardsPropagationHandler(TaintPropagationHandler handler) {
        this.backwardsPropagationHandler = handler;
    }

    /**
     * Removes a handler that is called when information flow results are available
     * @param handler The handler to remove
     */
    public void removeResultsAvailableHandler(ResultsAvailableHandler handler) {
        onResultsAvailable.remove(handler);
    }

    /**
     * Gets the maximum memory consumption during the last analysis run
     * @return The maximum memory consumption during the last analysis run if
     * available, otherwise -1
     */
    public long getMaxMemoryConsumption() {
        return this.maxMemoryConsumption;
    }

    /**
     * Gets the concrete set of sources that have been collected in preparation
     * for the taint analysis. This method will return null if source and sink
     * logging has not been enabled (see InfoflowConfiguration.
     * setLogSourcesAndSinks()),
     * @return The set of sources collected for taint analysis
     */
    public Set<Stmt> getCollectedSources() {
        return this.collectedSources;
    }

    /**
     * Gets the concrete set of sinks that have been collected in preparation
     * for the taint analysis. This method will return null if source and sink
     * logging has not been enabled (see InfoflowConfiguration.
     * setLogSourcesAndSinks()),
     * @return The set of sinks collected for taint analysis
     */
    public Set<Stmt> getCollectedSinks() {
        return this.collectedSinks;
    }
}
