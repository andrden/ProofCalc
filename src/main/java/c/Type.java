package c;

import java.util.*;

/**
 * Created by denny on 8/11/15.
 */
public class Type {
    static Set<String> vars = new HashSet<>(Arrays.asList("x", "y", "f", "g"));
    static Set<String> consts = new HashSet<>(Arrays.asList("sin", "cos", "π", "√"));

    static boolean isVar(String s){
        return vars.contains(s);
    }
    static boolean isConst(String s){
        return consts.contains(s);
    }
    static boolean isVarOrConst(String s){
        return isVar(s) || isConst(s);
    }
}
