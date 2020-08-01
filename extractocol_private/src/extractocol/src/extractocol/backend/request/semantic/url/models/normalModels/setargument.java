package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import soot.jimple.Constant;

public class setargument extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if (spb.iie.getMethodRef().getSignature().equals("<com.belkin.cybergarage.wrapper.UpnpDeviceList: boolean setArgument(org.cybergarage.upnp.Action,java.lang.String[],java.lang.String[])>"))
        {
            //we need to use heapfinder to find bodykey. typically body key is a heap object.
            spb.CurrentPB.BT().RRI().addRequestBody("SOAP Body key" , spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(1).toString()).GenRegex());
        }
    }
}
