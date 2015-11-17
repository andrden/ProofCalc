package c.calc;

import c.model.Expr;
import c.model.Rule;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by denny on 8/11/15.
 */
public class Calc {
    List<Rule> rules;

    AssocCommutCancelRule plusMinus = new AssocCommutCancelRule("+","-","0",false);
    AssocCommutCancelRule multDiv = new AssocCommutCancelRule("*","/","1",true);

    Map<Expr, Expr> subResults = new HashMap<>();

    public Calc(List<Rule> rules, List<Rule> localRules) {
        this.rules = new ArrayList<>();
        this.rules.addAll(localRules);
        this.rules.addAll(rules);

        for( Rule r : rules ){
            List<Expr> condsNorm = new ArrayList<>();
            for( Expr ce : r.cond ){
                Expr ceNorm = normalize(ce);
                condsNorm.add(ceNorm);
            }
            if( ! r.cond.equals(condsNorm) ){
                this.rules.add(new Rule(r.assertion, condsNorm, r.getSrcLines()));
            }
        }

        // need to try to optimize quest-local rules, i.e. preconditions for quest
        for( Rule r : localRules ){
            if( r.assertion.node.equals("=") ){
                Expr expr = r.assertion.rightChild();
                //Rule qrule = new Rule(expr, Collections.emptyList(), null);
                Expr simpl = quest(expr, null, 25);
                if( ! expr.equals(simpl) ) {
                    System.out.println("simpl=" + simpl);
                    this.rules.add(new Rule(r.assertion.rightChildReplace(simpl), Collections.emptyList(), null));
                }
            }
        }
    }

    public Expr quest(Expr expr, Predicate<Expr> checkIfAnswer, int maxOps){
        if( subResults.containsKey(expr) ){
            return subResults.get(expr);
        }
        final Expr origExpr = expr;
        String indent = (checkIfAnswer==null ? null/*"    "*/:"");
        println(indent, "\n");
        println(indent, "================QUEST:\n" + expr + "\n");


        //Expr expr = q.assertion;
        expr = normalize(expr);
//        if( ! expr.equals(q.assertion) ){
//            System.out.println("QUEST try: "+expr.toMathString());
//        }

        Set<FringeEl> fringe = new HashSet<>();
        fringe.add(new FringeEl(expr, null, null));

        Set<FringeEl> visited = new LinkedHashSet<>(); // for avoiding loops
        visited.addAll(fringe);

        FringeEl resultPath = null;
        int step=0;
        while (++step<maxOps && ! fringe.isEmpty() ) {
            FringeEl el = shortest(fringe);
            if( el.expr.sub==null ){
                resultPath = el;
                break; // single term cannot be simplified
            }
            if( el.toString().contains("(+ (* (^ x 2) (apply cos x)) (* (apply sin x) 2 x))") ){
                breakpoint();
            }
            if( checkIfAnswer!=null && checkIfAnswer.test(el.expr) ){ // answer reached, no more work required
                resultPath = el;
                break;
            }
//            if( el.expr.toLispString().length()<q.assertion.toLispString().length() ){
//                break;
//            }
            fringe.remove(el);
            String exprString = el.expr.toMathString();
            println(indent, "QUEST try #" + step + ": " + exprString);
            if( exprString.contains("(((∂ (func x (x ^ 2))) x) + ((∂ (func x (x ^ 3))) x) + ((∂ (func x 1)) x))") ){
                breakpoint();
            }
            el = tryByPairs(el);
            if( ! visited.contains(el) ){
                visited.add(el);
                fringe.add(el);
            }

            List<FringeEl> exprNew = exprSimplifyDeep(el.expr);
            for( FringeEl feNew : exprNew ){
                Expr e = feNew.expr;
                e = normalize(e);
                e = e.simplifyApplyFunc();
                e = normalize(e);
                feNew = feNew.newExpr(e);
                feNew.parent = el;
                if( ! visited.contains(feNew) ){
                    visited.add(feNew);
                    fringe.add(feNew);
                }
            }
        }
        if( resultPath==null ){
            resultPath = shortest(visited);
            if( checkIfAnswer!=null ){
                List<FringeEl> topShortest = topShortest(visited, 15);
                Collections.reverse(topShortest);
                for( FringeEl el : topShortest ){
                    println(indent, "Candidate: "+el.expr.toMathString());
                }
            }
        }
        Expr res = resultPath.expr;
        cacheResult(origExpr, res);
        if( checkIfAnswer!=null ){
            resultPath.printDerivationPath(indent);
            println(indent, "QUEST res: "+res.toMathString());

            List list = visited.stream().filter(x -> ! x.toString().contains("∂")).collect(Collectors.toList());
            breakpoint();
        }
        if( origExpr.toString().equals("(apply (apply ∂ ff) x)") ){
            breakpoint();
        }
        return res;
    }

