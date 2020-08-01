
package extractocol.tester;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import extractocol.backend.request.helper.*;
import extractocol.common.UIfinder.EHSigPair;
import extractocol.common.UIfinder.EHSigManager;
import extractocol.common.outputs.helper.HtmlGraphDrawing;
import extractocol.common.valueEntry.PartofUrlStringTable;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import extractocol.Constants;
import extractocol.backend.common.BackendThread;
import extractocol.backend.request.helper.CFGSerializer.ICFG_CASE;
import extractocol.backend.request.semantic.url.UrlBuilder;
import extractocol.common.debugger.DebugInfo;
import extractocol.common.helper.MultiThreadHelper;
import extractocol.common.outputs.BackendOutput;
import extractocol.common.outputs.backendOutputHelper.ReqRespInfo;
import extractocol.common.outputs.helper.BackendOutputHelper;
import extractocol.common.retrofit.RetrofitFinalize;
import extractocol.common.retrofit.RetrofitHandle;
import extractocol.common.retrofit.utils.FileAnalyzer;
import extractocol.common.trackers.tools.ArgToVEL;
import extractocol.common.trackers.tools.HeapToVEL;
import extractocol.common.valueEntry.ValueEntryList;
import extractocol.frontend.basic.ExtractocolLogger;
import extractocol.frontend.helper.PropagateHelper;
import extractocol.frontend.helper.StubMethodHandler;
import extractocol.frontend.output.TaintResultContainer;
import extractocol.frontend.output.basic.DPContainer;
import extractocol.frontend.output.basic.EPContainer;
import extractocol.frontend.basic.MyCallGraphBuilder;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.Edge;

public class Backend
{
	public static 	String Forward_EPMethod;
	public static String Forward_DPStmt;
	public static String CurrentParentMethod;
	public static String CurrentEntryPoint;
	public static HashSet<String> AnalyzedMethods = new HashSet<String>(); // list of methods that have been already analyzed.
	
	private static DPContainer DPC;
	private static EPContainer EPC;
	private static DebugInfo DebugInfo;
	private static DecimalFormat runtimeFormat = new DecimalFormat("##.#");
	
	private static int maxEP; // 
	private static int currentEPCnt;
	
	static ThreadPoolExecutor executor;
	
