package c.calc;

import c.Expr;
import c.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
* Created by denny on 9/16/15.
*/
class FringeEl {
    Expr expr;
    Rule byRule;
    Map<String, Expr> unifMap;
    FringeEl parent;

    FringeEl(Expr expr, Rule byRule, Map<String, Expr> unifMap) {
        this.expr = expr;
        this.byRule = byRule;
        this.unifMap = unifMap;
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

    public void printDerivationPath(){
        List<FringeEl> path = new ArrayList<>();
        FringeEl e = this;
        while(e!=null){
            path.add(e);
            e = e.parent;
        }
        Collections.reverse(path);
        for( FringeEl el : path ){
            String s = "";
            if( el.byRule!=null ) {
                //s = "By " + el.byRule.assertion.toMathString() + " " + unifMap + " => ";
                s = "By " + el.byRule.toString() + " " + unifMap + " => ";
            }
            System.out.println("DERIV path: " + s + el.expr.toMathString());
        }
    }
}
