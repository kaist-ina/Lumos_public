package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.Constants;
import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import extractocol.common.tools.Pair;
import extractocol.common.valueEntry.ValueEntryList;
import extractocol.common.valueEntry.node.Constant;

import java.util.ArrayList;
import java.util.HashMap;

public class a extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        String method = spb.sie.getMethodRef().toString();
        if (method.equals("<com.loopj.android.http.a: java.lang.String a(boolean,java.lang.String)>"))
        {
            spb.CurrentPB.varTable.OverWriteValueEntryListFromSrcToDest(spb.CurrentPB.strDest, spb.sie.getArg(1).toString(), false);
            spb.CurrentPB.BT().RRI().AddHTTPMethod("GET");
            spb.CurrentPB.BT().RRI().SaveURI(spb.CurrentPB, spb.CurrentPB.strDest);
        }
        // for nest
        else if (method.equals("<com.obsidian.v4.data.cz.service.z: com.obsidian.v4.data.cz.service.z a(org.json.JSONObject)>"))
        {
            HashMap<String, ValueEntryList> result = spb.CurrentPB.varTable.getValueEntryTable();
            ArrayList<Pair> kvpair = result.get(spb.iie.getArg(0).toString()).getMap();
            for (Pair pr : kvpair) {
                spb.CurrentPB.BT().RRI().addRequestBody(pr.getKey(), pr.getValue());
            }
        }
        // for nest
        else if (method.equals("<com.obsidian.v4.data.cz.service.z: com.obsidian.v4.data.cz.service.z a(java.lang.String,java.lang.String)>"))
        {
            if (spb.iie.getArg(0) instanceof Constant)
                spb.CurrentPB.BT().RRI().addRequestHeader(spb.iie.getArg(0).toString(), spb.iie.getArg(1).toString());
            else
                spb.CurrentPB.BT().RRI().addRequestHeader(spb.iie.getArg(0).toString(), spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(1).toString()).GenRegex());
        }

//        //for nest
//        else if (method.equals("<com.obsidian.v4.data.cz.service.z: com.obsidian.v4.data.cz.service.NetRequest a()>"))
//        {
//
//        }
    }
}
