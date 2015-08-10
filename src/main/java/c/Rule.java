package c;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by denny on 8/6/15.
 */
public class Rule {
    boolean quest;
    List<Expr> cond;
    Expr assertion;


    public Rule(boolean quest, Expr assertion, List<Expr> cond) {
        this.quest = quest;
        this.assertion = assertion;
        this.cond = cond;
    }

    @Override
    public String toString() {
        return "\n" +
                "$e " + cond + (quest ? "\n$? " : "\n$a ") + assertion.toLispString() +
                "\n";
    }
}
