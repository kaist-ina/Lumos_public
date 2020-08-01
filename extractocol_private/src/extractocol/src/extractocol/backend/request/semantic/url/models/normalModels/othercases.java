
package extractocol.backend.request.semantic.url.models.normalModels;

import java.util.ArrayList;

import extractocol.Constants;
import extractocol.backend.request.basics.BFNode;
import extractocol.backend.request.semantic.retrofit.retrofit_http_old;
import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import extractocol.common.UIfinder.EHSigManager;
import extractocol.common.UIfinder.EHSigPair;
import extractocol.common.UIfinder.EPSeedPair;
import extractocol.common.outputs.BackendOutput;
import extractocol.common.valueEntry.ValueEntry;
import soot.jimple.Constant;

public class othercases extends BaseModel
{
	private static void okHttpHandler(SemanticParameterBucket spb)
	{
		String method = Constants.Deobfuse(spb.iie.getMethodRef().getSignature().toString());
		if (method.startsWith("<com.squareup.okhttp.Request$Builder: com.squareup.okhttp.Request$Builder ")
				&& spb.iie.getArgCount() == 1 && spb.iie.getArg(0).getType().toString().equals("java.lang.String"))
		{
			//spb.BFTtable.put(spb.ub.strDest, spb.ub.CopyList(spb.BFTtable.get(spb.iie.getArg(0).toString())));
			
			//BK
			spb.CurrentPB.varTable.OverWriteValueEntryListFromSrcToDest(spb.ub.strDest, spb.iie.getArg(0).toString(), false);
			//Constants.BFTResultAlreadyApplied = true;
		}
		else if (method.startsWith("<com.squareup.okhttp.Request$Builder: com.squareup.okhttp.Request")
					&& spb.iie.getArgCount() == 0)
		{
			//spb.BFTtable.put(spb.ub.strDest, spb.ub.CopyList(spb.BFTtable.get(spb.iie.getBase().toString())));
			
			//BK
			spb.CurrentPB.varTable.OverWriteValueEntryListFromSrcToDest(spb.ub.strDest, spb.iie.getBase().toString(), false);
			//Constants.BFTResultAlreadyApplied = true;
		} 
		else if (method.startsWith("<com.squareup.okhttp.OkHttpClient: com.squareup.okhttp.Call")
				&& spb.iie.getArgCount() == 1
				&& spb.iie.getArg(0).getType().toString().equals("com.squareup.okhttp.Request")) 
		{
			String arg0 = spb.iie.getArg(0).toString();
			/*spb.ub.TrackingReg = arg0;
			spb.ub.printUrl(spb.CurrentPB, spb.BFTtable, spb.sm, spb.ut);*/
			
			// BK
			spb.CurrentPB.BT().RRI().AddHTTPMethod(spb.ub.isGet? "GET" : "POST");
			spb.CurrentPB.BT().RRI().SaveURI(spb.CurrentPB, arg0);
		} 
		else if (spb.iie.getMethodRef().getSignature()
				.equals("<com.squareup.okhttp.OkHttpClient: java.net.HttpURLConnection open(java.net.URL)>")) 
		{
			/*spb.ub.TrackingReg = spb.ub.strDest;
			ArrayList<BFNode> list = spb.BFTtable.get(spb.iie.getArg(0).toString());
			spb.BFTtable.put(spb.ub.strDest, spb.ub.CopyList(list));*/
			
			//BK
			spb.CurrentPB.varTable.OverWriteValueEntryListFromSrcToDest(spb.ub.strDest, spb.iie.getArg(0).toString(), false);
			//Constants.BFTResultAlreadyApplied = true;
		} 
		else if (method.startsWith("<com.squareup.okhttp.Request$Builder: com.squareup.okhttp.Request$Builder")
				&& spb.iie.getArgCount() == 1
				&& spb.iie.getArg(0).getType().toString().contains("com.squareup.okhttp.")) 
		{
			/*spb.ub.TrackingReg = spb.ub.strDest;
			ArrayList<BFNode> list = spb.BFTtable.get(spb.iie.getArg(0).toString());
			spb.BFTtable.put(spb.ub.strDest, spb.ub.CopyList(list));*/
			
			//BK
			spb.CurrentPB.varTable.OverWriteValueEntryListFromSrcToDest(spb.ub.strDest, spb.iie.getArg(0).toString(), false);
			//Constants.BFTResultAlreadyApplied = true;
		} 
		else if (method.contains(
				"<com.squareup.okhttp.Request$Builder: com.squareup.okhttp.Request$Builder method(java.lang.String,com.squareup.okhttp.RequestBody)>")) 
		{
			/*String methodstring = spb.ub.GenRegex(null, spb.BFTtable, spb.iie.getArg(0).toString());
			String url = spb.ub.GenRegex(null, spb.BFTtable, spb.iie.getBase().toString());
			String requestbody = spb.ub.GenRegex(null, spb.BFTtable, spb.iie.getArg(1).toString());
			if (!requestbody.equals(""))
				url += requestbody;
			url = methodstring + url;
			BFNode bfn = new BFNode();
			bfn.makeUrlBfn(url);
			ArrayList<BFNode> list = new ArrayList<BFNode>();
			list.add(bfn);
			spb.BFTtable.put(spb.ub.strDest, list);*/
			
			// BK 
			String methodstring = spb.CurrentPB.varTable.GenRegex(spb.iie.getArg(0).toString());
			String url = spb.CurrentPB.varTable.GenRegex(spb.iie.getBase().toString());
			String requestbody = spb.CurrentPB.varTable.GenRegex(spb.iie.getArg(1).toString());
			if (!requestbody.equals(""))
				url += requestbody;
			url = methodstring + url;
			spb.CurrentPB.varTable.setConstantValue(spb.ub.strDest, url, false);
			//Constants.BFTResultAlreadyApplied = true;
		} 
		else if (method.contains("<com.squareup.okhttp.Request: com.squareup.okhttp.Request$Builder newBuilder()>")) 
		{
			//spb.BFTtable.put(spb.ub.strDest, spb.ub.CopyList(spb.BFTtable.get(spb.iie.getBase().toString())));
			
			//BK
			spb.CurrentPB.varTable.OverWriteValueEntryListFromSrcToDest(spb.ub.strDest, spb.iie.getBase().toString(), false);
			//Constants.BFTResultAlreadyApplied = true;
		}
	}
	
