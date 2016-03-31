package c.model;

import java.util.*;
import java.util.function.Function;

/**
 * Created by denny on 8/10/15.
 */
public class Expr {
    public final String node;
    final Expr[] sub;
    private String lispString;
    int minElems;

    public static Expr True = node("True");
    public static Expr False = node("False");

    public static Expr node(String node){
        return new Expr(node);
    }

    public Expr(String node) {
        this.node = node;
        sub = null;
        validate();
    }

    public Expr(String node, Expr a1) {
        this.node = node;
        sub = new Expr[]{a1};
        validate();
    }

    public Expr(String node, Expr a1, Expr a2) {
        this.node = node;
        sub = new Expr[]{a1, a2};
        validate();
    }

    public Expr(String node, Expr a1, Expr a2, Expr a3) {
        this.node = node;
        sub = new Expr[]{a1, a2, a3};
        validate();
    }

    public Expr(String node, List<Expr> sub) {
        this.node = node;
        this.sub = sub.toArray(new Expr[sub.size()]);
        validate();
    }

    public Expr singleChild(){
        if( sub.length!=1 ){
            throw new IllegalStateException(node+" sub.length="+sub.length);
        }
        return sub[0];
    }
    public Expr rightChild(){
        if( sub.length!=2 ){
            throw new IllegalStateException(node+" sub.length="+sub.length);
        }
        return sub[1];
    }

    public Expr lastChild(){
        return sub[sub.length-1];
    }

    public Expr child(int i){
        return sub[i];
    }

    public boolean hasChildren(){
        return sub!=null;
    }

    public boolean isQuantified(){
        return node.equals("∀") || node.equals("∃");
    }

    public int subCount(){
        return sub.length;
    }

    public Expr rightChildReplace(Expr replacement){
        if( sub.length!=2 ){
            throw new IllegalStateException(node+" sub.length="+sub.length);
        }
        return new Expr(node, sub[0], replacement);
    }

    void validate(){
        if( node.equals("-") && sub.length!=1 ){
            throw new IllegalStateException("Only unary minus allowed in internal representations");
        }
        if( node.equals("func") && ! sub[0].isVar() ){
            throw new IllegalStateException(""+this);
        }
        minElems = 1;
        if( sub!=null && ! node.equals("apply") && ! node.equals("func")){
            // Note for apply: "x ↦ cos(g(x))" can be unified with "x ↦ cos(x)" using "{g=(func x x)}
            // Note for func: "x ↦ cos(g(x))" can be unified with "cos" using "{g=(func x x)}"
            for( Expr e : sub ){
                minElems += e.minElems;
            }
        }
    }

    boolean isVar(){
        return sub==null && Type.isVar(node);
    }

