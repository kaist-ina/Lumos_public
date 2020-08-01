package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import extractocol.common.tools.Pair;
import extractocol.common.valueEntry.ValueEntryList;

import java.util.ArrayList;
import java.util.HashMap;

public class getParams extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if (spb.iie.getMethodRef().getSignature().equals("<com.insteon.hub2.bean.Hub2Command: java.util.Map getParams()>"))
        {
            spb.CurrentPB.varTable.OverWriteValueEntryListFromSrcToDest(spb.strDst, spb.iie.getBase().toString(), false);
            HashMap<String, ValueEntryList> result = spb.CurrentPB.varTable.getValueEntryTable();
            ArrayList<Pair> kvpair = result.get(spb.iie.getBase().toString()).getMap();
            for (Pair pr : kvpair) {
                spb.CurrentPB.BT().RRI().addRequestBody(pr.getKey(), pr.getValue());
            }
        }
    }
}
