package c;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by denny on 8/21/15.
 */
public class PlusMinusRule {
    static Expr optimize(Expr e){
        if( e.node.equals("+") || e.node.equals("-") ){
            List<Expr> plusList = new ArrayList<>();
            List<Expr> minusList = new ArrayList<>();
            scan(1, e, plusList, minusList);
            boolean removed = false;
            for( Iterator<Expr> it = plusList.iterator(); it.hasNext(); ){
                Expr i = it.next();
                int idx = minusList.indexOf(i);
                if( idx != -1 ){
                    minusList.remove(idx);
                    it.remove();
                    removed = true;
                }
            }
            if( removed ){
                if( minusList.isEmpty() ){
                    if( plusList.size()==0 ){
                        return new Expr("0");
                    }else if( plusList.size()==1 ){
                        return plusList.get(0);
                    }else{
                        return new Expr("+", plusList);
                    }
                }
                throw new UnsupportedOperationException();
            }
            return e;
        }else {
            return e;
        }
    }

    static void scan(int sign, Expr e, List<Expr> plusList, List<Expr> minusList){
        if( e.node.equals("+") ){
            for( Expr i : e.sub ){
                scan(sign, i, plusList, minusList);
            }
        } else if( e.node.equals("-") ){
            boolean first = true;
            for( Expr i : e.sub ){
                scan(first ? sign : - sign, i, plusList, minusList);
                first = false;
            }
        } else {
            if( sign == 1 ) plusList.add(e); else minusList.add(e);
        }
    }
}
