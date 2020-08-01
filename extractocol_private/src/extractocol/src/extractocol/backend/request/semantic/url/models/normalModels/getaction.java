package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import soot.jimple.Constant;

public class getaction extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if (spb.iie.getMethodRef().getSignature().equals("<org.cybergarage.upnp.Device: org.cybergarage.upnp.Action getAction(java.lang.String)>"))
        {
            if (spb.iie.getArg(0) instanceof Constant)
                spb.CurrentPB.BT().RRI().addRequestBody("SOAP Action" , spb.iie.getArg(0).toString().replaceAll("\"", ""));
            else if (spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(0).toString()) != null)
                spb.CurrentPB.BT().RRI().addRequestBody("SOAP Action" , spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(0).toString()).GenRegex().replaceAll("\"", ""));
            else
                spb.CurrentPB.BT().RRI().addRequestBody("SOAP Action" , ".*");
        }
    }
}
