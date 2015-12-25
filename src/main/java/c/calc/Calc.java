package c.calc;

import c.model.AssocCommutCancelRule;
import c.model.Expr;
import c.model.Normalizer;
import c.model.Rule;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by denny on 8/11/15.
 */
public class Calc {
    List<Rule> rules;


    Map<Expr, Expr> subResults = new HashMap<>();

    public Calc(List<Rule> rules, List<Rule> localRules) {
        this.rules = new ArrayList<>();
        this.rules.addAll(localRules);

        for( Rule r : rules ){
            if( ! r.assertion.equals(Normalizer.normalize(r.assertion)) ){
              r = new Rule(Normalizer.normalize(r.assertion), r.cond, r.getSrcLines());
            }
            this.rules.add(r);
            List<Expr> condsNorm = new ArrayList<>();
            for( Expr ce : r.cond ){
                Expr ceNorm = Normalizer.normalize(ce);
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
        expr = Normalizer.normalize(expr);
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
            if( ! el.expr.hasChildren() ){
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
            if( exprString.contains("(lim0 (func y ((cos ((y * (/ 2)) + x)) * (sin (y * (/ 2))) * 2 * (/ y))))") ){
                breakpoint();
            }
            //el = tryByPairs(el);
            el = el.newExpr(Normalizer.normalize(el.expr));
            if( ! visited.contains(el) ){
                visited.add(el);
                fringe.add(el);
            }

            List<FringeEl> exprNew = exprSimplifyDeep(el.expr);
            for( FringeEl feNew : exprNew ){
                Expr e = feNew.expr;
                e = Normalizer.normalize(e);
                e = e.simplifyApplyFunc();
                e = Normalizer.normalize(e);
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

            List list = visited.stream()
                    .filter(x -> ! x.toString().contains("lim0"))
                    .filter(x -> ! x.toString().contains("∂"))
                    .collect(Collectors.toList());
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

    FringeEl tryByPairs(FringeEl el){
        while(true){
            Expr enew = tryByPairsDeep(el.expr, Normalizer.plusMinus);
            if( enew!=null ){
                el = new FringeEl(enew, new NamedRule("tryByPairs+"), null, el);
            }else{
                break;
            }
        }
        while(true){
            Expr enew = tryByPairsDeep(el.expr, Normalizer.multDiv);
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
        if( expr.hasChildren() ){
            for(int i=0; i<expr.subCount(); i++ ){
                Expr s = expr.child(i);
                Expr sTry = tryByPairsDeep(s, assocCommutCancelRule);
                if( sTry != null ){
                    return expr.replaceChild(i, sTry);
                }
            }
        }
        return null;
    }

    private Expr tryByPairs(Expr expr, AssocCommutCancelRule assocCommutCancelRule) {
        Set<Expr> splitPairs = assocCommutCancelRule.separateAllPossiblePairs(expr);
        //System.out.println("split pairs size="+splitPairs.size());
        for( Expr esplitPair : splitPairs ){

            Expr pair = esplitPair.child(0);
            Expr e1 = assocCommutCancelRule.optimizeDeep(pair);
            pair = assocCommutCancelRule.optimizeDeep(Normalizer.multDiv.optimizeDeep(e1));
            List<FringeEl> exprNew = exprSimplifyDeep(pair);
            //System.out.println("   split pair: simplNew.size="+exprNew.size()+" "+esplitPair.toMathString());
            for( FringeEl fe : exprNew ){
                if( fe.expr.toLispString().length()<pair.toLispString().length() ){
                    //System.out.println(""+e+" "+pair);
                    expr = new Expr(assocCommutCancelRule.getRolePlus(), fe.expr , esplitPair.child(1));
                    expr = assocCommutCancelRule.optimizeDeep(Normalizer.multDiv.optimizeDeep(assocCommutCancelRule.optimizeDeep(expr)));
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
        for( Expr splitPair : Normalizer.plusMinus.separateAllPossiblePairs(expr) ){
            ways.addAll(exprSimplifyDeep(splitPair));
        }
        for( Expr splitPair : Normalizer.multDiv.separateAllPossiblePairs(expr) ){
            ways.addAll(exprSimplifyDeep(splitPair));
        }
        if( expr.hasChildren() ) {
            for (int i = 0; i < expr.subCount(); i++) {
                List<FringeEl> elist = exprSimplifyDeep(expr.child(i));
                for( FringeEl fe : elist ){
                    Expr clone = expr.replaceChild(i, fe.expr);
                    ways.add(fe.newExpr(clone));
                }
            }
        }
        return ways;
    }

    List<FringeEl> exprSimplify(Expr expr) {
        List<FringeEl> ways = new ArrayList<>();
        ways.addAll(new CodedRules(expr).getWays());
        for (Rule r : rules) {
            if (r.assertion.node.equals("=")) {
                Expr template = r.assertion.child(0);
                Map<String, Expr> unifMap = template.unify(expr);
                if( unifMap!=null ) {
                    if( expr.toString().equals("(apply (apply ∂ (func x (+ (^ x 2) 7))) x)") ){
                        breakpoint();
                    }
                    List<Map<String, Expr>> options = checkCanUseRule(r, unifMap);
                    for( Map<String, Expr> m : options ){
                        if( r.freeVariables.containsAll(m.keySet()) ){
                            Expr exprNew = r.assertion.child(1).substitute(m);
                            //System.out.println(expr + " ==simplified==> " + exprNew);
                            FringeEl fe = new FringeEl(exprNew, r, m);
                            ways.add(fe);
                        }
                    }
                }
            }else{
                Map<String, Expr> unifMap = r.assertion.unify(expr);
                if( unifMap!=null ) {
                    List<Map<String, Expr>> subDerivations = checkCanUseRule(r, unifMap);
                    boolean canUseRule = ! subDerivations.isEmpty();
                    if( canUseRule ) {
                        //System.out.println("ok");
                        ways.add(new FringeEl(new Expr("True"), r, unifMap));
                    }
                }
            }
        }
        return ways;
    }

    private List<Map<String, Expr>> checkCanUseRule(Rule r, Map<String, Expr> unifMap) {
        List<Map<String, Expr>> cases = Collections.singletonList(unifMap);
        for( Expr cond : r.cond ){
            List<Map<String, Expr>> newCases = new ArrayList<>();
            for( Map<String, Expr> m : cases ) {
                Expr condSubs = cond.substitute(m);
                List<FringeEl> checkIfTrueResultList = checkIfTrueOrCanBeMadeTrue(condSubs);
                if (checkIfTrueResultList != null) {
                    for( FringeEl checkIfTrueResult : checkIfTrueResultList ) {
                        Map<String, Expr> mi = new HashMap<>(m);
                        mi.putAll(checkIfTrueResult.unifMap);
                        newCases.add(mi);
                    }
                }
            }
            cases = newCases;
        }
        return cases;
    }

    List<Map<String,Expr>> unifyEquality(Expr expr){
        if( expr.node.equals("=") ) {
            List<Map<String,Expr>> ret = new ArrayList<>();
            // just try to unify both parts of the tested equality first
            Expr concrete = expr.child(0);
            Expr tpl = expr.child(1);
            List<Map<String,Expr>> cases = tpl.unifyOptions(concrete);
            for( Map<String, Expr> map : cases ){
                Expr resultTpl = tpl.substitute(map).simplifyFuncOrApply();
                Expr resultConcrete = concrete.substitute(map).simplifyFuncOrApply();
                if (resultTpl.equals(resultConcrete)) {
                    // Avoid erroneous unification of 'x' with 'x+1'
                    // for 'x = x + 1' equality.
                    // Unification itself is correct, but not suitable for equality
                    ret.add(map);
                }else{
                    breakpoint();
                }
            }
            return ret;
        }
        return null;
    }

    private void breakpoint() {
        System.out.println("breakpoint");
    }

    List<FringeEl> checkIfTrueOrCanBeMadeTrue(Expr expr){
        if( expr.toLispString().contains("(= (apply ff x) (+ (apply g x) (apply h x)))") ){
            breakpoint();
        }

        if( expr.node.equals("=") ){
            // just try to unify both parts of the tested equality first
            List<Map<String,Expr>> cases = unifyEquality(expr);
            if( cases!=null && ! cases.isEmpty() ) {
                List<FringeEl> ret = new ArrayList<>();
                for( Map<String, Expr> unifMapEquation : cases ){
                    ret.add(new FringeEl(null, null, unifMapEquation));
                }
                return ret;
            }
        }

        Expr res = quest(expr, null, 20);
        if( res.node.equals("True") ){
            return Collections.singletonList(new FringeEl(null, null/*r*/, Collections.emptyMap() /*unifMap*/));
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
                return Collections.singletonList(new FringeEl(null, r, unifMapEquation));
            }
        }
        return null;
    }

}
