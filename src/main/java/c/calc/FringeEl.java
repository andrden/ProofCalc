package c.calc;

import c.Expr;
import c.Rule;

/**
* Created by denny on 9/16/15.
*/
class FringeEl {
    Expr expr;
    Rule byRule;

    FringeEl(Expr expr, Rule byRule) {
        this.expr = expr;
        this.byRule = byRule;
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
