package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import extractocol.common.tools.Pair;
import extractocol.common.valueEntry.ValueEntryList;

import java.util.ArrayList;
import java.util.HashMap;

public class sendSync extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if (spb.iie.getMethodRef().getSignature().equals("<com.insteon.hub2.util.PubNubHelper: com.insteon.hub2.bean.Hub2Response sendSync(com.insteon.hub2.bean.Hub2Command)>"))
        {
            //It should be changed to using Heapfinder.
            spb.CurrentPB.BT().RRI().SaveURI("https://pubsub.pubnub.com/(publish|subscribe)/.*");
            HashMap<String, ValueEntryList> result = spb.CurrentPB.varTable.getValueEntryTable();
            if (result.get(spb.iie.getArg(0).toString()) != null) {
                ArrayList<Pair> kvpair = result.get(spb.iie.getArg(0).toString()).getMap();
                for (Pair pr : kvpair) {
                    spb.CurrentPB.BT().RRI().addRequestBody(pr.getKey(), pr.getValue());
                }
            }
        }
    }
}
