package c.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by denny on 8/6/15.
 */
public class Rule {
    List<String> srcLines;

    public List<Expr> cond;
    public Expr assertion;
    public final Set<String> freeVariables;


    public Rule(Expr assertion, List<Expr> cond, List<String> srcLines) {
        this.assertion = assertion;
        this.cond = cond;
        this.srcLines = srcLines;
        freeVariables = assertion==null ? null : assertion.freeVariables();
    }

    @Override
    public String toString() {
        if( srcLines!=null ){
            return srcLines.stream().collect(Collectors.joining("\n","\n","\n"));
        }else {
            return "\n" +
                    "$e " + cond + "\n$a " + assertion==null ? "" : assertion.toLispString() +
                    "\n";
        }
    }

    public String toLineString() {
        if( srcLines!=null ){
            return srcLines.stream().collect(Collectors.joining("  "));
        }else {
            return "\n" +
                    "$e " + cond + "\n$a " + assertion.toLispString() +
                    "\n";
        }
    }

    public List<String> getSrcLines() {
        return srcLines;
    }
}
