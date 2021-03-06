
package ResponseEp_Extractor.BeeFramework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import ResponseEp_Extractor.EPCandidate;
import ResponseEp_Extractor.ExtractorUtils;
import extractocol.Constants;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.options.Options;

public class fivemiles_Case
{
	public static void main(String args[]) throws IOException
	{
		/*
		 * Arg0 : TargetClassName Arg1 : Sig Method 5miles - BeeFramework
		 */
		final ArrayList<String> SigMethod = new ArrayList<String>();
		SigMethod.add("<com.insthub.fivemiles.a.ap: com.external.androidquery.a ajaxProgress(com.external.androidquery.b.d)>");
		SigMethod.add("<com.insthub.fivemiles.a.be: com.external.androidquery.a ajax(com.external.androidquery.b.d)>");

		// Variables
		Constants.apkName = "5miles";
		/*
		 * Start Soot Main
		 */
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_allow_phantom_refs(true);

		final ArrayList<EPCandidate> EpList = new ArrayList<EPCandidate>();

		PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer()
		{

			@Override
			protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options)
			{
				final PatchingChain<Unit> units = b.getUnits();
				final EPCandidate EPcan = new EPCandidate();

				// important to use snapshotIterator here
				for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();)
				{
					final Unit u = iter.next();
					u.apply(new AbstractStmtSwitch()
					{

						public void caseInvokeStmt(InvokeStmt stmt)
						{
							if (SigMethod.contains(stmt.getInvokeExpr().getMethodRef().getSignature()))
							{
								SootClass thisClass = Scene.v().getSootClass(stmt.getInvokeExpr().getArg(0).getType().toString());
								if (thisClass.isConcrete())
								{
									EPcan.setSuperclasses(Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(thisClass));
									EPcan.setEPSig(stmt.getInvokeExpr().getArg(0).getType().toString());
								}
							}
						}

						public void caseAssignStmt(AssignStmt stmt)
						{

						}

						public void caseReturnStmt(ReturnStmt stmt)
						{

						}
					});
				}

				if (EPcan.getEPSig() != null)
					EpList.add(EPcan);
			}
		}));

		soot.Main.main(args);
		int count = 0;
		for (EPCandidate epc : EpList)
		{
			// System.out.println("EP : " + epc.getEPSig());
			// System.out.println("Sup : " + epc.getSuperclasses());
			for (SootClass sup : epc.getSuperclasses())
			{
				for (SootMethod sm : sup.getMethods())
				{
					if (!sm.getSignature().startsWith("<com.BeeFramework.b.c") && sm.getName().equals("callback") && sm.getParameterCount() == 3)
					{
						if (sm.getParameterType(0).toString().equals("java.lang.String")
								&& sm.getParameterType(1).toString().equals("org.json.JSONObject"))
						{
							epc.setEPSig(sm.getSignature());
							epc.setDPStmt("$r2 := @parameter1: org.json.JSONObject");
							System.out.println("Found EP[" + count++ + "] : " + sm.getSignature());
						}
					}
				}
			}
		}
		ExtractorUtils.WriteEPs(EpList);
	}
}
