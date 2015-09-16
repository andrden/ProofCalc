package c;

import java.util.*;

/**
 * Created by denny on 8/10/15.
 */
public class Expr {
    public String node;
    public List<Expr> sub;

    public Expr(String node) {
        this.node = node;
    }

    public Expr(String node, Expr a1) {
        this.node = node;
        sub = new ArrayList<>();
        sub.add(a1);
        validate();
    }

    public Expr(String node, Expr a1, Expr a2) {
        this.node = node;
        sub = new ArrayList<>();
        sub.add(a1);
        sub.add(a2);
        validate();
    }

    public Expr(String node, List<Expr> sub) {
        this.node = node;
        this.sub = sub;
        validate();
    }

    void validate(){
        if( node.equals("-") && sub.size()!=1 ){
            throw new IllegalStateException("Only unary minus allowed in internal representations");
        }
    }

    boolean isVar(){
        return sub==null && Type.isVar(node);
    }

    public Expr shallowClone(){
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

    public Expr substitute(Map<String,Expr> vars){
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

    public Map<String,Expr> unify(Expr concrete){
        Map<String,Expr> map = new HashMap<>();
        if( unify(concrete, map) ){
            return map;
        }
        return null;
    }

    boolean unify(Expr concrete, Map<String,Expr> vars){
        if( isVar() ){
            Expr val = vars.get(node);
            if( val==null ){
                vars.put(node, concrete);
                return true;
            }
            return val.equals(concrete);
        }

        if( ! node.equals(concrete.node) ){
            return false;
        }
        if( sub==null && concrete.sub==null ){
            return true;
        }
       if( sub.size()!=concrete.sub.size() ){
           return false;
       }
        if (! subUnify(concrete, vars)){
            if( "+".equals(node) && sub.size()==2 ){
                // try swapping and unifying the other way
                if (! subUnify(new Expr(concrete.node, concrete.sub.get(1), concrete.sub.get(0)), vars)){
                    return false; // tried 2 ways and failed
                }
                return true; // swapped unification succeeded
            }
            return false;
        }
        return true;
    }

    boolean subUnify(Expr concrete, Map<String, Expr> vars) {
        for( int i=0; i<sub.size(); i++ ){
          if( ! sub.get(i).unify(concrete.sub.get(i), vars) ){
              return false;
          }
        }
        return true;
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
        if( node.equals("-") ) {
            return "-" + sub.get(0).toMathString();
        } if( Type.infixOps.contains(node) && sub.size()>1 ){
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

    /*
\begin{math}
x^2
\end{math}
     */
    public String toLatexString() {
        if( sub==null ) return node;
        if( "/".equals(node) && sub.size()==2 ) {
            return "\\frac{"+sub.get(0).toLatexString()+"}{"+sub.get(1).toLatexString()+"}";
        } else if( Type.infixOps.contains(node) && sub.size()>1 ){
            String ret = "(";
            int i=0;
            for (Expr e : sub) {
                ret += e.toLatexString();
                if( i<sub.size()-1 ){
                    ret += " " + node + " ";
                }
                i++;
            }
            return ret + ")";
        }else if( node.equals("apply") && sub.size()==2 ) {
            String func = sub.get(0).toLatexString();
            if( "exp".equals(func) ){
                return "e^{"+sub.get(1).toLatexString()+"}";
            }else {
                String ret = "(";
                ret += func + " " + sub.get(1).toLatexString();
                return ret + ")";
            }
        } else {
            String ret = "(" + node;
            for (Expr e : sub) {
                ret += " " + e.toLatexString();
            }
            return ret + ")";
        }
    }

    @Override
    public String toString() {
        return toLispString();
    }
}
