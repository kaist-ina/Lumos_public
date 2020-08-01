package extractocol.backend.request.semantic.url.models.normalModels;

import extractocol.backend.request.semantic.url.models.BaseModel;
import extractocol.backend.request.semantic.url.models.SemanticParameterBucket;
import soot.Scene;
import soot.SootClass;
import soot.SootField;

public class tojson extends BaseModel {
    @Override
    public void applySemantic(SemanticParameterBucket spb) {
        if (spb.iie.getMethodRef().toString().equals("<com.google.gson.Gson: java.lang.String toJson(java.lang.Object)>"))
        {
//            System.out.println(spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(0).toString()).getTypes());

            for (SootField fd : Scene.v().getSootClass(spb.CurrentPB.varTable.getValueEntryList(spb.iie.getArg(0).toString()).getTypes().get(0).toString()).getFields())
            {
                spb.CurrentPB.BT().RRI().addRequestBody(fd.getName(), ".*");
                GsonClassInterator(Scene.v().getSootClass(fd.getType().toString()), spb);
            }
//            System.out.println(spb.CurrentPB.BT().RRI().getRequestInfoEntry().Body);
        }
    }

    // It may has some of bugs. Honestly, it is a temporary solution.
    private void GsonClassInterator(SootClass sc, SemanticParameterBucket spb)
    {
        if (sc.getName().equals("com.winix.android.smartair.nike.network.http.data.RequestHeader"))
        {
            for (SootField sf : sc.getFields()) {
                spb.CurrentPB.BT().RRI().addRequestBody(sf.getName(), ".*");
            }
            return;
        }

        for (SootClass sc2 : Scene.v().getActiveHierarchy().getDirectSubclassesOf(sc))
        {
            if (spb.CurrentPB.BT().getWinixRequestBodyClass().equals(sc2.getName())) {
                for (SootField sf : sc2.getFields()) {
                    spb.CurrentPB.BT().RRI().addRequestBody(sf.getName(), ".*");
                }
            }
        }
    }
}
