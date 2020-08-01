package extractocol.common.UIfinder;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.VirtualInvokeExpr;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EHSigManager {
    public static ArrayList<EHSigPair> results = null;
    public transient static ArrayList<ListenerEntry> AllofsetOnListeners = new ArrayList<ListenerEntry> ();
    public static ArrayList<EPSeedPair> ObtainedSeeds = new ArrayList<>();


    public static void addListener(Unit ut, VirtualInvokeExpr vie, SootMethod sm)
    {
        ListenerEntry le = new ListenerEntry();
        le.setSm(sm);
        le.setVie(vie);
        le.setUt(ut);
        AllofsetOnListeners.add(le);
    }

    public static void setRetultArray(ArrayList<EHSigPair> _results)
    {
        results = _results;
    }

    public static void addPair(EHSigPair _bke)
    {
        if (results != null)
            results.add(_bke);
        else
            System.out.println("EH-Sig pair is null.");
    }

    public static void ButterKnifeParsing(){
        for (SootClass sc : Scene.v().getClasses())
        {
            if (sc.getName().endsWith("$$ViewBinder") || sc.getName().contains("_ViewBinding"))
            {
                for (SootMethod sm: sc.getMethods())
                {
                    // Target signature: butterknife.Unbinder a(butterknife.internal.Finder, com.smartthings.android.battery.DozeFragment, java.lang.Object)
                    if (isButterKnifeBinder(sm, sc.getName()))
                    {
                        ParseBindingMethod(sm, sc.getName());
                    }
                }
            }
        }
    }

    public static boolean containsEH(SootMethod target)
    {
        for (EHSigPair bke : results)
        {
            for (String EH : bke.getEH())
            {
                if (EH.equals(target))
                    return true;
            }
        }
        return false;
    }

    private static void ParseBindingMethod(SootMethod sm, String scName) {
        for (Unit ut : sm.getActiveBody().getUnits())
        {
            if (ut instanceof AssignStmt)
            {
                AssignStmt as = (AssignStmt) ut;
                if (as.containsInvokeExpr()) {
                    if (as.getInvokeExpr() instanceof VirtualInvokeExpr) {
                        VirtualInvokeExpr vie = (VirtualInvokeExpr) as.getInvokeExpr();
                        if (vie.getMethod().getSignature().equals("<butterknife.internal.Finder: java.lang.Object findRequiredView(java.lang.Object,int,java.lang.String)>")) {
                            ParseMethodandHexid(vie, scName);
                        }
                    }
                    else if (as.getInvokeExpr() instanceof StaticInvokeExpr) {
                        StaticInvokeExpr sie = (StaticInvokeExpr) as.getInvokeExpr();
                        if (sie.getMethod().getSignature().equals("<butterknife.internal.Utils: android.view.View findRequiredView(android.view.View,int,java.lang.String)>")) {
                            ParseMethodandHexid(sie, scName);
                        }
                    }
                }
            }
        }
    }

    private static void ParseMethodandHexid(VirtualInvokeExpr vie, String scName) {

        //Param3: "field \'floatingSaveButton\' and method \'onFloatingSaveButtonClick\'"
        if (vie.getArg(2).toString().contains("method")) {
            EHSigPair bke = new EHSigPair();
            bke.addHexid(Integer.toString(Integer.parseInt(vie.getArg(1).toString()), 16));
            String Param3 = vie.getArg(2).toString();
            ArrayList<String> strTargetSMs = extractEventHandlerName(Param3);
            ArrayList<String> TargetSMs = new ArrayList<String>();
//            System.out.println("HEX-id: 0x" + Integer.toString(Integer.parseInt(vie.getArg(1).toString()), 16));

            for (String targetSM : strTargetSMs) {
                SootClass targetSc = Scene.v().getSootClass(scName.replace("$$ViewBinder", ""));
                TargetSMs.add(targetSc.getMethodByName(targetSM).getSignature());
//                System.out.println("Param3: " + targetSM);
            }

            bke.setEH(TargetSMs);
            results.add(bke);
        }
    }

    private static void ParseMethodandHexid(StaticInvokeExpr sie, String scName) {

        //Param3: "field \'floatingSaveButton\' and method \'onFloatingSaveButtonClick\'"
        if (sie.getArg(2).toString().contains("method")) {
            EHSigPair bke = new EHSigPair();
            bke.addHexid(Integer.toString(Integer.parseInt(sie.getArg(1).toString()), 16));
            String Param3 = sie.getArg(2).toString();
            ArrayList<String> strTargetSMs = extractEventHandlerName(Param3);
            ArrayList<String> TargetSMs = new ArrayList<String>();
//            System.out.println("HEX-id: 0x" + Integer.toString(Integer.parseInt(sie.getArg(1).toString()), 16));

            for (String targetSM : strTargetSMs) {
                SootClass targetSc = Scene.v().getSootClass(scName.replace("_ViewBinding", ""));
                TargetSMs.add(targetSc.getMethodByName(targetSM).getSignature());
//                System.out.println("Param3: " + targetSM);
            }

            bke.setEH(TargetSMs);
            results.add(bke);
        }
    }

    public static ArrayList<String> extractEventHandlerName(String param3)
    {
        ArrayList<String> results = new ArrayList<String> ();
        Pattern p = Pattern.compile("method\\s\\\\'([a-z,A-Z]+)[\\\\']");
        Matcher m = p.matcher(param3);
        while (m.find())
        {
            results.add(m.group(1));
        }
        return results;
    }

    private static boolean isButterKnifeBinder(SootMethod sm, String name) {
        if (sm.getReturnType().toString().equals("butterknife.Unbinder")
                && sm.getParameterCount() == 3 && sm.getParameterTypes().get(1).toString().equals(name.substring(0, name.indexOf("$$"))))
            return true;
        //void <init>(com.august.luna.ui.main.doorbell.VideoStreamControlFragment, android.view.View)

        if (sm.getName().equals("<init>") && sm.getParameterCount() ==2) {
            if (sm.getParameterTypes().get(0).toString().equals(name.substring(0, name.indexOf("_")))
                    && sm.getParameterTypes().get(1).toString().equals("android.view.View"))
                return true;
        }
        return false;
    }

    public static void PrintAll()
    {
        for (int i = 0; i < EHSigManager.results.size(); i++ )
        {
            System.out.println("EP[" +i+"]: " + EHSigManager.results.get(i).getEP());
            for (int j =0; j < EHSigManager.results.get(i).getEH().size(); j++) {
                System.out.println("\tCandidate[" + j + "]: " + EHSigManager.results.get(i).getEH().get(j));
                System.out.println("\tCandidate[" + j + "]: " + EHSigManager.results.get(i).getHexid());
            }
        }
    }

    public static void FindSetListners()
    {
        for (int i = 0; i < EHSigManager.results.size(); i++)
        {
            for (int j = 0; j < EHSigManager.results.get(i).getEH().size(); j++)
            {
                for (ListenerEntry le : AllofsetOnListeners) {
                    SootMethod sm = Scene.v().getMethod(EHSigManager.results.get(i).getEH().get(j));
                    for (SootClass subclass : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(sm.getDeclaringClass())) {
                        if (le.getVie().getArg(0).getType().toString().equals(subclass.getName())) {

                            EPSeedPair eps = new EPSeedPair();
                            eps.setEP(sm.getSignature());
                            eps.setSeed(le.getUt());
                            eps.setSeedParentMethod(le.getSm());
                            ObtainedSeeds.add(eps);

                            System.out.println("EH: " + sm.getSignature());
                            System.out.println("Taint Seed: " + le.getVie() + " in " + le.getSm());
                        }
                    }
                }
            }
        }
    }
}
