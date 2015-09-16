package c.calc;

import c.Expr;
import c.Rule;

import java.util.*;
import java.util.function.Predicate;

/**
 * Created by denny on 8/11/15.
 */
public class Calc {
    List<Rule> rules;

    AssocCommutCancelRule plusMinus = new AssocCommutCancelRule("+","-","0");
    AssocCommutCancelRule multDiv = new AssocCommutCancelRule("*","/","1");

    public Calc(List<Rule> rules) {
        this.rules = rules;
    }

    public Expr quest(Rule q, Predicate<Expr> checkIfAnswer){
        System.out.println("\n================QUEST:\n"+q+"\n");


        Expr expr = q.assertion;
        expr = plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(expr)));
//        if( ! expr.equals(q.assertion) ){
//            System.out.println("QUEST try: "+expr.toMathString());
//        }

        Set<FringeEl> fringe = new HashSet<>();
        fringe.add(new FringeEl(expr, null));

        Set<FringeEl> visited = new HashSet<>(); // for avoiding loops
        visited.addAll(fringe);

        while (! fringe.isEmpty() ) {
            FringeEl el = shortest(fringe);
            if( el.expr.sub==null ){
                break; // single term cannot be simplified
            }
            if( checkIfAnswer.test(el.expr) ){ // answer reached, no more work required
                break;
            }
//            if( el.expr.toLispString().length()<q.assertion.toLispString().length() ){
//                break;
//            }
            fringe.remove(el);
            System.out.println("QUEST try: " + el.expr.toMathString());
            while(tryByPairs(el, plusMinus));
            while(tryByPairs(el, multDiv));
            List<FringeEl> exprNew = exprSimplifyDeep(el.expr);
            for( FringeEl feNew : exprNew ){
                Expr e = feNew.expr;
                e = plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(e)));
                FringeEl fe = new FringeEl(e, null);
                if( ! visited.contains(fe) ){
                    visited.add(fe);
                    fringe.add(fe);
                }
            }
        }
        Expr res = shortest(visited).expr;
        System.out.println("QUEST res: "+res.toMathString());
        return res;
    }

    private boolean tryByPairs(FringeEl el, AssocCommutCancelRule assocCommutCancelRule) {
        List<Expr> splitPairs = assocCommutCancelRule.separateAllPossiblePairs(el.expr);
        //System.out.println("split pairs size="+splitPairs.size());
        for( Expr esplitPair : splitPairs ){

            Expr pair = esplitPair.sub.get(0);
            Expr e1 = assocCommutCancelRule.optimizeDeep(pair);
            pair = assocCommutCancelRule.optimizeDeep(multDiv.optimizeDeep(e1));
            List<FringeEl> exprNew = exprSimplifyDeep(pair);
            //System.out.println("   split pair: simplNew.size="+exprNew.size()+" "+esplitPair.toMathString());
            for( FringeEl fe : exprNew ){
                if( fe.expr.toLispString().length()<pair.toLispString().length() ){
                    //System.out.println(""+e+" "+pair);
                    el.expr = new Expr(assocCommutCancelRule.rolePlus, fe.expr , esplitPair.sub.get(1));
                    el.expr = assocCommutCancelRule.optimizeDeep(multDiv.optimizeDeep(assocCommutCancelRule.optimizeDeep(el.expr)));
                    return true;
                }
            }
        }
        return false;
    }

    FringeEl shortest(Collection<FringeEl> fringe){
        FringeEl sh=null;
        for( FringeEl e : fringe ){
            if( sh==null || sh.expr.toLispString().length()>e.expr.toLispString().length() ){
                sh = e;
            }
        }
        return sh;
    }

//    Expr shortest(List<Expr> exprNew){
//        Expr sh=null;
//        for( Expr e : exprNew ){
//            if( sh==null || sh.toLispString().length()>e.toLispString().length() ){
//                sh = e;
//            }
//        }
//        return sh;
//    }

    List<FringeEl> exprSimplifyDeep(Expr expr) {
        List<FringeEl> ways = exprSimplify(expr);
        if( expr.sub!=null ) {
            for (int i = 0; i < expr.sub.size(); i++) {
                List<FringeEl> elist = exprSimplifyDeep(expr.sub.get(i));
                for( FringeEl fe : elist ){
                    Expr clone = expr.shallowClone();
                    clone.sub.set(i, fe.expr);
                    fe.expr = clone;
                    ways.add(fe);
                }
            }
        }
        return ways;
    }

    List<FringeEl> exprSimplify(Expr expr) {
        List<FringeEl> ways = new ArrayList<>();
        for (Rule r : rules) {
            if (r.assertion.node.equals("=")) {
                Expr template = r.assertion.sub.get(0);
                Map<String, Expr> unifMap = template.unify(expr);
                if( unifMap!=null ) {
                    boolean canUseRule = true;
                    for( Expr cond : r.cond ){
                        Expr condSubs = cond.substitute(unifMap);
                        Map<String, Expr> unifMapCond = checkIfTrue(condSubs);
                        if( unifMapCond==null ){
                            canUseRule = false;
                            break;
                        }else{
                            unifMap.putAll(unifMapCond);
                        }
                    }
                    if( canUseRule ) {
                        //System.out.println("unify with " + r + " results in " + unifMap);
                        Expr exprNew = r.assertion.sub.get(1).substitute(unifMap);
                        //System.out.println(expr + " ==simplified==> " + exprNew);
                        ways.add(new FringeEl(exprNew, r));
                    }
                }
            }
        }
        return ways;
    }

    Map<String, Expr> checkIfTrue(Expr expr){
        for (Rule r : rules) {
            Map<String, Expr> unifMap = expr.unify(r.assertion);
            if( unifMap!=null ){
                return unifMap;
            }
        }
        return null;
    }
}
