package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;

public class buildUpon extends BaseModel
{
	@Override
	public void applySemantic(SemanticParameterBucket spb)
	{
		if (spb.iie.getMethodRef().getSignature().equals("<android.net.Uri: android.net.Uri$Builder buildUpon()>"))
		{
			/*spb.BFTtable.put(spb.ub.strDest, spb.ub.CopyList(spb.BFTtable.get(spb.iie.getBase().toString())));
			spb.ub.TrackingReg = spb.ub.strDest;*/

			if (spb.CurrentPB.varTable.getValueEntryList(spb.iie.getBase().toString()) != null)
				spb.CurrentPB.varTable.AppendURLPathToStringBuilder(spb.CurrentPB.strDest, spb.CurrentPB.varTable.getValueEntryList(spb.iie.getBase().toString()).GenRegex());
			else
				spb.CurrentPB.varTable.AppendURLPathToStringBuilder(spb.CurrentPB.strDest, "");
//			spb.CurrentPB.varTable.OverWriteValueEntryListFromSrcToDest(spb.CurrentPB.strDest, spb.iie.getBase().toString(), false);
		}
	}
}
