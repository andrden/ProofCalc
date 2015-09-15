package c;

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

    static class FringeEl{
        Expr expr;

        FringeEl(Expr expr) {
            this.expr = expr;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FringeEl)) return false;

            FringeEl fringeEl = (FringeEl) o;

            if (expr != null ? !expr.equals(fringeEl.expr) : fringeEl.expr != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return expr != null ? expr.toLispString().hashCode() : 0;
        }
    }

    Expr quest(Rule q, Predicate<Expr> checkIfAnswer){
        System.out.println("\nQUEST:\n"+q+"\n");


        Expr expr = q.assertion;
        expr = plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(expr)));
//        if( ! expr.equals(q.assertion) ){
//            System.out.println("QUEST try: "+expr.toMathString());
//        }

        Set<FringeEl> fringe = new HashSet<>();
        fringe.add(new FringeEl(expr));

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
            tryByPairs(el);
            List<Expr> exprNew = exprSimplifyDeep(el.expr);
            for( Expr e : exprNew ){
                e = plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(e)));
                FringeEl fe = new FringeEl(e);
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

    private void tryByPairs(FringeEl el) {
        List<Expr> splitPairs = plusMinus.separateAllPossiblePairs(el.expr);
        //System.out.println("split pairs size="+splitPairs.size());
        for( Expr esplitPair : splitPairs ){

            Expr pair = esplitPair.sub.get(0);
            //System.out.println("pair="+pair.toMathString());
//            if( pair.toMathString().contains("((sh ψ) ^ 2) * (x ^ 2)) + (- (((ch ψ) ^ 2) * (x ^ 2)") ){
//                System.out.println();
//            }
            Expr e1 = plusMinus.optimizeDeep(pair);
            pair = plusMinus.optimizeDeep(multDiv.optimizeDeep(e1));
            List<Expr> exprNew = exprSimplifyDeep(pair);
            //System.out.println("   split pair: simplNew.size="+exprNew.size()+" "+esplitPair.toMathString());
            for( Expr e : exprNew ){
                if( e.toLispString().length()<pair.toLispString().length() ){
                    //System.out.println(""+e+" "+pair);
                    el.expr = new Expr("+",e,esplitPair.sub.get(1));
                    el.expr = plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(el.expr)));
                    return;
                }
            }


//                FringeEl fe = new FringeEl(esplitPair);
//                if( ! visited.contains(fe) ){
//                    visited.add(fe);
//                    fringe.add(fe);
//                }
        }
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
                        ways.add(exprNew);
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
