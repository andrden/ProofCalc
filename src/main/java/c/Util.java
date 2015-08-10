package c;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by denny on 8/10/15.
 */
public class Util {
    static List<String> splitLine(String line){
        StringTokenizer st = new StringTokenizer(line, " \t");
        List<String> ret = new ArrayList<>();
        while(st.hasMoreTokens()){
            ret.add(st.nextToken());
        }
        return ret;
    }
}
