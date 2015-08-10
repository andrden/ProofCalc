package c;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by denny on 8/6/15.
 */
public class Rule {
    List<String> lines;
    Expr assertion;
    List<List<String>> cond = new ArrayList<>();

    public Rule(Expr assertion) {
        this.assertion = assertion;
    }

    @Override
    public String toString() {
        return "\n" +
                "$e " + cond + "\n$a "+assertion.toLispString() +
                "\n";
    }
}
