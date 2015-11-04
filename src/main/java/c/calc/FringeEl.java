package c.calc;

import c.model.Expr;
import c.model.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
* Created by denny on 9/16/15.
*/
class FringeEl {
    final Expr expr;
    Rule byRule;
    Map<String, Expr> unifMap;
    FringeEl parent;
    //List<FringeEl> subDerivations;

    FringeEl(Expr expr, Rule byRule, Map<String, Expr> unifMap) {
        this.expr = expr;
        this.byRule = byRule;
        this.unifMap = unifMap;
    }

    FringeEl(Expr expr, Rule byRule, Map<String, Expr> unifMap, FringeEl parent) {
        this.expr = expr;
        this.byRule = byRule;
        this.unifMap = unifMap;
        this.parent = parent;
    }

    FringeEl newExpr(Expr enew){
        return new FringeEl(enew, byRule, unifMap, parent);
    }

    @Override
    public String toString() {
        return ""+expr;
    }

    //    FringeEl(Expr expr, Rule byRule, Map<String, Expr> unifMap, List<FringeEl> subDerivations) {
//        this.expr = expr;
//        this.byRule = byRule;
//        this.unifMap = unifMap;
//        this.subDerivations = subDerivations;
//    }

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

    public void printDerivationPath(String indent){
        List<FringeEl> path = new ArrayList<>();
        FringeEl e = this;
        while(e!=null){
            path.add(e);
            e = e.parent;
        }
        Collections.reverse(path);
        for( FringeEl el : path ){
//            if( el.subDerivations!=null && el.subDerivations.size()>0 ){
//                System.out.println("DERIV path: sub-derivs " + el.subDerivations.size());
//            }
            String s = "";
            if( el.byRule!=null ) {
                //s = "By " + el.byRule.assertion.toMathString() + " " + unifMap + " => ";
                s = "By " + el.byRule.toLineString() + " " + (unifMap==null || unifMap.isEmpty()?"":unifMap) + " =>  ";
            }
            System.out.println(indent+"DERIV path: " + s + el.expr.toMathString());
        }
    }
}
