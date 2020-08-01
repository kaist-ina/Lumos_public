package extractocol.common.UIfinder;

import soot.SootMethod;
import soot.Unit;

public class EPSeedPair {
    private String EP;
    private Unit Seed;
    private SootMethod SeedParentMethod;

    public String getEP() {
        return EP;
    }

    public void setEP(String EP) {
        this.EP = EP;
    }

    public Unit getSeed() {
        return Seed;
    }

    public void setSeed(Unit seed) {
        Seed = seed;
    }

    public SootMethod getSeedParentMethod() {
        return SeedParentMethod;
    }

    public void setSeedParentMethod(SootMethod seedParentMethod) {
        SeedParentMethod = seedParentMethod;
    }
}
