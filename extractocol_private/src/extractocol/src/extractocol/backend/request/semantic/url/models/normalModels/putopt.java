package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import extractocol.common.tools.Pair;
import extractocol.common.valueEntry.ValueEntryList;

import java.util.ArrayList;
import java.util.HashMap;

public class putopt extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if (spb.iie.getMethodRef().getSignature().equals("<org.json.JSONObject: org.json.JSONObject putOpt(java.lang.String,java.lang.Object)>"))
        {
            String baseVar = spb.iie.getBase().toString();
            if (spb.strDst != null && !spb.strDst.equals(baseVar)) {
                spb.CurrentPB.varTable.addMapValue(baseVar, spb.iie.getArg(0), spb.iie.getArg(1), true);
                spb.CurrentPB.varTable.setValueEntryList(spb.strDst, spb.CurrentPB.varTable.getValueEntryList(baseVar), true);
            }
            else if (spb.iie.getArg(1).getType().toString().equals("org.json.JSONObject")) {

                HashMap<String, ValueEntryList> result = spb.CurrentPB.varTable.getValueEntryTable();
                ArrayList<Pair> kvpair = result.get(spb.iie.getArg(1).toString()).getMap();
                for (Pair pr : kvpair) {
                    spb.CurrentPB.varTable.addMapValue(baseVar, pr.getKey(), pr.getValue(), true);
                }
            }
            else
                spb.CurrentPB.varTable.addMapValue(baseVar, spb.iie.getArg(0), spb.iie.getArg(1), true);
        }
    }
}
