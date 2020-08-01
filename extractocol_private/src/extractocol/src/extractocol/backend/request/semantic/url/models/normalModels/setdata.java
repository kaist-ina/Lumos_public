package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import soot.jimple.Constant;

public class setdata extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if (spb.iie.getMethodRef().getSignature().equals("<com.logitech.harmonyhub.sdk.Request: void setData(java.lang.String,java.lang.String)>"))
        {
            if (spb.iie.getArg(1) instanceof Constant)
                spb.CurrentPB.BT().RRI().addRequestBody(spb.iie.getArg(0).toString(), spb.iie.getArg(1).toString());
            else {
                if (spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(1).toString()) != null)
                    spb.CurrentPB.BT().RRI().addRequestBody(spb.iie.getArg(0).toString(), spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(1).toString()).GenRegex());
                else
                    spb.CurrentPB.BT().RRI().addRequestBody(spb.iie.getArg(0).toString(), ".*");
            }
        }
    }
}
