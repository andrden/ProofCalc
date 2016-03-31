package c.calc;

import c.model.Expr;
import c.model.Normalizer;
import c.model.Rule;

import java.util.*;

/**
* Created by denny on 3/31/16.
*/
class ChangeTreeEl {
    final CalcByTime calc;
    ExprTreeEl next;
    long ops;
    LinkedHashMap<Expr,Expr> initialSubstitutions;
    Rule rule;
    Map<String, Expr> unifMap;
    Set<Expr> scope;
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
        return (stalled ? "stalled  " : "") + rule;
    }

    ChangeTreeEl(CalcByTime calc, Rule r, Expr exprFromRule){
        this.calc = calc;
        this.rule = r;
        this.exprFromRule = exprFromRule;
        condsOk = true;
    }

    ChangeTreeEl(CalcByTime calc, Rule r, Map<String, Expr> unifMap, Set<Expr> scope) {
        this.calc = calc;
        this.rule = r;
        this.unifMap = unifMap;
        this.scope = scope;
        if( ! r.cond.isEmpty() ) {
            Expr cond = r.cond.iterator().next();
            Expr condSubs;
            condSubs = cond.substitute(unifMap);
            if( scope.contains(condSubs) ){
                condsOk = true;
            }else {
                condCheck = new ExprTreeEl(calc, condSubs, true);
            }
        }else{
            condsOk = true;
        }
    }
    void doOper(CalcByTime.Results results, Expr origExpr) {
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
            if( exprFromRule==null ) { // if not direct result from CodedRules
                Map<String, Expr> subs = new HashMap<>(unifMap);
                if (suggestedParameters != null) {
                    subs.putAll(suggestedParameters.get(0));
                }
                //if (rule.freeVariables.containsAll(subs.keySet())) {
                if (subs.keySet().containsAll(rule.freeVariables)) {
                    //exprNew = r.assertion.child(1).substitute(unifMap);
                    exprFromRule = rule.assertion.substitute(subs);
                }else{
                    throw new IllegalStateException("strange, not enough substitution values?");
                }
            }
            if( exprFromRule!=null ){
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
                        next = new ExprTreeEl(calc, subst, false);
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

}
