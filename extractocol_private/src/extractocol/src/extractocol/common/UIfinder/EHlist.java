package extractocol.common.UIfinder;

import soot.SootMethod;
import soot.Unit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EHlist {
    public static Set<String> EHs = new HashSet<String>();
    //ParentEHs is temporary. It will be changed to be found automatically.
    public static ArrayList<String> ParentEHs = new ArrayList<String>();
    public static ArrayList<String> EHObjs = new ArrayList<String>();

    static {
        EHs.add("onClick");
        EHs.add("onCreate");
        EHs.add("onItemClick");

        ParentEHs.add("<com.netflix.mediaclient.util.MdxUtils: android.support.v7.app.AlertDialog createMdxDisconnectDialog(com.netflix.mediaclient.android.activity.NetflixActivity,com.netflix.mediaclient.util.MdxUtils$MdxTargetSelectionDialogInterface)>");
        EHObjs.add("specialinvoke $r15.<com.netflix.mediaclient.util.MdxUtils$2: void <init>(com.netflix.mediaclient.android.activity.NetflixActivity)>($r0)");
    }

    public static Unit findTargetUnit(SootMethod sm, String targetObj)
    {
        for (Unit ut : sm.getActiveBody().getUnits())
        {
            if (ut.toString().contains("new " + targetObj)) {
                System.out.println("Target Unit found: " + ut.toString());
                return ut;
            }
        }
        return null;
    }
}
