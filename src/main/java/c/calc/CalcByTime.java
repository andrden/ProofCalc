package c.calc;

import c.model.Expr;
import c.model.Normalizer;
import c.model.Rule;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by denny on 8/11/15.
 */
public class CalcByTime {
    List<Rule> rules;


    Map<Expr, Expr> subResults = new HashMap<>();

    public CalcByTime(List<Rule> rules, List<Rule> localRules) {
        this.rules = new ArrayList<>();
        this.rules.addAll(localRules);

        for( Rule r : rules ){
//            if( ! r.assertion.equals(Normalizer.normalize(r.assertion)) ){
//              r = new Rule(Normalizer.normalize(r.assertion), r.cond, r.getSrcLines());
//            }
            //this.rules.add(r);
            List<Expr> condsNorm = new ArrayList<>();
            for( Expr ce : r.cond ){
                Expr ceNorm = Normalizer.normalize(ce);
                condsNorm.add(ceNorm);
            }
            //if( ! r.cond.equals(condsNorm) ){
                this.rules.add(new Rule(r.assertion, condsNorm, r.getSrcLines()));
            //}
        }

        // need to try to optimize quest-local rules, i.e. preconditions for quest
//        for( Rule r : localRules ){
//            if( r.assertion.node.equals("=") ){
//                Expr expr = r.assertion.rightChild();
//                //Rule qrule = new Rule(expr, Collections.emptyList(), null);
//                Expr simpl = quest(expr, null, 25);
//                if( ! expr.equals(simpl) ) {
//                    System.out.println("simpl=" + simpl);
//                    this.rules.add(new Rule(r.assertion.rightChildReplace(simpl), Collections.emptyList(), null));
//                }
//            }
//        }
    }

    public List<Rule> getRules() {
        return rules;
    }

    class Results{
        Set<Expr> set = new HashSet<>();
        List<Expr> nextGroup = new ArrayList<>();
        boolean add(Expr expr){
            if( set.add(expr) ){
                nextGroup.add(expr);
                return true;
            }else{
                return false;
            }
        }
        List<Expr> takeNextGroup(){
            if( ! nextGroup.isEmpty() ){
                List<Expr> ret = nextGroup;
                nextGroup = new ArrayList<>();
                return ret;
            }
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "count="+set.size();
        }
    }


    public Expr quest(Expr expr, Predicate<Expr> checkIfAnswer, int maxOps){
        if( subResults.containsKey(expr) ){
            return subResults.get(expr);
        }
        final Expr origExpr = expr;
        cacheResult(origExpr, origExpr); // protection against self-recursive calls and StackOverflow
        String indent = (checkIfAnswer==null ? null/*"    "*/:"");
        println(indent, "\n");
        println(indent, "================QUEST:\n" + expr + "\n");


        FringeEl resultPath = null;

        expr = Normalizer.normalize(expr);
        ExprTreeEl exprTreeEl = new ExprTreeEl(this, expr, false);
        Results results = new Results();
        results.add(expr);
        int step=0;
        label_steps:
        for(; step<10000 && !exprTreeEl.finished(); step++){
            exprTreeEl.doOper(results);
            for( Expr e : results.takeNextGroup() ) {
                e = Normalizer.normalize(e);
                if (e.equals(Expr.True) || e.equals(Expr.False) ||
                      (checkIfAnswer != null && checkIfAnswer.test(e)) ) { // answer reached, no more work required
                    resultPath = new FringeEl(e, null, null);
                    break label_steps;
                }
            }
        }
        System.out.println("STEP="+step);
        if( resultPath==null && exprTreeEl.res!=null ){
            resultPath = new FringeEl(exprTreeEl.res, null, null);
        }

        Set<FringeEl> fringe = new LinkedHashSet<>();
        fringe.add(new FringeEl(expr, null, null));

        Set<FringeEl> visited = new LinkedHashSet<>(); // for avoiding loops
        visited.addAll(fringe);

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

    void cacheResult(Expr origExpr, Expr res){
        if( subResults.containsKey(origExpr) && ! origExpr.equals(subResults.get(origExpr)) ){
            throw new IllegalStateException("different non-identical transformation");
        }else{
            subResults.put(origExpr, res);
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

    List<FringeEl> topShortest(Collection<FringeEl> fringe, int n){
        return fringe.stream().sorted((a,b) -> Long.compare(a.expr.toLispString().length(),b.expr.toLispString().length()) )
                .limit(n)
                .collect(Collectors.toList());
    }



    private List<Map<String, Expr>> checkCanUseRule(Rule r, Map<String, Expr> unifMap, Scope scope) {
        List<Map<String, Expr>> cases = Collections.singletonList(unifMap);
        for( Expr cond : r.cond ){
            List<Map<String, Expr>> newCases = new ArrayList<>();
            for( Map<String, Expr> m : cases ) {
                Expr condSubs = cond.substitute(m);
                if( scope.has(condSubs) ){
                    Map<String, Expr> mi = new HashMap<>(m);
                    newCases.add(mi);
                }else {
                    List<FringeEl> checkIfTrueResultList = checkIfTrueOrCanBeMadeTrue(condSubs);
                    if (checkIfTrueResultList != null) {
                        for (FringeEl checkIfTrueResult : checkIfTrueResultList) {
                            Map<String, Expr> mi = new HashMap<>(m);
                            mi.putAll(checkIfTrueResult.unifMap);
                            newCases.add(mi);
                        }
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
