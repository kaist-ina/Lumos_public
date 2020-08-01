
package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import soot.jimple.Constant;

public class appendQueryParameter extends BaseModel
{
	@Override
	public void applySemantic(SemanticParameterBucket spb)
	{
		if (spb.iie.getMethodRef().toString().equals(
				"<android.net.Uri$Builder: android.net.Uri$Builder appendQueryParameter(java.lang.String,java.lang.String)>"))
		{
			if (spb.iie.getArg(0) instanceof Constant)
			{
				if (spb.iie.getArg(1) instanceof  Constant)
					spb.CurrentPB.BT().RRI().addRequestQuery(spb.iie.getArg(0).toString(), spb.iie.getArg(1).toString());
				else {
					spb.CurrentPB.BT().RRI().addRequestQuery(spb.iie.getArg(0).toString(), ".*");
				}
			}
		}
	}
}
