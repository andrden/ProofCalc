package c.calc;

import c.model.Expr;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains expressions like (∈ δ ℝ+) while we scan inside quantified expressions, like "∀ϵ ∈ ℝ+ ∃δ ∈ ℝ+ ..."
 * so that we could use those expressions when checking rule conditions
 */
public class Scope {
    Expr e;
    Scope parent;

    public Scope(){

    }

    public Scope(Expr e, Scope parent) {
        this.e = e;
        this.parent = parent;
    }

    Scope push(Expr e){
        return new Scope(e, this);
    }

    void populate(Set<Expr> set){
        if( e!= null ) {
            set.add(e);
        }
        if( parent!=null ){
            parent.populate(set);
        }
    }

    Set<Expr> all(){
        if( e==null ){
            return Collections.emptySet();
        }
        Set<Expr> s = new HashSet<>();
        populate(s);
        return s;
    }

    boolean has(Expr test){
        if( test.equals(e) ){
            return true;
        }
        if( parent!=null ){
            return parent.has(test);
        }
        return false;
    }
}