	public static void main(String[] args)
	{
		try
		{
			long analysis_start, analysis_end;
			analysis_start = System.currentTimeMillis();
			
			// Process argument
			ArgProcess(args);
			
			// Check whether the Backend is required to be running
			if(!BackendOutputHelper.needToRunBackend() && !Constants.heapobject && !ArgToVEL.isArgTracking() && !Constants.FindingEventHandler) {
				ExtractocolLogger.Log("Not performing Backend. ReqRespInfoList.ser already exists.");
				return;
			}

			// JimpleLoader
			// BK: Need not to retrieve active bodies again if it has been done before 
			// cgBuilt will be true when the heap handler is being performed and the frontend has been called at least once. 
			// cgBuilt is set to false as default. You don't need to care about this when running backend independently not through the heap handler.
			if(MyCallGraphBuilder.needToRetrieveActiveBodies()){
				JimpleLoader jl = new JimpleLoader(Constants.getAndroidSDKPath(), Constants.getAPKpath(args[1] + ".apk"),Constants.getSourcesAndSinksPath());
			}
			
			// Initialize
			BackendOutput.clear();
			executor = MultiThreadHelper.createExecutor(15, TimeUnit.MINUTES);
			
			long start = System.currentTimeMillis();


			//added by JM
            if (Constants.FindingEventHandler)
            {
                SootMethod target = null;
				System.out.println("Finding UI handler.");

				//for Netflix
//                target = Scene.v().getMethod("<com.netflix.mediaclient.util.MdxUtils$1: void onItemClick(android.widget.AdapterView,android.view.View,int,long)>");

				//for insteon
//                target = Scene.v().getMethod("<com.insteon.SmartLincManager: void sendCommand(com.insteon.InsteonService.House,com.insteon.CommandInfo,boolean,boolean)>");

				//for Hue
//				target = Scene.v().getMethod("<com.philips.lighting.hue.sdk.b.a.e: void a(java.lang.String,com.philips.lighting.a.p,com.philips.lighting.hue.f.d,com.philips.lighting.hue.sdk.a.a.b)>");

//				for Nest
//				target = Scene.v().getMethod("<com.obsidian.v4.data.cz.a.i: java.lang.Object doInBackground(java.lang.Object[])>");

                //for Harmony
//                target = Scene.v().getMethod("<com.logitech.harmonyhub.sdk.core.hub.BaseHub$16: void run()>");

                //for August
//                target = Scene.v().getMethod("<com.august.luna.network.http.AugustAPIClient: io.reactivex.Single a(com.august.luna.model.Lock,com.august.luna.model.Bridge$BridgeOperation,boolean,boolean,boolean)>");

                //for Winix
//                target = Scene.v().getMethod("<com.winix.android.smartair.nike.control.ControlMainActivity$6: void onClick(android.view.View)>");

                //for SmartThings
//                target = Scene.v().getMethod("<com.smartthings.android.common.ui.tiles.data_binders.DeviceTile6x4DataBinder: void g()>");

                //for wink - Set EP
                target = Scene.v().getMethod("<com.quirky.android.wink.core.devices.siren.a.a.e: void q()>");

				/**
				 *
                 * Complete
				 * Before UI Handler Finding, we need to analyze ButterKnife Classes in APK.
				 * Author: JM
				 * Purpose: Parsing ButterKnife Classes.
				 */
				ArrayList<EHSigPair> EH_SigPairs = new ArrayList<EHSigPair>();
                EHSigManager.setRetultArray(EH_SigPairs);
                EHSigManager.ButterKnifeParsing();

				/**
				 * In progress
				 * UI Handler Finding
				 * Author: JM
				 * */
                Set<String> reachedlist = new HashSet<>();
                ArrayList<String> EHlist = new ArrayList<>();
                FindingEH(target, reachedlist, EHlist);

                //The app doesn't use BF lib, we need to add BKE entries for each EP.
                EHSigPair espair = new EHSigPair();
                espair.setEP(target.getSignature());
                espair.setEH(EHlist);
                EHSigManager.addPair(espair);

                //For debugging
                EHSigManager.PrintAll();

                /**
                 * In progress - we haven't handle Nest yet.
                 * Purpose: finding EH to hexid
                 * Process
                 *  1. Finding setOnXX method in a specific condition that are 1) method name starts with setOn, 2) the first param is xxxListener and 3) the type of the param is same to our EH class.
                 *  2. Serializing the results to be used in Backward taint analysis module.
                 * Author: JM
                 * Prerequisites
                 * Before this point, EH - Sig pairs should be obtained.
                 */
                EHSigManager.FindSetListners();


                /**
                 * Calling the BackwardTaint module
                 */
                Extractocol.Extractocol_Frontend_forUI();


                /**
                 * Calling the Backend of Extractocol module
                 */
                Backend_ForUI(start, analysis_start);


                //Print results
                for ( EHSigPair ehsigpair : EHSigManager.results)
                {
                    System.out.println("EP : " + ehsigpair.getEP());
                    int i = 0;
                    for (String res : ehsigpair.getHexid())
                    {
                        System.out.println("[" + i++ +"]Res-id: " + res);
                    }
                }

                if (Constants.isNotFullStack())
                    System.exit(0);
            }

            // This block is temporary.
            else {
                List<DPContainer> dplist = InitBackward();

                if(dplist == null) {
                    ExtractocolLogger.Log("Nothing to do in Backend.");
                    return;
                }

                StubMethodHandler.readStubMapFromFile();
                long end = System.currentTimeMillis();

                System.out.println("Loading time: " + (end - start) / 1000 + " second");
                System.out.println("\n\nBackend Start! ( Backward & Forward )");
                System.out.println();

                countTotEP(dplist);

                //Added by jeongmin for drawing block graph.
                if (Constants.DrawGraph != null) {
                    HtmlGraphDrawing.DrawingBlockGraph(Scene.v().getMethod(Constants.DrawGraph));
                    System.exit(1);
                }


				// Main function
				if (Constants.backendMultiThread)
					multiThread(dplist);
				else
					singleThread(dplist);

				analysis_end = System.currentTimeMillis();
				System.out.println("\n** Total analysis time: " + getTimeString((analysis_end - analysis_start) / 1000.0) + "\n");

				if (Constants.heapobject) {
					getHeapValue();
					BackendOutput.clear();
					return;
				}

				// not need to save the other result when tracking argument
				if (ArgToVEL.isArgTracking()) {
					BackendOutput.clear();
					return;
				}

				// Finalize retrofit entries (for request)
				if (Constants.getIsRetrofit())
					RetrofitFinalize.Request();

				// De-duplicate the reqRespInfo entries
				//BackendOutput.deduplication();
				//BackendOutput.deduplication_multiThreading();
				/*
				By JM - disabled
				 */
                BackendOutput.deduplication_new();


                //BackendOutput.Finalize();

				// Print url signatures
				ResultPrintHandler.urlResultPrint();
				ResultPrintHandler.requestResultPrint();

				// Save the result into file
				BackendOutputHelper.SerializeBackendOutputs(BackendOutput.getFinalReqRespInfoList());
				BackendOutput.clear();

				PartofUrlStringTable.serialize();

				if (Constants.isNotFullStack())
					System.exit(0);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void Backend_ForUI(long start, long analysis_start)
    {
        List<DPContainer> dplist = null;
        try {
            dplist = InitBackward();
        } catch (Exception e) {
            e.printStackTrace();
        }

        StubMethodHandler.readStubMapFromFile();
        long end = System.currentTimeMillis();

        System.out.println("Loading time: " + (end - start) / 1000 + " second");
        System.out.println("\n\nBackend Start! ( Backward & Forward )");
        System.out.println();

        // Main function
        multiThread(dplist);

        //in this point, we don't use global analysis_end. global analysis_end var is used in normal Backend mode.
        long analysis_end = System.currentTimeMillis();
        System.out.println("\n** Total analysis time: " + getTimeString((analysis_end - analysis_start) / 1000.0) + "\n");
    }

	private static String extractClassnameofMethod(String methodSig){
		return methodSig.substring(1, methodSig.indexOf(":"));
	}

	private static boolean isAndroidListener(String className)
	{
	    if (EHSigManager.results.size() > 0)
	        return false;

		SootClass sc = Scene.v().getSootClass(className);

		// sc may has superclasses
        for (SootClass supsc : Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(sc))
    		for (SootClass ifs : supsc.getInterfaces())
	    	{
		    	if (ifs.toString().endsWith("Listener"))
			    	return true;
		    }
		return false;
	}

	private static void FindingEH(SootMethod target, Set<String> reachedlist, ArrayList<String> EHlist) {
        Iterator<Edge> iter = null;

        if (EHSigManager.containsEH(target))
        {
            EHlist.add(target.getSignature());
//            System.out.println("Candidate EventHandlers: " + target.getSignature());
            return;
        }
        // GestureListener trigger
        else if (target.getDeclaringClass().getName().endsWith("$GestureListener"))
        {
            // we need to find the init point of this GestureListner
//            SootMethod init = target.getDeclaringClass().getMethod("<" + target.getDeclaringClass().getName()
//                    + ": void <init>(" + target.getDeclaringClass().getName().substring(0, target.getDeclaringClass().getName().indexOf("$"))
//            + ")>");
                    //<com.insteon.ui.ControllableDevices$GestureListener: void <init>(com.insteon.ui.ControllableDevices)>

            SootMethod init = null;

            for (SootMethod sm: target.getDeclaringClass().getMethods())
            {
                if (sm.getSignature().equals("<" + target.getDeclaringClass().getName()
                    + ": void <init>(" + target.getDeclaringClass().getName().substring(0, target.getDeclaringClass().getName().indexOf("$")) + ")>"))
                {
                    init = sm;
                    break;
                }
            }

            iter = Scene.v().getCallGraph().edgesInto(init);
        }
		// Rx trigger
        else if (target.getName().equals("subscribe"))
        {
            //In this point we try to find the initiali point of the class of this subscribe().
            SootMethod init = target.getDeclaringClass().getMethodByName("<init>");
            iter = Scene.v().getCallGraph().edgesInto(init);
        }
        else if (target.getName().contains("on"))
        {
            // Class postfix - Listener
            if (isAndroidListener(extractClassnameofMethod(target.getSignature()))) {
                EHlist.add(target.getSignature());
//				System.out.println("Candidate EventHandlers: " + target.getSignature());
            }
            return;
        }

        if (iter == null)
    		iter = Scene.v().getCallGraph().edgesInto(target);

		for(; iter.hasNext(); ) {
			Edge ed = iter.next();
			String methodsig= ed.getSrc().method().getSignature();
			if (!reachedlist.contains(methodsig)) {
				reachedlist.add(methodsig);
//				System.out.println("Caller : " + methodsig);
                FindingEH(ed.getSrc().method(), reachedlist, EHlist);
			}
		}
	}

	private static void singleThread(List<DPContainer> dplist) {
		for (int i = 0; i < dplist.size(); i++)
		{
			DPC = dplist.get(i);
			CurrentParentMethod = DPC.getDPMethod();
			System.out.println("\n" + (i + 1) + "th DP : " + CurrentParentMethod);
			System.out.println("\tDP Stmt: " + DPC.getDPStmt() + "\n");
			
			if (SkipORSpecificDP(DPC))
				continue;
			
			for (int j = 0; j < DPC.getEPList().size(); j++)
			{
				/*******************************************************/
				/** 1. Backward analysis (Request signature building) **/
				/*******************************************************/
				EPC = DPC.getEPList().get(j);
				CurrentEntryPoint = EPC.getEP();
				System.out.println("\t" + (i + 1) + "/" + dplist.size() + " DP - " + (j + 1) + "/" + DPC.getEPList().size() + " EP : " + CurrentEntryPoint);
				
				if (SkipORSpecificEP(EPC))
					continue;
				
				if(!doesContainSpecificTaintMethod(EPC))
					continue;
				
				increaseEPCnt();
				if(Constants.heapobject)
					if(doesReachMaxEP())
						break;
				
				BackendThread bt = new BackendThread(currentEPCnt, DPC, EPC, false);
				bt.run();
			}
		}
	}
	
	private static void multiThread(List<DPContainer> dplist) {
		int cnt = 0;
		for (int i = 0; i < dplist.size(); i++)
		{
			DPC = dplist.get(i);
			
			if (SkipORSpecificDP(DPC))
				continue;
			
			for (int j = 0; j < DPC.getEPList().size(); j++)
			{
				/*******************************************************/
				/** 1. Backward analysis (Request signature building) **/
				/*******************************************************/
				EPC = DPC.getEPList().get(j);
				
				if (SkipORSpecificEP(EPC))
					continue;
				
				if(!doesContainSpecificTaintMethod(EPC))
					continue;
				
				increaseEPCnt();
				if(Constants.heapobject)
					if(doesReachMaxEP())
						break;

				System.out.println("this EP: " + EPC.getEP());
				
				executor.execute(new BackendThread(currentEPCnt, DPC, EPC, true));
				cnt++;
			}
		}
		
		ExtractocolLogger.Log("Thread #: " + cnt);
		MultiThreadHelper.awaitCompletion(executor, Constants.getMaxBackendRunningTime(), Constants.getBackendTimeUnit(), 
				"The Backend will be finished within " + Constants.getMaxBackendRunningTime() +	" " + Constants.getBackendTimeUnit() + ".", 
				"Backend finished!");
	}
	
	
	/****************************************************************************/
	/***                    APIs for heap value tracking                      ***/
	/****************************************************************************/
	private static void getHeapValue()
	{
		HeapToVEL.HeapValue = new ValueEntryList(null);
		for (ReqRespInfo rri : BackendOutput.ReqRespInfoList)
		{
			ValueEntryList vel = rri.heapTable.getValueEntryListDeep(HeapToVEL.targetHeap);
			if (vel != null)
				HeapToVEL.HeapValue.addValueEntryList(vel.Clone(), false);
		}
	}
	
	/****************************************************************************/
	/***                        APIs for initialization                       ***/
	/****************************************************************************/
	private static void ArgProcess(String[] args)
	{
		int k = 0;
		Constants.serIsForward = true;
		Constants.heapobject = false;
		while (k < args.length)
		{
			if (args[k].equalsIgnoreCase("--app"))
			{
				Constants.apkName = args[k + 1];
				FileAnalyzer.setAppName(args[k + 1]);
				k += 2;
				continue;
			}
			else if (args[k].equalsIgnoreCase("--backward"))
			{
				Constants.serIsForward = false;
				k++;
				continue;
			}
			else if (args[k].equalsIgnoreCase("--heapobject"))
			{
				Constants.heapobject = true;
				k += 2;
				continue;
			}
			else if (args[k].equalsIgnoreCase("--maxms"))
			{
				PropagateHelper.setMaxMainStream(Integer.parseInt(args[k + 1]));
				k += 2;
				continue;
			}
			else if (args[k].equalsIgnoreCase("--retrofit"))
			{
				Constants.setIsRetrofit(true);
				k++;
				continue;
			}
			k++;
		}
	}
	
	private static List<DPContainer> InitBackward() throws Exception
	{
		try
		{
			Constants.readDeobfuse(Constants.apkName);
			Constants.readignorelibrary(Constants.apkName);
			
			if(Constants.getIsRetrofit())
				RetrofitHandle.TransactionMapLoad();
			
			System.out.println("App Name : " + Constants.apkName + "\n");
			System.out.println("Loading Call Flow Graph ...");
			CFGSerializer CFGs = new CFGSerializer();
			Constants.sCFG = CFGs.Deserialize(ICFG_CASE.BACKWARD); // Not need to distinguish cfg between heap/backward analysis
			
			if (Constants.isDiffMode)
				DebugInfoDeserialize();
			else
				Constants.DebugInfoMap = new HashMap<String, DebugInfo>();
			
			// create SB_request object
			//Constants.SB_Request = new JSONBuilder();
			
			// Initialize currentEPCnt to zero
			currentEPCnt = 0;
			
			// Initialize semantic models
			UrlBuilder.initSemanticModels();
			
			if (Constants.heapobject)
				return TaintResultContainer.Deserialization(ICFG_CASE.HEAP);
			else if(ArgToVEL.isArgTracking())
				return TaintResultContainer.Deserialization(ICFG_CASE.ARG);
			else if (Constants.FindingEventHandler)
			    return TaintResultContainer.Deserialization(ICFG_CASE.UIFIND);
			else
				return TaintResultContainer.Deserialization(ICFG_CASE.BACKWARD);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	
	@SuppressWarnings("unchecked")
	private static void DebugInfoDeserialize() throws FileNotFoundException
	{
		String path = Constants.DebugPath();
		Kryo kryo = new Kryo();
		Input input = new Input(new FileInputStream(path));
		HashMap<String, DebugInfo> DiMap = kryo.readObject(input, HashMap.class);
		input.close();
		Constants.DebugInfoMap = (HashMap<String, DebugInfo>) DiMap.clone();
	}
	
	/****************************************************************************/
	/*** APIs for backward processing ***/
	/****************************************************************************/
	private static boolean SkipORSpecificDP(DPContainer DPC)
	{
		if (Constants.heapobject)
			return false;
		if (Constants.SpecificDP != null)
			if (!DPC.getDPMethod().equals(Constants.SpecificDP))
				return true;
		if (!(Constants.SkipDP.isEmpty()))
			if (Constants.SkipDP.contains(DPC.getDPMethod()))
				return true;
		return false;
	}
	
	private static boolean SkipORSpecificEP(EPContainer EPC)
	{
		// Skip the EP when the EP has been already analyzed before within the same DP
		//if (AnalyzedMethods.contains(CurrentEntryPoint))
			//return true;
		if(Constants.heapobject)
			return false;
		
		if (Constants.SpecificEP != null)
			if (!EPC.getEP().contains(Constants.SpecificEP))
				return true;
		if (Constants.SpecificEPList != null)
			if (Constants.SpecificEPList.size() > 0)
				if (!Constants.SpecificEPList.contains(EPC.getEP()))
					return true;
		if (!(Constants.SkipEP.isEmpty()))
			if (Constants.SkipEP.contains(EPC.getEP()))
				return true;
		return false;
	}
	
	private static boolean doesContainSpecificTaintMethod(EPContainer EPC) {
		if(Constants.SpecificTaintMethodList == null ||
				Constants.SpecificTaintMethodList.size() == 0)
			return true;
		else {
			for(String method: Constants.SpecificTaintMethodList) {
				if(!EPC.getTaintMethodSet().contains(method))
					return false;
			}
			return true;
		}
	}
	
	
	/****************************************************************************/
	/*** APIs for forward processing ***/
	/****************************************************************************/
	public static void SetForwardEP(String EPMethod){ Forward_EPMethod = EPMethod; }
	public static void SetForwardEPStmt(String EPStmt) { Forward_DPStmt = EPStmt; }
	
	public static void InitForwardEP(String EPmethod, String EPstmt){
		Forward_EPMethod = EPmethod;
		Forward_DPStmt = EPstmt;
		
		// TODO: need to handle various type according to DP stmt (BK)
		//BackendOutput.setSeedType(SEED_TYPE.DEST);
		
		//Forward_EPMethod = "<com.android.volley.toolbox.Volley: com.android.volley.RequestQueue newRequestQueue(android.content.Context,com.android.volley.toolbox.HttpStack)>";
		
		//Forward_EPMethod = "<com.android.volley.toolbox.PinterestJsonObjectRequest: void deliverResponse(com.pinterest.e.c.d)>";
		//Forward_DPStmt = "$r1 := @parameter0: com.pinterest.e.c.d";
	}
	
	/****************************************************************************/
	/***                                   Etc                                ***/
	/****************************************************************************/
	public static String getTimeString(double t)
	{
		if (t < 60)

			return t + " seconds";
		else if (t < 3600)
		{
			int m = ((int) t / 60);
			int s = (int) t - (m * 60);
			return m + "m " + s + "s (" + runtimeFormat.format(t) + " sec)";
		}
		else
		{
			int h = ((int) t / 3600);
			int m = ((int) t - (h * 3600)) / 60;
			int s = (int) t - (h * 3600) - (m * 60);
			return h + "h " + m + "m " + s + "s (" + runtimeFormat.format(t) + " sec)";
		}
	}
	
	public static void setMaxEPInitCurrEPCnt(int m) { maxEP = m; currentEPCnt = 0; }
	//public static int getMaxEP() { return maxEP; }
	public static boolean doesReachMaxEP() { return maxEP <= currentEPCnt;}
	public static void increaseEPCnt() { currentEPCnt++; }
	
	/****************************************************************************/
	/***                        APIs for initialization                       ***/
	/****************************************************************************/
	private static void countTotEP(List<DPContainer> dplist) {
		int cnt = 0;
		
		for (DPContainer dpc: dplist)
			cnt += dpc.getEPList().size();
		
		BackendOutput.totEPCnt = cnt;
		ExtractocolLogger.Log("Total # EPs: " + cnt);
	}
}
