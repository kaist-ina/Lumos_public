package extractocol.tester;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.tree.CommonTree;

import QueryConvertor.QueryConverter;
import extractocol.Constants;
import extractocol.common.outputs.backendOutputHelper.ReqRespInfo;
import extractocol.common.regex.RegexHandler;
import extractocol.common.regex.basic.RegexNode;
import extractocol.common.retrofit.RetrofitBaseURLTracker;
import extractocol.common.retrofit.RetrofitParse;
import extractocol.common.retrofit.struct.Transaction;
import extractocol.common.retrofit.utils.FileAnalyzer;
import extractocol.common.retrofit.utils.InnerClassAnalyzer;
import extractocol.common.retrofit.utils.JavaFileAnalyzer;
import extractocol.common.retrofit.utils.MethodPrototype;
import extractocol.common.retrofit.utils.ResponseFileAnalyzer;
import extractocol.common.trackers.ImplicitCallEdgeTracker;
import extractocol.common.trackers.IntentMapTracker;
import extractocol.common.trackers.tools.ArgToVEL;
import extractocol.common.trackers.tools.HeapToString;
import extractocol.common.trackers.tools.HeapToVEL;
import extractocol.common.valueEntry.ValueEntryList;
import extractocol.frontend.helper.PropagateHelper;
import extractocol.frontend.output.basic.TaintResultEntry;
import pcreparser.PCRE;
import pcreparser.PCRELexer;
import pcreparser.PCREParser;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.DOTTreeGenerator;
import org.antlr.stringtemplate.StringTemplate;

public class ForTest {

    @SuppressWarnings("unchecked")
    private static void walk(CommonTree tree, StringBuilder builder) {

        List<CommonTree> firstStack = new ArrayList<CommonTree>();
        firstStack.add(tree);

        List<List<CommonTree>> childListStack = new ArrayList<List<CommonTree>>();
        childListStack.add(firstStack);

        while (!childListStack.isEmpty()) {

            List<CommonTree> childStack = childListStack.get(childListStack.size() - 1);

            if (childStack.isEmpty()) {
                childListStack.remove(childListStack.size() - 1);
            }
            else {
                tree = childStack.remove(0);

                String indent = "";

                for (int i = 0; i < childListStack.size() - 1; i++) {
                    indent += (childListStack.get(i).size() > 0) ? "|  " : "   ";
                }

                String tokenName = PCREParser.tokenNames[tree.getType()];
                String tokenText = tree.getText();

                builder.append(indent)
                        .append(childStack.isEmpty() ? "'- " : "|- ")
                        .append(tokenName)
                        .append(!tokenName.equals(tokenText) ? "='" + tree.getText() + "'" : "")
                        .append("\n");

                if (tree.getChildCount() > 0) {
                    childListStack.add(new ArrayList<CommonTree>((List<CommonTree>)tree.getChildren()));
                }
            }
        }
    }
    
	public static void main(String[] args) {
		Constants.setAPPName("postmates");
		FileAnalyzer.setAppName("postmates");
		FileAnalyzer.debug=true;
		String path = "E:\\extractocol_private\\SerializationFiles\\postmates\\java\\com\\postmates\\android\\webservice\\APIService.java";
		
		List<Transaction> res = null;
		try {
			res = JavaFileAnalyzer.parser(path);
		//InnerClassAnalyzer.Parser(path, "Places", new Transaction(), new ArrayList<String>(), new Stack<String>(), new ArrayList<String>());
		}catch (Exception e) {
			
		}
		
		System.out.println("Finished!");
		System.exit(0);
	}

}

class myString {
	String s;
	
	public myString(String _s) {
		this.s = _s;
	}
	
	public String getString() {return this.s; }
	
	public boolean equals(myString other) {
		return (other.s.equals(this.s));
	}
}

class myThread implements Runnable{
	public static BitSet bm = new BitSet();
	int myI;
	public myThread(int i) {
		this.myI = i;
	}
	
	public void run() {
		bm.set(this.myI, true);
		System.out.println("Done: " + this.myI);
	}
}
