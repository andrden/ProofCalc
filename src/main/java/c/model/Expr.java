package c.model;

import java.util.*;

/**
 * Created by denny on 8/10/15.
 */
public class Expr {
    public final String node;
    public final List<Expr> sub;

    public Expr(String node) {
        this.node = node;
        sub = null;
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

    public Expr singleChild(){
        if( sub.size()!=1 ){
            throw new IllegalStateException(node+" sub.size()="+sub.size());
        }
        return sub.get(0);
    }
    public Expr rightChild(){
        if( sub.size()!=2 ){
            throw new IllegalStateException(node+" sub.size()="+sub.size());
        }
        return sub.get(1);
    }

    public Expr rightChildReplace(Expr replacement){
        if( sub.size()!=2 ){
            throw new IllegalStateException(node+" sub.size()="+sub.size());
        }
        return new Expr(node, sub.get(0), replacement);
    }

    void validate(){
        if( node.equals("-") && sub.size()!=1 ){
            throw new IllegalStateException("Only unary minus allowed in internal representations");
        }
        if( node.equals("func") && ! sub.get(0).isVar() ){
            throw new IllegalStateException(""+this);
        }
    }

    boolean isVar(){
        return sub==null && Type.isVar(node);
    }

    public Expr shallowClone(){
        Expr ret = new Expr(node, new ArrayList<>(sub));
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
        if( node.equals("func") ){
            String argVar = sub.get(0).node; // local variable inside this function
            vars = new HashMap<>(vars);
            vars.remove(argVar);
        }
        if( vars.containsKey(node) ){
            return vars.get(node);
        }
        if( sub==null ){
            return this;
        }
        List<Expr> subList = new ArrayList<>();
        for( Expr s : sub ){
            subList.add(s.substitute(vars));
        }
        Expr n = new Expr(node, subList);
        return n;
    }

    public Map<String,Expr> unify(Expr concrete){
        Map<String,Expr> map = new LinkedHashMap<>();
        if( unify(concrete, map) ){
            map.replaceAll((v, e) -> e.simplifyFuncApply());
            return map;
        }
        return null;
    }

    public Expr replaceChild(int i, Expr newChild){
        List<Expr> newSub = new ArrayList<>(sub);
        newSub.set(i, newChild);
        return new Expr(node, newSub);
    }


    public Expr simplifyFuncApply(){
        // (func x (apply cos x)) -> cos
        if( sub==null ){
            return this;
        }

        if( node.equals("func") && rightChild().node.equals("apply") && sub.get(0).equals(rightChild().rightChild()) ){
            return rightChild().sub.get(0);
        }

        List<Expr> subList = new ArrayList<>();
        for( Expr s : sub ){
            subList.add(s.simplifyApplyFunc());
        }
        Expr n = new Expr(node, subList);
        return n;
    }

    public Expr simplifyApplyFunc(){
        //(func x (+ (apply (func x (^ x 2)) x) (apply (func x 7) x)))    ->   (func x (+ (^ x 2) 7))
        if( sub==null ){
            return this;
        }

        if( node.equals("apply") && sub.get(0).node.equals("func") ){
            Expr func = sub.get(0);
            String funcVar = (String)(func.sub.get(0).node);
            return func.rightChild().substitute(Collections.singletonMap(funcVar, rightChild()));
        }

        List<Expr> subList = new ArrayList<>();
        for( Expr s : sub ){
            subList.add(s.simplifyApplyFunc());
        }
        Expr n = new Expr(node, subList);
        return n;
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
        if( node.equals("apply") && sub.get(0).isVar() ){
            String func = sub.get(0).node;

            if( concrete.node.equals("apply") && rightChild().equals(concrete.rightChild()) ){
                vars.put(func, concrete.sub.get(0));
                return true;
            }

            if (vars.get(func) == null) {
                Expr argument = rightChild();
                if (argument.isVar()) {
                    // (apply g x)
                    vars.put(func, new Expr("func", argument, concrete));
                    return true;
                } else {
                    // (apply g (apply h x)) unify to concrete (^ (apply sin x) 3)
                        if (concrete.sub != null) {
                            for (int i = 0; i < concrete.sub.size(); i++) {
                                Expr concreteSub = concrete.sub.get(i);
                                if (concreteSub.sub != null) { // if complex expression, not single term
                                    // trying to find at least one point where inner expression could be unified
                                    if (argument.unify(concreteSub, vars)) {
                                        Expr x = new Expr("x");
                                        vars.put(func, new Expr("func", x, concrete.replaceChild(i, x)));
                                        return true;
                                    }
                                }
                            }
                        }
                }
            }
        }

        if( ! node.equals(concrete.node) ){
            return false;
        }
        if( sub==null && concrete.sub==null ){
            return true;
        }
        if( node.equals("+") && sub.size()==2 && sub.size() < concrete.sub.size() ){
            concrete = new Expr("+", concrete.sub.get(0), new Expr("+", concrete.sub.subList(1,concrete.sub.size())));
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
        if( node.equals("func") ){
            if( sub.size()!=2 ){
                throw new IllegalStateException();
            }
            String myFuncArgVar = sub.get(0).node;
            Expr concreteFuncArgVar = concrete.sub.get(0);
            Expr myExprSubs = rightChild().substitute(Collections.singletonMap(myFuncArgVar, concreteFuncArgVar));

            if( ! myExprSubs.unify(concrete.rightChild(), vars) ){
                return false;
            }
            if( vars.containsKey(concreteFuncArgVar.node) &&
                    ! concreteFuncArgVar.equals(vars.get(concreteFuncArgVar.node)) ){
                // function argument variable, no meaning outside function
                return false;
            }
            vars.remove(concreteFuncArgVar.node);
        }else{
            for( int i=0; i<sub.size(); i++ ){
                if( ! sub.get(i).unify(concrete.sub.get(i), vars) ){
                    return false;
                }
            }
        }
        return true;
    }

    public Set<String> freeVariables(){
        Set<String> set = new HashSet<>();
        if( isVar() ){
            set.add(node);
        }else {
            freeVariables(set);
        }
        return set;
    }

    private void freeVariables(Set<String> fillSet){
        if( sub==null ) return;
        for( Expr e : sub ){
            if( e.isVar() ){
                fillSet.add(e.node);
            }else{
                e.freeVariables(fillSet);
            }
        }
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

    @Override
    public int hashCode() {
        int result = node != null ? node.hashCode() : 0;
        result = 31 * result + (sub != null ? sub.hashCode() : 0);
        return result;
    }
}
