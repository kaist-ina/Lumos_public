package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;

public class setbody extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if(spb.iie.getMethodRef().toString().equals("<com.winix.android.smartair.nike.network.http.data.RequestWinix: void setBody(com.winix.android.smartair.nike.network.http.data.ReqBody)>"))
        {
            spb.CurrentPB.BT().setWinixRequestBodyClass(spb.iie.getArg(0).getType().toString());
        }
    }
}