	@Override
	public void applySemantic(SemanticParameterBucket spb) {
        // JM for obfuscated OkhttpLib
        if (spb.methodref.toString().startsWith("<com.squareup.okhttp")) {
            okHttpHandler(spb);
        } else if (spb.methodref.toString().endsWith("android.view.View findViewById(int)>")) {
//            System.out.println("findViewById: " + spb.iie.getArg(0).toString());
//            System.out.println("findViewById stmt: " + spb.iie.toString() + " in " + spb.CurrentPB.CurrentSM.getSignature());
            if (spb.iie.getArg(0) instanceof Constant)
                spb.CurrentPB.varTable.addValueEntry(spb.ub.strDest, spb.iie.getArg(0).toString(), ValueEntry.SOURCE_TYPE.CONSTANT, false);
            else {
//                System.out.println("Heap object - res id: " + spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(0).toString()).GenRegex());
                spb.CurrentPB.varTable.addValueEntryList(spb.ub.strDest, spb.iie.getArg(0).toString(), false);
            }
        }
        else if (spb.methodref.toString().endsWith("setOnClickListener(android.view.View$OnClickListener)>")
                || spb.methodref.toString().endsWith("setOnSeekBarChangeListener(android.widget.SeekBar$OnSeekBarChangeListener)>")
                || spb.methodref.toString().endsWith("setOnTouchListener(android.view.View$OnTouchListener)>"))
        {
            if (spb.CurrentPB.BT().getDPStmt().contains(spb.iie.toString())) {
                EPSeedPair targetEP = null;
                for (EPSeedPair EPseed : EHSigManager.ObtainedSeeds) {
//                    System.out.println("EP: " + EHsigpair.getSeed());

                    if (EPseed.getSeed().equals(spb.ut))
                    {
//                        System.out.println("Found target EHsigpair.");
                        targetEP = EPseed;
                        break;
                    }
                }

                for (EHSigPair EHpair : EHSigManager.results)
                    for (String EH : EHpair.getEH())
                    {
                        if (EH.equals(targetEP.getEP()))
                        {
                            String hexRegex = spb.CurrentPB.varTable.GenRegex(spb.iie.getBase().toString());
                            if (hexRegex.contains("|"))
                            {
                                String[] splitedRes = hexRegex.split("\\|");
                                for (String res : splitedRes)
                                    EHpair.addHexid(res.replaceAll("\\(", "").replaceAll("\\)", ""));
                        }
                            else
                                EHpair.addHexid(hexRegex);
                        }
                    }

//                    if (EHsigpair.getEP().equals(spb.CurrentPB.BT().getDPMethod()))
//                System.out.println("setOnClickListener: " + spb.CurrentPB.varTable.GenRegex(spb.iie.getBase().toString()));
            }
        }
    }

}
