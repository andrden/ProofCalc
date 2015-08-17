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

    Expr quest(Rule q){
        System.out.println("\nQUEST:\n"+q+"\n");

        Expr expr = q.assertion;
        for(;;) {
            Expr exprNew = exprSimplifyDeep(expr);
            if( exprNew==null ){
                break;
            }else{
                expr = exprNew;
                System.out.println("QUEST res: "+expr);
            }
        }
        return expr;
    }

    Expr exprSimplifyDeep(Expr expr) {
        //System.out.println("Try simplify: "+expr);
        Expr enew = exprSimplify(expr);
        if( enew!=null ){
            return enew;
        }
        if( expr.sub==null ){
            return null;
        }
        for( int i=0; i<expr.sub.size(); i++ ){
            Expr e = exprSimplifyDeep(expr.sub.get(i));
            if( e!=null ){
                expr.sub.set(i, e);
                return expr;
            }
        }
        return null;
    }

    Expr exprSimplify(Expr expr) {
        for (Rule r : rules) {
            if (r.assertion.node.equals("=")) {
                Expr template = r.assertion.sub.get(0);
                Map<String, Expr> unifMap = template.unify(expr);
                if( unifMap!=null ) {
                    //System.out.println("unify with " + r + " results in " + unifMap);
                    Expr exprNew = r.assertion.sub.get(1).substitute(unifMap);
                    //System.out.println(expr + " ==simplified==> " + exprNew);
                    return exprNew;
                }
            }
        }
        return null;
    }
}
