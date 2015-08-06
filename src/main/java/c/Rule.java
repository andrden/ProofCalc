package c;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by denny on 8/6/15.
 */
public class Rule {
    List<String> lines;

    public Rule(List<String> lines) {
        this.lines = new ArrayList<>(lines);
    }

    @Override
    public String toString() {
        return "Rule{" +
                "lines=" + lines +
                '}';
    }
}
