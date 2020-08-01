package extractocol.common.UIfinder;

import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashSet;

public class EHSigPair {
    public HashSet<String> getHexid() {
        return Hexids;
    }

    public void addHexid(String hexid) {
        Hexids.add(hexid);
    }

    public ArrayList<String> getEH() {
        return EH;
    }

    public void setEH(ArrayList<String> EH) {
        this.EH = EH;
    }

    public String getEP() {
        return EP;
    }

    public void setEP(String EP) {
        this.EP = EP;
    }

    private HashSet<String> Hexids = new HashSet<>();
    private ArrayList<String> EH;
    private String EP;
}
