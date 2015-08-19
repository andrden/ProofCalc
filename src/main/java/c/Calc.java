package c;

import java.util.ArrayList;
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

    Expr quest(Rule q){
        System.out.println("\nQUEST:\n"+q+"\n");

        Expr expr = q.assertion;
        for(;;) {
            List<Expr> exprNew = exprSimplifyDeep(expr);
            if( exprNew.isEmpty() ){
                break;
            }else{
                //expr = exprNew.get(0);
                expr = shortest(exprNew);
                System.out.println("QUEST res: "+expr.toMathString());
            }
        }
        return expr;
    }

    Expr shortest(List<Expr> exprNew){
        Expr sh=null;
        for( Expr e : exprNew ){
            if( sh==null || sh.toLispString().length()>e.toLispString().length() ){
                sh = e;
            }
        }
        return sh;
    }

    List<Expr> exprSimplifyDeep(Expr expr) {
        List<Expr> ways = exprSimplify(expr);
        if( expr.sub!=null ) {
            for (int i = 0; i < expr.sub.size(); i++) {
                List<Expr> elist = exprSimplifyDeep(expr.sub.get(i));
                for( Expr e : elist ){
                    Expr clone = expr.shallowClone();
                    clone.sub.set(i, e);
                    ways.add(clone);
                }
            }
        }
        return ways;
    }

    List<Expr> exprSimplify(Expr expr) {
        List<Expr> ways = new ArrayList<>();
        for (Rule r : rules) {
            if (r.assertion.node.equals("=")) {
                Expr template = r.assertion.sub.get(0);
                Map<String, Expr> unifMap = template.unify(expr);
                if( unifMap!=null ) {
                    //System.out.println("unify with " + r + " results in " + unifMap);
                    Expr exprNew = r.assertion.sub.get(1).substitute(unifMap);
                    //System.out.println(expr + " ==simplified==> " + exprNew);
                    ways.add(exprNew);
                }
            }
        }
        return ways;
    }
}
