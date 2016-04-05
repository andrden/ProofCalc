package c.calc;

import c.model.Expr;
import c.model.Normalizer;
import c.model.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
* Created by denny on 3/31/16.
*/
class ExprTreeEl {
    final CalcByTime calc;
    final Expr expr;
    final boolean canChooseParameters;
    List<ChangeTreeEl> changes;

    long ops=0;
    List<Map<String,Expr>> suggestedParameters;

    int opIdx=0;
    List<ChangeTreeEl> changesNotFinished;
    Expr res;
    ChangeTreeEl resDueTo;

    ExprTreeEl(CalcByTime calc, Expr expr, boolean canChooseParameters) {
        this.calc = calc;
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

    void doOper(CalcByTime.Results results){
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
        changes = exprSimplifyDeep(expr, new Path(), new Scope());
        if( changes.isEmpty() && canChooseParameters ) {
            if( expr.node.equals("=") ){
                Expr l = expr.child(0).simplifyApplyFunc();
                Expr r = expr.child(1);
                List<Map<String, Expr>> opts = r.unifyOptions(l);
                if( ! opts.isEmpty() ) {
                    suggestedParameters = opts;
                }
            }
            for (Rule r : calc.getRules()) {
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

    List<ChangeTreeEl> exprSimplifyDeep(Expr expr, Path path, Scope scope) {
        List<ChangeTreeEl> ways = exprSimplify(expr, path, scope);
        for( Expr splitPair : Normalizer.plusMinus.separateAllPossiblePairs(expr) ){
            for( ChangeTreeEl i : exprSimplifyDeep(splitPair, path, scope) ) {
                i.addInitialSubstitution(expr, splitPair);
                ways.add(i);
            }
        }
        for( Expr splitPair : Normalizer.multDiv.separateAllPossiblePairs(expr) ){
            for( ChangeTreeEl i : exprSimplifyDeep(splitPair, path, scope) ) {
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
                path.push(i);
                List<ChangeTreeEl> elist = exprSimplifyDeep(child, path, subScope);
                path.pop();
                ways.addAll(elist);
//                for( FringeEl fe : elist ){
//                    Expr clone = expr.replaceChild(i, fe.expr);
//                    ways.add(fe.newExpr(clone));
//                }
            }
        }
        return ways;
    }

    List<ChangeTreeEl> exprSimplify(Expr expr, Path path, Scope scope) {
        List<ChangeTreeEl> changes = new ArrayList<>();
        List<FringeEl> ways = new ArrayList<>();
        ways.addAll(new CodedRules(expr).getWays());
        for( FringeEl e : ways ){
            //changes.add(new ChangeTreeEl(calc, e.byRule, new Expr("=",expr,e.expr)));
            changes.add(new ChangeTreeEl(calc, e.byRule, new Expr("=",expr,e.expr), path));
        }
        for (Rule r : calc.getRules()) {
            if (r.assertion.node.equals("=")) {
                Expr template = r.assertion.child(0);
                List<Map<String,Expr>> cases;
                if( r.freeVariables.isEmpty() && ! template.equals(expr) ){
                    cases = Collections.emptyList(); // no way it can be unified
                }else {
                    cases = template.unifyOptions(expr);
                }
                for( Map<String, Expr> unifMap : cases ){
                    changes.add(new ChangeTreeEl(calc, r, unifMap, scope.all(), path.copy()));

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
                    changes.add(new ChangeTreeEl(calc, r, unifMap, scope.all(), path.copy()));
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

}
