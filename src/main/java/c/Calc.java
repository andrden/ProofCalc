package c;

import java.util.List;
import java.util.Map;

/**
 * Created by denny on 8/11/15.
 */
public class Calc {
    List<Rule> rules;

    public Calc(List<Rule> rules) {
        this.rules = rules;
    }

    void quest(Rule q){
        System.out.println("\nQUEST:\n"+q+"\n");

        Expr expr = q.assertion;
        for(;;) {
            boolean changed = false;
            for (Rule r : rules) {
                if (r.assertion.node.equals("=")) {
                    Map<String, Expr> unifMap = r.assertion.sub.get(0).unify(expr);
                    if( unifMap!=null ) {
                        System.out.println("unify with " + r + " results in " + unifMap);
                        expr = r.assertion.sub.get(1).substitute(unifMap);
                        System.out.println("simplified: " + expr);
                        changed = true;
                        break;
                    }
                }
            }
            if( ! changed ){
                break;
            }
        }
    }
}
