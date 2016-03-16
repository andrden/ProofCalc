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

    class ExprTreeEl{
        final Expr expr;
        final boolean canChooseParameters;
        List<ChangeTreeEl> changes;

        long ops=0;
        List<Map<String,Expr>> suggestedParameters;

        int opIdx=0;
        List<ChangeTreeEl> changesNotFinished;
        Expr res;
        ChangeTreeEl resDueTo;

        ExprTreeEl(Expr expr, boolean canChooseParameters) {
            this.expr = expr.simplifyFuncOrApply();
            this.canChooseParameters = canChooseParameters;
        }

        @Override
        public String toString() {
            return ""+expr;
        }

        boolean finished(){
            return changesNotFinished !=null && changesNotFinished.isEmpty();
        }

        void doOper(Results results){
            ops++;
            if( finished() ){
                return;
            }
            populateChildren();
            if( finished() ){
                return;
            }
            ChangeTreeEl ch = changesNotFinished.get(opIdx);

            ch.doOper(results, expr);
            if( ch.stalled ){
                changesNotFinished.remove(opIdx); // dead end
            }else if( ch.exprFromRule!=null ) {
                if (expr.equals(ch.exprFromRule)) {
                    res = Expr.True;
                    resDueTo = ch;
                } else if (ch.exprFromRule.node.equals("=") && expr.equals(ch.exprFromRule.child(0))) {
                    res = ch.exprFromRule.child(1);
                    resDueTo = ch;
                } else if( canChooseParameters ){
                    suggestedParameters = expr.unifyOptions(ch.exprFromRule);
                    if( suggestedParameters.isEmpty() ){
                        suggestedParameters = null;
                    }
                }
                if( ch.finished() ) {
                    changesNotFinished.remove(opIdx);
                }
            }
            if( ! finished() ){
                opIdx = (opIdx + 1) % changesNotFinished.size();
            }
        }

        void populateChildren(){
            if( changes!=null ){
                return;
            }
            changes = exprSimplifyDeep(expr, new Scope());
            if( changes.isEmpty() && canChooseParameters ) {
                if( expr.node.equals("=") ){
                    Expr l = expr.child(0).simplifyApplyFunc();
                    Expr r = expr.child(1);
                    List<Map<String, Expr>> opts = r.unifyOptions(l);
                    if( ! opts.isEmpty() ) {
                        suggestedParameters = opts;
                    }
                }
                for (Rule r : rules) {
                    if( r.cond.isEmpty() ) {
                        List<Map<String, Expr>> opts = expr.unifyOptions(r.assertion);
                        if (!opts.isEmpty()) {
                            suggestedParameters = opts;
                            break;
                        }
                    }
                }
            }
            changesNotFinished = new ArrayList<>(changes);
        }
    }


    class ChangeTreeEl{
        ExprTreeEl next;
        long ops;
        LinkedHashMap<Expr,Expr> initialSubstitutions;
        Rule r;
        Map<String, Expr> unifMap;
        boolean condsOk = false;
        ExprTreeEl condCheck;
        Expr exprFromRule;
        List<Map<String,Expr>> suggestedParameters;
        boolean stalled = false;

        boolean finished(){
            return stalled || (next!=null && next.finished());
        }
        void addInitialSubstitution(Expr from, Expr to){
            if( initialSubstitutions == null ){
                initialSubstitutions = new LinkedHashMap<>();
            }
            initialSubstitutions.put(from, to);
        }

        @Override
        public String toString() {
            return (stalled ? "stalled  " : "") + r;
        }

        ChangeTreeEl(Rule r, Map<String, Expr> unifMap) {
            this.r = r;
            this.unifMap = unifMap;
            if( ! r.cond.isEmpty() ) {
                Expr cond = r.cond.iterator().next();
                Expr condSubs;
                condSubs = cond.substitute(unifMap);
                condCheck = new ExprTreeEl(condSubs, true);
            }else{
                condsOk = true;
            }
        }
        void doOper(Results results, Expr origExpr) {
            ops++;
            if( next!=null ){
                next.doOper(results);
                return;
            }
            if( ! condsOk && ! stalled ) {
                condCheck.doOper(null);
                if( condCheck.res==Expr.True || condCheck.suggestedParameters!=null ){
                    suggestedParameters = condCheck.suggestedParameters;
                    condsOk = true;
                }else if( condCheck.finished() ){
                    stalled = true;
                    return;
                }
            }

            if( condsOk ){
                Map<String, Expr> subs = new HashMap<>(unifMap);
                if( suggestedParameters!=null ){
                    subs.putAll(suggestedParameters.get(0));
                }
                if (r.freeVariables.containsAll(subs.keySet())) {
                    //exprNew = r.assertion.child(1).substitute(unifMap);
                    exprFromRule = r.assertion.substitute(subs);
                    Expr from, to;
                    if( exprFromRule.node.equals("=") ) {
                        from = exprFromRule.child(0);
                        to = exprFromRule.child(1);
                    } else {
                        from = exprFromRule;
                        to = Expr.True;
                    }
                    Expr orig = origExpr.simplifyFuncOrApply();
                    if( initialSubstitutions!=null ){
                        for( Map.Entry<Expr,Expr> me : initialSubstitutions.entrySet() ){
                            orig = applySubstitution(orig, me.getKey(), me.getValue());
                        }
                    }
                    Expr subst = applySubstitution(orig, from, to);
                    subst = subst.simplifyApplyFunc();
                    subst = Normalizer.normalize(subst);
                    if( results!=null ) {
                        if (results.add(subst)) {
                            next = new ExprTreeEl(subst, false);
                        } else {
                            stalled = true; // this is a duplicate branch
                        }
                    }

                    //System.out.println(expr + " ==simplified==> " + exprNew);
                    //                if( ! exprNew.toLispString().contains(expr.toLispString()) ) { // if we are not actually complicating
                    //                    FringeEl fe = new FringeEl(exprNew, r, m);
                    //                    ways.add(fe);
                    //                }
                }
            }
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
        ExprTreeEl exprTreeEl = new ExprTreeEl(expr, false);
        Results results = new Results();
        results.add(expr);
        int step=0;
        for(; step<1000 && !exprTreeEl.finished(); step++){
            exprTreeEl.doOper(results);
            for( Expr e : results.takeNextGroup() ) {
                e = Normalizer.normalize(e);
                if (checkIfAnswer != null && checkIfAnswer.test(e)) { // answer reached, no more work required
                    resultPath = new FringeEl(e, null, null);
                    break;
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

//        while (++step<maxOps && ! fringe.isEmpty() ) {
//            FringeEl el = shortest(fringe);
//            if( ! el.expr.hasChildren() ){
//                resultPath = el;
//                break; // single term cannot be simplified
//            }
//            if( el.toString().contains("(+ (* (^ x 2) (apply cos x)) (* (apply sin x) 2 x))") ){
//                breakpoint();
//            }
//            if( checkIfAnswer!=null && checkIfAnswer.test(el.expr) ){ // answer reached, no more work required
//                resultPath = el;
//                break;
//            }
////            if( el.expr.toLispString().length()<q.assertion.toLispString().length() ){
////                break;
////            }
//            fringe.remove(el);
//            String exprString = el.expr.toMathString();
//            println(indent, "QUEST try #" + step + ": " + exprString);
//            if( exprString.contains("(lim0 (func y ((cos ((y * (/ 2)) + x)) * (sin (y * (/ 2))) * 2 * (/ y))))") ){
//                breakpoint();
//            }
//            //el = tryByPairs(el);
//            el = el.newExpr(Normalizer.normalize(el.expr));
//            if( ! visited.contains(el) ){
//                visited.add(el);
//                fringe.add(el);
//            }
//
////            List<FringeEl> exprNew = exprSimplifyDeep(el.expr, new Scope());
////            for( FringeEl feNew : exprNew ){
////                Expr e = feNew.expr;
////                e = Normalizer.normalize(e);
////                e = e.simplifyApplyFunc();
////                e = Normalizer.normalize(e);
////                feNew = feNew.newExpr(e);
////                feNew.parent = el;
////                if( ! visited.contains(feNew) ){
////                    visited.add(feNew);
////                    fringe.add(feNew);
////                }
////            }
//        }
//        if( resultPath==null ){
//            resultPath = shortest(visited);
//            if( checkIfAnswer!=null ){
//                List<FringeEl> topShortest = topShortest(visited, 15);
//                Collections.reverse(topShortest);
//                for( FringeEl el : topShortest ){
//                    println(indent, "Candidate: "+el.expr.toMathString());
//                }
//            }
//        }
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

    Expr applySubstitution(Expr expr, Expr from, Expr to){
        if( expr.equals(from) ){
            return to;
        }
        if( expr.hasChildren() ){
            for (int i = 0; i < expr.subCount(); i++) {
                Expr child = expr.child(i);
                Expr subst = applySubstitution(child, from, to);
                if( subst!=null ){
                    Expr clone = expr.replaceChild(i, subst);
                    return clone;
                }
            }
        }
        return null;
    }

    List<ChangeTreeEl> exprSimplifyDeep(Expr expr, Scope scope) {
        List<ChangeTreeEl> ways = exprSimplify(expr, scope);
        for( Expr splitPair : Normalizer.plusMinus.separateAllPossiblePairs(expr) ){
            for( ChangeTreeEl i : exprSimplifyDeep(splitPair, scope) ) {
                i.addInitialSubstitution(expr, splitPair);
                ways.add(i);
            }
        }
        for( Expr splitPair : Normalizer.multDiv.separateAllPossiblePairs(expr) ){
            for( ChangeTreeEl i : exprSimplifyDeep(splitPair, scope) ) {
                i.addInitialSubstitution(expr, splitPair);
                ways.add(i);
            }
        }
        if( expr.hasChildren() ) {
            int start = 0;
            Scope subScope = scope;
            if( expr.isQuantified() ){
                start = 2;
                subScope = scope.push(new Expr("∈", expr.child(0), expr.child(1)));
            }
            for (int i = start; i < expr.subCount(); i++) {
                Expr child = expr.child(i);
                List<ChangeTreeEl> elist = exprSimplifyDeep(child, subScope);
                ways.addAll(elist);
//                for( FringeEl fe : elist ){
//                    Expr clone = expr.replaceChild(i, fe.expr);
//                    ways.add(fe.newExpr(clone));
//                }
            }
        }
        return ways;
    }

    List<ChangeTreeEl> exprSimplify(Expr expr, Scope scope) {
        List<ChangeTreeEl> changes = new ArrayList<>();
        List<FringeEl> ways = new ArrayList<>();
        ways.addAll(new CodedRules(expr).getWays());
        for (Rule r : rules) {
            if (r.assertion.node.equals("=")) {
                Expr template = r.assertion.child(0);
                List<Map<String,Expr>> cases;
                if( r.freeVariables.isEmpty() && ! template.equals(expr) ){
                    cases = Collections.emptyList(); // no way it can be unified
                }else {
                    cases = template.unifyOptions(expr);
                }
                for( Map<String, Expr> unifMap : cases ){
                    changes.add(new ChangeTreeEl(r, unifMap));

                    /*
                    List<Map<String, Expr>> options = checkCanUseRule(r, unifMap, scope);
                    for( Map<String, Expr> m : options ){
                        if( r.freeVariables.containsAll(m.keySet()) ){
                            Expr exprNew = r.assertion.child(1).substitute(m);
                            //System.out.println(expr + " ==simplified==> " + exprNew);
                            if( ! exprNew.toLispString().contains(expr.toLispString()) ) { // if we are not actually complicating
                                FringeEl fe = new FringeEl(exprNew, r, m);
                                ways.add(fe);
                            }
                        }
                    }
                    */
                }
            }else{
                Map<String, Expr> unifMap = r.assertion.unify(expr);
                if( unifMap!=null ) {
                    changes.add(new ChangeTreeEl(r, unifMap));
//                    List<Map<String, Expr>> subDerivations = checkCanUseRule(r, unifMap, scope);
//                    boolean canUseRule = ! subDerivations.isEmpty();
//                    if( canUseRule ) {
//                        //System.out.println("ok");
//                        ways.add(new FringeEl(new Expr("True"), r, unifMap));
//                    }
                }
            }
        }
        return changes;
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
