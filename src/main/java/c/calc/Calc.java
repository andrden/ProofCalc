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

    AssocCommutCancelRule plusMinus = new AssocCommutCancelRule("+","-","0",false);
    AssocCommutCancelRule multDiv = new AssocCommutCancelRule("*","/","1",true);

    public Calc(List<Rule> rules, List<Rule> localRules) {
        this.rules = new ArrayList<>(rules);
        this.rules.addAll(localRules);

        // need to try to optimize quest-local rules, i.e. preconditions for quest
        for( Rule r : localRules ){
            if( r.assertion.node.equals("=") ){
                Expr expr = r.assertion.rightChild();
                Rule qrule = new Rule(expr, Collections.emptyList(), null);
                Expr simpl = quest(qrule, null, 15);
                if( ! expr.equals(simpl) ) {
                    System.out.println("simpl=" + simpl);
                    this.rules.add(new Rule(r.assertion.rightChildReplace(simpl), Collections.emptyList(), null));
                }
            }
        }
    }

    public Expr quest(Rule q, Predicate<Expr> checkIfAnswer, int maxOps){
        String indent = (checkIfAnswer==null ? "    ":"");
        System.out.println("\n"+indent+"================QUEST:\n"+q+"\n");


        Expr expr = q.assertion;
        expr = plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(expr)));
//        if( ! expr.equals(q.assertion) ){
//            System.out.println("QUEST try: "+expr.toMathString());
//        }

        Set<FringeEl> fringe = new HashSet<>();
        fringe.add(new FringeEl(expr, null, null));

        Set<FringeEl> visited = new HashSet<>(); // for avoiding loops
        visited.addAll(fringe);

        int step=0;
        while (++step<maxOps && ! fringe.isEmpty() ) {
            FringeEl el = shortest(fringe);
            if( el.expr.sub==null ){
                break; // single term cannot be simplified
            }
            if( step>5 && checkIfAnswer!=null && checkIfAnswer.test(el.expr) ){ // answer reached, no more work required
                break;
            }
//            if( el.expr.toLispString().length()<q.assertion.toLispString().length() ){
//                break;
//            }
            fringe.remove(el);
            String exprString = el.expr.toMathString();
            System.out.println(indent+"QUEST try #" + step + ": " + exprString);
            if( exprString.contains("(const ff)") ){
                System.out.println("breakpoint");
            }
            while(tryByPairs(el, plusMinus));
            while(tryByPairs(el, multDiv));
            List<FringeEl> exprNew = exprSimplifyDeep(el.expr);
            for( FringeEl feNew : exprNew ){
                Expr e = feNew.expr;
                e = plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(e)));
                feNew.expr = e;
                feNew.parent = el;
                if( ! visited.contains(feNew) ){
                    visited.add(feNew);
                    fringe.add(feNew);
                }
            }
        }
        FringeEl resultPath = shortest(visited);
        Expr res = resultPath.expr;
        resultPath.printDerivationPath();
        System.out.println(indent+"QUEST res: "+res.toMathString());
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
                    if( expr.toString().equals("(+ 5 (- 1))") ){
                        System.out.println("breakpoint");
                    }
                    boolean canUseRule = checkCanUseRule(r, unifMap);
//                    for( String v : expr.freeVariables() ){
//                        if( unifMap.containsKey(v) ){
//                            canUseRule = false; // we must simplify generically, can't fix vals of vars
//                        }
//                    }
                    if( canUseRule ) {
                        //System.out.println("unify with " + r + " results in " + unifMap);
                        if( ! r.freeVariables.containsAll(unifMap.keySet()) ){
                            //throw new IllegalStateException();
                        }else {
                            Expr exprNew = r.assertion.sub.get(1).substitute(unifMap);
                            //System.out.println(expr + " ==simplified==> " + exprNew);
                            ways.add(new FringeEl(exprNew, r, unifMap));
                        }
                    }
                }
            }else{
                Map<String, Expr> unifMap = r.assertion.unify(expr);
                if( unifMap!=null ) {
                    boolean canUseRule = checkCanUseRule(r, unifMap);
                    if( canUseRule ) {
                        //System.out.println("ok");
                        ways.add(new FringeEl(new Expr("True"), r, unifMap));
                    }
                }
            }
        }
        return ways;
    }

    private boolean checkCanUseRule(Rule r, Map<String, Expr> unifMap) {
        boolean canUseRule = true;
        for( Expr cond : r.cond ){
            Expr condSubs = cond.substitute(unifMap);
            FringeEl checkIfTrueResult = checkIfTrue(condSubs);
            if( checkIfTrueResult==null ){
                canUseRule = false;
                break;
            }else{
                Map<String, Expr> unifMapCond = checkIfTrueResult.unifMap;
                unifMap.putAll(unifMapCond);
            }
        }
        return canUseRule;
    }

    FringeEl checkIfTrue(Expr expr){
        for (Rule r : rules) {
            Expr template = r.assertion;
            //Map<String, Expr> unifMap = template.unify(expr);
            Map<String, Expr> unifMap = expr.unify(template);
            if( unifMap!=null ){
                return new FringeEl(null, r, unifMap);
            }
        }
        return null;
    }
}