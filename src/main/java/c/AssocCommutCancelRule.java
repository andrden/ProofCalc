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

    List<Expr> separateAllPossiblePairs(Expr e){
        List<Expr> variants = new ArrayList<>();
        if( e.node.equals(rolePlus) && e.sub.size()>2 ) {
            List<Expr> plusList = new ArrayList<>();
            List<Expr> minusList = new ArrayList<>();
            scan(1, e, plusList, minusList);
            for( Expr em : minusList ){
                plusList.add(new Expr(roleMinus, em));
            }
            for( int i=0; i<plusList.size(); i++ ){
                for( int j=i+1; j<plusList.size(); j++ ){
                    Expr pair = new Expr(rolePlus, plusList.get(i), plusList.get(j));
                    List<Expr> rest = new ArrayList<>(plusList);
                    rest.remove(j);
                    rest.remove(i);
                    variants.add(new Expr(rolePlus, pair, new Expr(rolePlus, rest)));
                }
            }
        }
        return variants;
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

            return assemble(plusList, minusList);
        }else {
            return e;
        }
    }

    Expr assemble(List<Expr> plusList, List<Expr> minusList) {
        List<Expr> all = new ArrayList<>(plusList);
        for( Expr e : minusList ){
            all.add(new Expr(roleMinus, e));
        }
        if( all.size()==0 ){
            return new Expr(roleNeutral);
        }
        if( all.size()==1 ){
            return all.get(0);
        }
        return new Expr(rolePlus, all);
    }

//    Expr normalize( List<Expr> plusList ){
//        if( plusList.size()==0 ){
//            return new Expr(roleNeutral);
//        }else if( plusList.size()==1 ){
//            return plusList.get(0);
//        }else{
//            return new Expr(rolePlus, plusList);
//        }
//    }

    void scan(int sign, Expr e, List<Expr> plusList, List<Expr> minusList){
        if( e.node.equals(rolePlus) ){
            for( Expr i : e.sub ){
                scan(sign, i, plusList, minusList);
            }
        } else if( e.node.equals(roleMinus) ){
             scan(- sign, e.sub.get(0), plusList, minusList);
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
