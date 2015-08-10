package c;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by denny on 8/10/15.
 */
public class Expr {
    String node;
    List<Expr> sub;

    public Expr(String node) {
        this.node = node;
    }
    public Expr(String node, Expr a1, Expr a2) {
        this.node = node;
        sub = new ArrayList<>();
        sub.add(a1);
        sub.add(a2);
    }

    public String toLispString() {
        if( sub==null ) return node;
        String ret = "(" + node;
        for( Expr e : sub ){
            ret += " " + e.toLispString();
        }
        return ret + ")";
    }
}
