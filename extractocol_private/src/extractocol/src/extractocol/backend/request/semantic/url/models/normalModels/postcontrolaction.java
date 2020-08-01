package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;

public class postcontrolaction extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if (spb.iie.getMethodRef().getSignature().equals("<org.cybergarage.upnp.Action: java.lang.String postControlAction()>"))
        {
            spb.CurrentPB.BT().RRI().SaveURI(".*");
        }
    }
}
