package c;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by denny on 8/11/15.
 */
public class Type {
    static List vars = Arrays.asList("x","f","g");
    static boolean isVar(String s){
        return vars.contains(s);
    }
}
