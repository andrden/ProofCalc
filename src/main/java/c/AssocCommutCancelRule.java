package c;

import java.util.*;

/**
 * Created by denny on 8/21/15.
 */
public class AssocCommutCancelRule {
    String rolePlus;
    String roleMinus;
    String roleNeutral;

    public AssocCommutCancelRule(String rolePlus, String roleMinus, String roleNeutral) {
        this.rolePlus = rolePlus;
        this.roleMinus = roleMinus;
        this.roleNeutral = roleNeutral;
    }

    Expr optimizeDeep(Expr e){
        if( e.sub==null ){
            return e;
        }
        List<Expr> newSub = new ArrayList<>();
        for( Expr se : e.sub ){
            newSub.add(optimize(optimizeDeep(se)));
        }
        return optimize(new Expr(e.node, newSub));
    }

    Expr optimize(Expr e){
       // if( e.toMathString().length()>)
        if( e.node.equals(rolePlus) || e.node.equals(roleMinus) ){
            List<Expr> plusList = new ArrayList<>();
            List<Expr> minusList = new ArrayList<>();
            scan(1, e, plusList, minusList);
            //boolean removed = false;
            for( Iterator<Expr> it = plusList.iterator(); it.hasNext(); ){
                Expr i = it.next();
                int idx = minusList.indexOf(i);
                if( idx != -1 ){
                    minusList.remove(idx);
                    it.remove();
              //      removed = true;
                }
            }
            Comparator<Expr> normExprComparator = (o1, o2) -> o1.toLispString().compareTo(o2.toLispString());
            Collections.sort(plusList, normExprComparator);
            Collections.sort(minusList, normExprComparator);
            //if( removed ){
                if( minusList.isEmpty() ){
                    return normalize(plusList);
                }else if( ! plusList.isEmpty() ){
                    return new Expr(roleMinus, normalize(plusList), normalize(minusList));
                } else {
                    return new Expr(roleMinus, new Expr(roleNeutral), normalize(minusList));
                }
            //}
            //return e;
        }else {
            return e;
        }
    }

    Expr normalize( List<Expr> plusList ){
        if( plusList.size()==0 ){
            return new Expr(roleNeutral);
        }else if( plusList.size()==1 ){
            return plusList.get(0);
        }else{
            return new Expr(rolePlus, plusList);
        }
    }

    void scan(int sign, Expr e, List<Expr> plusList, List<Expr> minusList){
        if( e.node.equals(rolePlus) ){
            for( Expr i : e.sub ){
                scan(sign, i, plusList, minusList);
            }
        } else if( e.node.equals(roleMinus) ){
            boolean first = true;
            for( Expr i : e.sub ){
                scan(first ? sign : - sign, i, plusList, minusList);
                first = false;
            }
        } else {
            if( e.node.equals(roleNeutral) && e.sub==null ){
                // skip neutral element
            }else {
                if (sign == 1) plusList.add(e);
                else minusList.add(e);
            }
        }
    }
}
