package c.model;

/**
 * Created by denny on 12/16/15.
 */
public class Normalizer {
    public static final AssocCommutCancelRule plusMinus = new AssocCommutCancelRule("+","-","0",false);
    public static final AssocCommutCancelRule multDiv = new AssocCommutCancelRule("*","/","1",true);

    public static Expr normalize(Expr expr) {
        return multDiv.optimizeDeep(plusMinus.optimizeDeep(multDiv.optimizeDeep(plusMinus.optimizeDeep(expr))));
    }

    static AssocCommutCancelRule getAssocCommuteRule(Expr e){
        if( e.node.equals(plusMinus.rolePlus ) ) return plusMinus;
        if( e.node.equals(multDiv.rolePlus ) ) return multDiv;
        throw new IllegalArgumentException(""+e);
    }
}
