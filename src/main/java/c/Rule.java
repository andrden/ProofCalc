package c;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by denny on 8/6/15.
 */
public class Rule {
    List<String> lines;

    public Rule(List<String> lines) {
        this.lines = new ArrayList<>(lines);
        for( String l : lines ){
            System.out.println(splitLine(l));
        }
    }

    List<String> splitLine(String line){
        StringTokenizer st = new StringTokenizer(line, " \t");
        List<String> ret = new ArrayList<>();
        while(st.hasMoreTokens()){
            ret.add(st.nextToken());
        }
        return ret;
    }

    @Override
    public String toString() {
        return "Rule{" +
                "lines=" + lines +
                '}';
    }
}