    void println(String indent, String msg){
        if( indent==null ){
            return;
        }
        System.out.println(indent + msg);
    }

    public Expr normalize(Expr expr) {
        return multDiv.optimizeDeep(plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(expr))));
    }

    FringeEl tryByPairs(FringeEl el){
        while(true){
            Expr enew = tryByPairsDeep(el.expr, plusMinus);
            if( enew!=null ){
                el = new FringeEl(enew, new NamedRule("tryByPairs+"), null, el);
            }else{
                break;
            }
        }
        while(true){
            Expr enew = tryByPairsDeep(el.expr, multDiv);
            if( enew!=null ){
                el = new FringeEl(enew, new NamedRule("tryByPairs*"), null, el);
            }else{
                break;
            }
        }
        return el;
    }

    void cacheResult(Expr origExpr, Expr res){
        if( subResults.containsKey(origExpr) ){
            throw new IllegalStateException();
        }else{
            subResults.put(origExpr, res);
        }
    }

    Expr tryByPairsDeep(Expr expr, AssocCommutCancelRule assocCommutCancelRule){
        Expr enew = tryByPairs(expr, assocCommutCancelRule);
        if( enew!=null ){
            return enew;
        }
        if( expr.sub!=null ){
            for(int i=0; i<expr.sub.size(); i++ ){
                Expr s = expr.sub.get(i);
                Expr sTry = tryByPairsDeep(s, assocCommutCancelRule);
                if( sTry != null ){
                    return expr.replaceChild(i, sTry);
                }
            }
        }
        return null;
    }

    private Expr tryByPairs(Expr expr, AssocCommutCancelRule assocCommutCancelRule) {
        List<Expr> splitPairs = assocCommutCancelRule.separateAllPossiblePairs(expr);
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
                    expr = new Expr(assocCommutCancelRule.rolePlus, fe.expr , esplitPair.sub.get(1));
                    expr = assocCommutCancelRule.optimizeDeep(multDiv.optimizeDeep(assocCommutCancelRule.optimizeDeep(expr)));
                    return expr;
                }
            }
        }
        return null;
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

    List<FringeEl> topShortest(Collection<FringeEl> fringe, int n){
        return fringe.stream().sorted((a,b) -> Long.compare(a.expr.toLispString().length(),b.expr.toLispString().length()) )
                .limit(n)
                .collect(Collectors.toList());
    }

    List<FringeEl> exprSimplifyDeep(Expr expr) {
        List<FringeEl> ways = exprSimplify(expr);
        if( expr.sub!=null ) {
            for (int i = 0; i < expr.sub.size(); i++) {
                List<FringeEl> elist = exprSimplifyDeep(expr.sub.get(i));
                for( FringeEl fe : elist ){
                    Expr clone = expr.shallowClone();
                    clone.sub.set(i, fe.expr);
                    ways.add(fe.newExpr(clone));
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
                    if( expr.toString().equals("(apply (apply ∂ (func x (+ (^ x 2) 7))) x)") ){
                        breakpoint();
                    }
                    List<FringeEl> subDerivations = checkCanUseRule(r, unifMap);
                    boolean canUseRule = subDerivations!=null;
                    if( canUseRule && r.toLineString().contains("x ↦ g( h(x) )") ) {
                        breakpoint();
                    }
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
                            FringeEl fe = new FringeEl(exprNew, r, unifMap);
                            ways.add(fe);
                        }
                    }
                }
            }else{
                Map<String, Expr> unifMap = r.assertion.unify(expr);
                if( unifMap!=null ) {
                    List<FringeEl> subDerivations = checkCanUseRule(r, unifMap);
                    boolean canUseRule = subDerivations != null;
                    if( canUseRule ) {
                        //System.out.println("ok");
                        ways.add(new FringeEl(new Expr("True"), r, unifMap));
                    }
                }
            }
        }
        return ways;
    }

    private List<FringeEl> checkCanUseRule(Rule r, Map<String, Expr> unifMap) {
        List<FringeEl> subDerivations = new ArrayList<>();
        for( Expr cond : r.cond ){
            Expr condSubs = cond.substitute(unifMap);
            FringeEl checkIfTrueResult = checkIfTrue(condSubs);
            if( checkIfTrueResult==null ){
                return null;
            }else{
                Map<String, Expr> unifMapCond = checkIfTrueResult.unifMap;
                unifMap.putAll(unifMapCond);
                subDerivations.add(checkIfTrueResult);
            }
        }
        return subDerivations;
    }

    Map<String,Expr> unifyEquality(Expr expr){
        if( expr.node.equals("=") ) {
            // just try to unify both parts of the tested equality first
            Expr concrete = expr.sub.get(0);
            Expr tpl = expr.sub.get(1);
            Map<String, Expr> map = tpl.unify(concrete);
            if (map != null) {
                Expr resultTpl = tpl.substitute(map).simplifyApplyFunc();
                Expr resultConcrete = concrete.substitute(map).simplifyApplyFunc();
                if (resultTpl.equals(resultConcrete)) {
                    // Avoid erroneous unification of 'x' with 'x+1'
                    // for 'x = x + 1' equality.
                    // Unification itself is correct, but not suitable for equality
                    return map;
                }else{
                    breakpoint();
                }
            }
        }
        return null;
    }

    private void breakpoint() {
        System.out.println("breakpoint");
    }

    FringeEl checkIfTrue(Expr expr){
        if( expr.toLispString().contains("(= (apply ff x) (+ (apply g x) (apply h x)))") ){
            breakpoint();
        }

        if( expr.node.equals("=") ){
            // just try to unify both parts of the tested equality first
            Map<String, Expr> unifMapEquation = unifyEquality(expr);
            if( unifMapEquation!=null ){
                    return new FringeEl(null, null, unifMapEquation);
            }
        }

        Expr res = quest(expr, null, 20);
        if( res.node.equals("True") ){
            return new FringeEl(null, null/*r*/, Collections.emptyMap() /*unifMap*/);
        }

        for (Rule r : rules) {
            Expr concrete = r.assertion;


/*
 Checking if 'const ff' is true (when ff(x)=5, such local rule is present).
 We have to unify rule "f(x)=5 => const f",
 that is unify its template assertion "const f" with our tested expression "const ff",
 seeing necessary mapping "f -> ff"
  */
//            Map<String, Expr> unifMap = template.unify(expr);
//            if( unifMap!=null ){
//                // we don't need to use this map for replacement in our checked 'expr'
//                return new FringeEl(null, r, Collections.emptyMap() /*unifMap*/);
//            }

/*
Another situation, more like an equation.
We want to see whether 'x+1=5' for any value of a parameter.
We can find that for x=4 this is true (according to rule '4+1=5', so we unify the other way,
and have to use unifMapEquation to substitute x in our original 'expr'
 */
            Map<String, Expr> unifMapEquation = expr.unify(concrete);
            if( unifMapEquation!=null ){
                return new FringeEl(null, r, unifMapEquation);
            }
        }
        return null;
    }

    private static class NamedRule extends Rule {
        String name;
        public NamedRule(String name) {
            super(null, null, null);
            this.name = name;
        }

        @Override
        public String toLineString() {
            return "["+name+"]";
        }

        @Override
        public String toString() {
            return toLineString();
        }
    }
}
