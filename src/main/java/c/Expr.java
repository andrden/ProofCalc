package c;

import java.util.*;

/**
 * Created by denny on 8/10/15.
 */
public class Expr {
    String node;
    List<Expr> sub;

    public Expr(String node) {
        this.node = node;
    }

    public Expr(String node, Expr a1) {
        this.node = node;
        sub = new ArrayList<>();
        sub.add(a1);
    }

    public Expr(String node, Expr a1, Expr a2) {
        this.node = node;
        sub = new ArrayList<>();
        sub.add(a1);
        sub.add(a2);
    }

    boolean isVar(){
        return sub==null && Type.isVar(node);
    }

    Expr shallowClone(){
        Expr ret = new Expr(node);
        ret.sub = new ArrayList<>(sub);
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expr)) return false;

        Expr expr = (Expr) o;

        if (node != null ? !node.equals(expr.node) : expr.node != null) return false;
        if (sub != null ? !sub.equals(expr.sub) : expr.sub != null) return false;

        return true;
    }

    Expr substitute(Map<String,Expr> vars){
        if( vars.containsKey(node) ){
            return vars.get(node);
        }
        if( sub==null ){
            return this;
        }
        Expr n = new Expr(node);
        n.sub = new ArrayList<>();
        for( Expr s : sub ){
            n.sub.add(s.substitute(vars));
        }
        return n;
    }

    Map<String,Expr> unify(Expr concrete){
        if( ! node.equals(concrete.node) ){
            return null;
        }
        if( sub==null && concrete.sub==null ){
            return Collections.singletonMap(node, this); // same variable
        }
       if( sub.size()!=concrete.sub.size() ){
           return null;
       }
       Map<String,Expr> map = new HashMap<>();
       for( int i=0; i<sub.size(); i++ ){
         if( ! sub.get(i).equals(concrete.sub.get(i)) ){
             if( !sub.get(i).isVar() ){
                 return null;
             }
             if( map.containsKey(sub.get(i).node) ){
                 throw new IllegalStateException();
             }
             map.put(sub.get(i).node, concrete.sub.get(i));
         }
       }
       return map;
    }

    public String toLispString() {
        if( sub==null ) return node;
        String ret = "(" + node;
        for( Expr e : sub ){
            ret += " " + e.toLispString();
        }
        return ret + ")";
    }

    public String toMathString() {
        if( sub==null ) return node;
        if( Type.infixOps.contains(node) && sub.size()>1 ){
            String ret = "(";
            int i=0;
            for (Expr e : sub) {
                ret += e.toMathString();
                if( i<sub.size()-1 ){
                    ret += " " + node + " ";
                }
                i++;
            }
            return ret + ")";
        }else if( node.equals("apply") && sub.size()==2 ) {
            String ret = "(" ;
            ret += sub.get(0).toMathString() + " " + sub.get(1).toMathString();
            return ret + ")";
        } else {
            String ret = "(" + node;
            for (Expr e : sub) {
                ret += " " + e.toMathString();
            }
            return ret + ")";
        }
    }

    @Override
    public String toString() {
        return toLispString();
    }
}
