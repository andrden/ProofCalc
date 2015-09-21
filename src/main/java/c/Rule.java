package c;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by denny on 8/6/15.
 */
public class Rule {
    public List<Expr> cond;
    public Expr assertion;
    public final Set<String> freeVariables;


    public Rule(Expr assertion, List<Expr> cond) {
        this.assertion = assertion;
        this.cond = cond;
        freeVariables = assertion.freeVariables();
    }

    @Override
    public String toString() {
        return "\n" +
                "$e " + cond +  "\n$a " + assertion.toLispString() +
                "\n";
    }
}