    public Expr shallowClone(){
        Expr ret = new Expr(node, new ArrayList<>(Arrays.asList(sub)));
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expr)) return false;

        Expr expr = (Expr) o;

        if (node != null ? !node.equals(expr.node) : expr.node != null) return false;
        if( node.equals("func") && ! sub[0].equals(expr.sub[0]) ){
            // make sure "x ↦ (1 + x)" equals "y ↦ (1 + y)"
            Set<String> usedVars = new HashSet<>();
            usedVars.addAll( freeVariables() );
            usedVars.addAll( expr.freeVariables() );
            Expr newVar = newVariable(usedVars); // change both functions to use new free variable
            Expr thisSubst = substitute(Collections.singletonMap(sub[0].node, newVar));
            Expr exprSubst = substitute(Collections.singletonMap(expr.sub[0].node, newVar));
            return thisSubst.equals(exprSubst);
        }
        if (sub != null ? !Arrays.equals(sub, expr.sub) : expr.sub != null) return false;

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
            String argVar = sub[0].node; // local variable inside this function
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
        List<Map<String,Expr>> cases = unify(concrete, map, new HashSet<>());
        if( ! cases.isEmpty() ){
            for( Map<String,Expr> m : cases ) {
                m.replaceAll((v, e) -> e.simplifyFuncApply());
            }
        }
        return cases;
    }

    public Map<String,Expr> unify(Expr concrete){
        List<Map<String,Expr>> cases = unifyOptions(concrete);
        if( cases.isEmpty() ){
            return null;
        }
//        if( cases.size()!=1 ){
//            throw new IllegalStateException();
//        }
        return cases.get(0);
    }

    public List<Map<String,Expr>> unifyOptions(Expr concrete) {
        if( minElems > concrete.minElems){
            return Collections.emptyList();
        }
        List<Map<String,Expr>> cases = unifyImpl(concrete);
        if( ! cases.isEmpty() ){
            return cases;
        }
        // retry for the case of unifying "x ↦ cos(g(x))" with "cos"
        if( node.equals("func") && rightChild().node.equals("apply") && ! concrete.node.equals("func") ){
            Expr var = sub[0];
            Expr altConcrete = new Expr("func",var, new Expr("apply", concrete, var));
            cases = unifyImpl(altConcrete);
        }
        return cases;
    }

    public Expr replaceChild(int i, Expr newChild){
        List<Expr> newSub = new ArrayList<>(Arrays.asList(sub));
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

    Expr simplifier(Function<Expr,Expr> shallowSimplifyOper){
        if( sub==null ){
            return this;
        }

        Expr enew = shallowSimplifyOper.apply(this);
        if( enew != this ){
            return enew;
        }

        boolean change = false;
        for( Expr s : sub ){
            if( s != s.simplifier(shallowSimplifyOper) ){
                change = true;
                break;
            }
        }
        if( ! change ){
            return this;
        }

        List<Expr> subList = new ArrayList<>();
        for( Expr s : sub ){
            subList.add(s.simplifier(shallowSimplifyOper));
        }
        Expr n = new Expr(node, subList);
        return n;
    }

    static final Function<Expr,Expr> simpFuncApply = (e) -> {
        // (func x (apply cos x)) -> cos
        if( e.node.equals("func") && e.rightChild().node.equals("apply") && e.sub[0].equals(e.rightChild().rightChild()) ){
            return e.rightChild().sub[0];
        }
        return e;
    };

    static final Function<Expr,Expr> simpApplyFunc = (e) -> {
        //(func x (+ (apply (func x (^ x 2)) x) (apply (func x 7) x)))    ->   (func x (+ (^ x 2) 7))
        if( e.node.equals("apply") && e.sub[0].node.equals("func") ){
            Expr func = e.sub[0];
            String funcVar = (String)(func.sub[0].node);
            Expr subs = func.rightChild().substitute(Collections.singletonMap(funcVar, e.rightChild()));
            return subs.simplifyApplyFunc(); // maybe there are several nested functions
        }
        return e;
    };


    public Expr simplifyFuncApply(){
        // (func x (apply cos x)) -> cos
        return simplifier(simpFuncApply);
    }

    public Expr simplifyApplyFunc(){
        //(func x (+ (apply (func x (^ x 2)) x) (apply (func x 7) x)))    ->   (func x (+ (^ x 2) 7))
        return simplifier(simpApplyFunc);
    }

    boolean disjoint(Set<String> a, Set<String> b){
        for( String s : a ){
            if( b.contains(s) ){
                return false;
            }
        }
        return true;
    }

    List<Map<String,Expr>> unify(Expr concrete, Map<String,Expr> vars, Set<String> argsVars){
        List<Map<String,Expr>> cases = new ArrayList<>();
        if( isVar() ){
            Expr val = vars.get(node);
            if( val==null ){
                if( disjoint(concrete.freeVariables(), argsVars) || concrete.equals(this) ) {
                    vars.put(node, concrete);
                    cases.add(vars);
                }
                return cases;
            }
            if( val.equals(concrete) ){
                cases.add(vars);
                return cases;
            }
            return cases;
        }
        if( node.equals("apply") && sub[0].isVar() ){
            String func = sub[0].node;

            if( concrete.node.equals("apply") && rightChild().equals(concrete.rightChild()) ){
                vars.put(func, concrete.sub[0]);
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
                            for (int i = 0; i < concrete.sub.length; i++) {
                                Expr concreteSub = concrete.sub[i];
                                if (concreteSub.sub != null) { // if complex expression, not single term
                                    // trying to find at least one point where inner expression could be unified
                                    List<Map<String,Expr>> sub = argument.unify(concreteSub, vars, argsVars);
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
        if( (node.equals("+") || node.equals("*")) && sub.length==2 && sub.length < concrete.sub.length ){
            Set<Expr> optionsConcrete = Normalizer.getAssocCommuteRule(concrete).separateAllPossiblePairs(concrete);
            // options here must be only groupings, without transposed pair treated as different, otherwise we will
            // produce g=(func y (* 2 (apply cos y)) and g=(func y (* (apply cos y) 2)) which are actually the same function
            // and will only increase branching factor, not adding new possibilities for unification
            for( Expr e : optionsConcrete ){
                cases.addAll(subUnify0(e, new HashMap<>(vars), argsVars));
            }
            return cases;
            //concrete = new Expr("+", concrete.sub[0], new Expr("+", concrete.sub.subList(1,concrete.sub.length)));
        }else {
            return subUnify0(concrete, vars, argsVars);
        }
    }

    private List<Map<String, Expr>> subUnify0(Expr concrete, Map<String, Expr> vars, Set<String> argsVars) {
        List<Map<String,Expr>> cases = new ArrayList<>();
        if( sub.length!=concrete.sub.length ){
            return cases;
        }
        cases = subUnify(concrete, vars, argsVars);
        if ( cases.isEmpty() ){
            if( "+".equals(node) && sub.length==2 ){
                // try swapping and unifying the other way
                cases = subUnify(new Expr(concrete.node, concrete.sub[1], concrete.sub[0]), vars, argsVars);
                return cases;
            }
            return cases;
        }
        return cases;
    }

    Set<String> extend(Set<String> argsVars, String s){
        Set<String> ret = new HashSet<>(argsVars);
        ret.add(s);
        return ret;
    }

    List<Map<String,Expr>> subUnify(Expr concrete, Map<String, Expr> vars, Set<String> argsVars) {
        if( node.equals("func") ){
            if( sub.length!=2 ){
                throw new IllegalStateException();
            }
            String myFuncArgVar = sub[0].node;
            Expr concreteFuncArgVar = concrete.sub[0];
            if( !myFuncArgVar.equals(concreteFuncArgVar.node) && rightChild().hasFreeVar(concreteFuncArgVar.node) ){
                // can't unify ( y ↦ x ) with ( x ↦ x )
                return Collections.emptyList();
                // maybe too restrictive fix,
                // but at least should not try to change this function ( y ↦ x ) to use function variable x
            }
            Expr myExprSubs = rightChild().substitute(Collections.singletonMap(myFuncArgVar, concreteFuncArgVar));

            List<Map<String,Expr>> cases = myExprSubs.unify(concrete.rightChild(), vars, extend(argsVars, myFuncArgVar));
            List<Map<String,Expr>> badCases = new ArrayList<>();
            for( Map<String,Expr> vs : cases ){
                if( vs.containsKey(concreteFuncArgVar.node) &&
                        ! concreteFuncArgVar.equals(vs.get(concreteFuncArgVar.node)) ){
                    // function argument variable, no meaning outside function
                    badCases.add(vs);
                }else {
                    vs.remove(concreteFuncArgVar.node);
                }
            }
            cases.removeAll(badCases);
            return cases;
        }else{
            List<Map<String,Expr>> cases = Collections.singletonList(vars);
            for( int i=0; i<sub.length; i++ ){
                List<Map<String,Expr>> casesNew = new ArrayList<>();
                for( Map<String,Expr> vs : cases ) {
                    Expr subTemplate = sub[i];
                    Expr subConcrete = concrete.sub[i];
                    casesNew.addAll(subTemplate.unify(subConcrete, vs, argsVars));
                }
                cases = casesNew;
            }
            return cases;
        }
    }

    public boolean hasFreeVar(String v){
        return freeVariables().contains(v);
    }

    public Set<String> freeVariables(){
        Set<String> set = new HashSet<>();
        Set<String> bound = new HashSet<>();
        if( isVar() ){
            set.add(node);
        }else {
            freeVariables(set, bound);
        }
        return set;
    }

    boolean isFunc(){
        return node.equals("func");
    }

    private void freeVariables(Set<String> fillSet, Set<String> bound){
        if( sub==null ) return;
        if( isFunc() ){
            if( ! bound.add(child(0).node) ) { // func scope
                throw new IllegalStateException("nested functions with same var.");
            }
        }
        for( Expr e : sub ){
            if( e.isVar() ){
                if( ! bound.contains(e.node) ) {
                    fillSet.add(e.node);
                }
            }else{
                e.freeVariables(fillSet, bound);
            }
        }
        if( isFunc() ){
            bound.remove(child(0).node); // end func scope
        }
    }

    public String toLispString() {
        if( sub==null ) return node;
        if( lispString==null ) {
            String ret = "(" + node;
            for (Expr e : sub) {
                ret += " " + e.toLispString();
            }
            lispString = ret + ")";
        }
        return lispString;
    }

    public String toMathString() {
        if( sub==null ) return node;
        if( node.equals("-") ) {
            return "-" + sub[0].toMathString();
        } if( Type.infixOps.contains(node) && sub.length>1 ){
            String ret = "(";
            int i=0;
            for (Expr e : sub) {
                ret += e.toMathString();
                if( i<sub.length-1 ){
                    ret += " " + node + " ";
                }
                i++;
            }
            return ret + ")";
        }else if( node.equals("apply") && sub.length==2 ) {
            String ret = "(" ;
            ret += sub[0].toMathString() + " " + sub[1].toMathString();
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
        if( "/".equals(node) && sub.length==2 ) {
            return "\\frac{"+sub[0].toLatexString()+"}{"+sub[1].toLatexString()+"}";
        } else if( Type.infixOps.contains(node) && sub.length>1 ){
            String ret = "(";
            int i=0;
            for (Expr e : sub) {
                ret += e.toLatexString();
                if( i<sub.length-1 ){
                    ret += " " + node + " ";
                }
                i++;
            }
            return ret + ")";
        }else if( node.equals("apply") && sub.length==2 ) {
            String func = sub[0].toLatexString();
            if( "exp".equals(func) ){
                return "e^{"+sub[1].toLatexString()+"}";
            }else {
                String ret = "(";
                ret += func + " " + sub[1].toLatexString();
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
        result = 31 * result + (sub != null ? Arrays.hashCode(sub) : 0);
        return result;
    }
}
