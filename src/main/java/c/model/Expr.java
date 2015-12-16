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
        if( node.equals("func") && ! sub.get(0).equals(expr.sub.get(0)) ){
            // make sure "x ↦ (1 + x)" equals "y ↦ (1 + y)"
            Set<String> usedVars = new HashSet<>();
            usedVars.addAll( freeVariables() );
            usedVars.addAll( expr.freeVariables() );
            Expr newVar = newVariable(usedVars); // change both functions to use new free variable
            Expr thisSubst = substitute(Collections.singletonMap(sub.get(0).node, newVar));
            Expr exprSubst = substitute(Collections.singletonMap(expr.sub.get(0).node, newVar));
            return thisSubst.equals(exprSubst);
        }
        if (sub != null ? !sub.equals(expr.sub) : expr.sub != null) return false;

        return true;
    }

    Expr newVariable(Set<String> usedVars){
        for( int i=1; ; i++ ){
            String v = "x" + i;
            if( ! usedVars.contains(v) ){
                return new Expr(v);
            }
        }
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

    List<Map<String,Expr>> unifyImpl(Expr concrete){
        Map<String,Expr> map = new LinkedHashMap<>();
        List<Map<String,Expr>> cases = unify(concrete, map);
        if( ! cases.isEmpty() ){
            for( Map<String,Expr> m : cases ) {
                m.replaceAll((v, e) -> e.simplifyFuncApply());
            }
        }
        return cases;
    }

    public Map<String,Expr> unify(Expr concrete){
        List<Map<String,Expr>> cases = unifyList(concrete);
        if( cases.isEmpty() ){
            return null;
        }
        return cases.get(0);
    }

    private List<Map<String,Expr>> unifyList(Expr concrete) {
        List<Map<String,Expr>> cases = unifyImpl(concrete);
        if( ! cases.isEmpty() ){
            return cases;
        }
        // retry for the case of unifying "x ↦ cos(g(x))" with "cos"
        if( node.equals("func") && rightChild().node.equals("apply") && ! concrete.node.equals("func") ){
            Expr var = sub.get(0);
            Expr altConcrete = new Expr("func",var, new Expr("apply", concrete, var));
            cases = unifyImpl(altConcrete);
        }
        return cases;
    }

    public Expr replaceChild(int i, Expr newChild){
        List<Expr> newSub = new ArrayList<>(sub);
        newSub.set(i, newChild);
        return new Expr(node, newSub);
    }

    public Expr simplifyFuncOrApply(){
        for( Expr e = this; ; ){
            Expr e1 = e.simplifyApplyFunc().simplifyFuncApply();
            if( e1.equals(e) ){
                return e;
            }
            e = e1; // and simplify further
        }
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
            Expr subs = func.rightChild().substitute(Collections.singletonMap(funcVar, rightChild()));
            return subs.simplifyApplyFunc(); // maybe there are several nested functions
        }

        List<Expr> subList = new ArrayList<>();
        for( Expr s : sub ){
            subList.add(s.simplifyApplyFunc());
        }
        Expr n = new Expr(node, subList);
        return n;
    }

    List<Map<String,Expr>> unify(Expr concrete, Map<String,Expr> vars){
        List<Map<String,Expr>> cases = new ArrayList<>();
        if( isVar() ){
            Expr val = vars.get(node);
            if( val==null ){
                vars.put(node, concrete);
                cases.add(vars);
                return cases;
            }
            if( val.equals(concrete) ){
                cases.add(vars);
                return cases;
            }
            return cases;
        }
        if( node.equals("apply") && sub.get(0).isVar() ){
            String func = sub.get(0).node;

            if( concrete.node.equals("apply") && rightChild().equals(concrete.rightChild()) ){
                vars.put(func, concrete.sub.get(0));
                cases.add(vars);
                return cases;
            }

            if (vars.get(func) == null) {
                Expr argument = rightChild();
                if (argument.isVar()) {
                    // (apply g x)
                    vars.put(func, new Expr("func", argument, concrete));
                    cases.add(vars);
                    return cases;
                } else {
                    // (apply g (apply h x)) unify to concrete (^ (apply sin x) 3)
                        if (concrete.sub != null) {
                            for (int i = 0; i < concrete.sub.size(); i++) {
                                Expr concreteSub = concrete.sub.get(i);
                                if (concreteSub.sub != null) { // if complex expression, not single term
                                    // trying to find at least one point where inner expression could be unified
                                    List<Map<String,Expr>> sub = argument.unify(concreteSub, vars);
                                    if (! sub.isEmpty()) {
                                        for( Map<String,Expr> m : sub ) {
                                            Expr x = new Expr("x");
                                            m.put(func, new Expr("func", x, concrete.replaceChild(i, x)));
                                        }
                                        cases.addAll(sub);
                                        return cases;
                                    }
                                }
                            }
                        }
                }
            }
        }

        if( ! node.equals(concrete.node) ){
            return cases;
        }
        if( sub==null && concrete.sub==null ){
            cases.add(vars);
            return cases;
        }
        if( node.equals("+") && sub.size()==2 && sub.size() < concrete.sub.size() ){
            concrete = new Expr("+", concrete.sub.get(0), new Expr("+", concrete.sub.subList(1,concrete.sub.size())));
        }
        if( sub.size()!=concrete.sub.size() ){
            return cases;
        }
        if (! subUnify(concrete, vars)){
            if( "+".equals(node) && sub.size()==2 ){
                // try swapping and unifying the other way
                if (! subUnify(new Expr(concrete.node, concrete.sub.get(1), concrete.sub.get(0)), vars)){
                    return cases; // tried 2 ways and failed
                }
                cases.add(vars);
                return cases;
                //return true; // swapped unification succeeded
            }
            return cases;
        }
        cases.add(vars);
        return cases;
        //return true;
    }

    boolean subUnify(Expr concrete, Map<String, Expr> vars) {
        if( node.equals("func") ){
            if( sub.size()!=2 ){
                throw new IllegalStateException();
            }
            String myFuncArgVar = sub.get(0).node;
            Expr concreteFuncArgVar = concrete.sub.get(0);
            Expr myExprSubs = rightChild().substitute(Collections.singletonMap(myFuncArgVar, concreteFuncArgVar));

            List<Map<String,Expr>> cases = myExprSubs.unify(concrete.rightChild(), vars);
            if( cases.isEmpty() ){
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
                List<Map<String,Expr>> cases = sub.get(i).unify(concrete.sub.get(i), vars);
                if( cases.isEmpty() ){
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
