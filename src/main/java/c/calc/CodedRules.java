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
        stripQuantifier(expr);
    }

    public List<FringeEl> getWays() {
        return ways;
    }

    void limit0Const(Expr expr) {
        if( expr.node.equals("apply") && expr.child(0).node.equals("lim0") && expr.child(1).node.equals("func") ){
            String var = expr.child(1).child(0).node;
            Set<String> usedVars = expr.child(1).child(1).freeVariables();
            if( ! usedVars.contains(var) ){
                ways.add( new FringeEl(expr.child(1).child(1), new NamedRule("lim0-const"), null) );
            }
        }
    }

    void stripQuantifier(Expr expr) {
        if( expr.node.equals("∃") || expr.node.equals("∀") ){
            String var = expr.child(0).node;
            Expr nested = expr.lastChild();
            Set<String> usedVars = nested.freeVariables();
            if( ! usedVars.contains(var) ){
                ways.add( new FringeEl(nested, new NamedRule("stripQuantifier"), null) );
            }
        }
    }
}
