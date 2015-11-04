package c;

import c.model.Expr;
import c.model.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by denny on 8/17/15.
 */
public class QuestRule extends Rule {
    Expr answer;
    boolean reusable;
    boolean focus;

    public QuestRule(Expr assertion, List<Expr> cond, Expr answer, List<String> srcLines) {
        super(assertion, cond, srcLines);
        this.answer = answer;
    }

    List<Rule> localConditionsAsRules(){
        List<Rule> ret = new ArrayList<>();
        for( Expr e : cond ){
            ret.add(new Rule(e, Collections.emptyList(), null));
        }
        return ret;
    }

}
