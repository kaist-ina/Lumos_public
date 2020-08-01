package extractocol.common.UIfinder;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.VirtualInvokeExpr;

public class ListenerEntry {
    private Unit ut;
    private VirtualInvokeExpr vie;
    private SootMethod sm;

    public Unit getUt() {
        return ut;
    }

    public void setUt(Unit ut) {
        this.ut = ut;
    }

    public VirtualInvokeExpr getVie() {
        return vie;
    }

    public void setVie(VirtualInvokeExpr vie) {
        this.vie = vie;
    }

    public SootMethod getSm() {
        return sm;
    }

    public void setSm(SootMethod sm) {
        this.sm = sm;
    }
}
