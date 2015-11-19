package c.calc;

import c.model.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by denny on 11/19/15.
 */
public class CodedRules {
    List<FringeEl> ways = new ArrayList<>();

    CodedRules(Expr expr){
        limit0Const(expr);
    }

    public List<FringeEl> getWays() {
        return ways;
    }

    void limit0Const(Expr expr) {
        if( expr.node.equals("apply") && expr.sub.get(0).node.equals("lim0") && expr.sub.get(1).node.equals("func") ){
            String var = expr.sub.get(1).sub.get(0).node;
            Set<String> usedVars = expr.sub.get(1).sub.get(1).freeVariables();
            if( ! usedVars.contains(var) ){
                ways.add( new FringeEl(expr.sub.get(1).sub.get(1), new NamedRule("lim0-const"), null) );
            }
        }
    }
}
