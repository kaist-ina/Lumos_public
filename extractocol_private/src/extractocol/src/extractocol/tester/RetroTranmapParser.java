package extractocol.tester;

import extractocol.Constants;
import extractocol.common.retrofit.struct.Param;
import extractocol.common.retrofit.struct.Transaction;

import java.util.Map;

public class RetroTranmapParser {
    public static void main (String[] args)
    {
        Constants.setAPPName(args[0]);
        Map<String, Transaction> TranMap = Transaction.Deserialize();

        System.out.println("Size: " + TranMap.size());
        for (Transaction tr : TranMap.values())
        {
            System.out.println(tr.Request().getHttpMethod() + " " + tr.Request().getBaseUrl() + " " + tr.Request().getSubUrl());
            System.out.println("Param List");
            for (Param pr : tr.getParams()) {
                System.out.println("\t" + pr.getKeyword() + " : " + pr.getRetrofitType());
            }
        }
        System.out.println("Finished printing TranMap");
    }
}
