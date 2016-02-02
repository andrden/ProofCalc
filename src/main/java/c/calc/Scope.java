package c.calc;

import c.model.Expr;

import java.io.Closeable;

/**
 * Created by denny on 2/2/16.
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
