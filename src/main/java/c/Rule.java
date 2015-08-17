package c;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by denny on 8/6/15.
 */
public class Rule {
    List<Expr> cond;
    Expr assertion;


    public Rule(Expr assertion, List<Expr> cond) {
        this.assertion = assertion;
        this.cond = cond;
    }

    @Override
    public String toString() {
        return "\n" +
                "$e " + cond +  "\n$a " + assertion.toLispString() +
                "\n";
    }
}
