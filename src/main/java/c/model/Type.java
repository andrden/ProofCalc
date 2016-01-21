package c.model;

import java.util.*;

/**
 * Created by denny on 8/11/15.
 */
public class Type {
    static Set<String> vars = new HashSet<>(Arrays.asList(
            "a", "b", "c", "n", "x", "y", "z", "f", "g","h", "t", "ψ","ϵ","δ"));
    static Set<String> consts = new HashSet<>(Arrays.asList(
            "True","False",
            "abs", "exp", "sin", "cos", "tan", "sh", "ch", "π", "√", "const",
            "lim0", "∂", "ℝ+",
            "int", // interval in ℝ, e.g. int(-5,5)
            "xx", "yy", "ff", "gg", "hh", "nn"));

    static Set<String> infixOps = new HashSet<>(Arrays.asList("+", "-", "*", "/", "^", "≤","≥", "↦"));

    static boolean isVar(String s){
        if( s.charAt(0)=='x' ){
            for( int i=1; i<s.length(); i++ ){
                if( s.charAt(i)<'0' || s.charAt(i)>'9' ){
                    return false;
                }
            }
            return true; // "x", "x24", "x0", "x555643" are all variables
        }
        return vars.contains(s);
    }
    static boolean isConst(String s){
        return consts.contains(s);
    }
    public static boolean isVarOrConst(String s){
        return isVar(s) || isConst(s);
    }
}
