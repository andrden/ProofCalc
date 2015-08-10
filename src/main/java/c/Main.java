package c;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by denny on 8/6/15.
 */
public class Main {
    public static void main(String[] args) throws Exception{
        BufferedReader br =
                new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream("math.txt")));
        Parser parser = new Parser();
        testParseLine(parser);

        List<Rule> rules = parser.parseMathDoc(br);
        System.out.println("Rules="+rules);

    }

    static void testParseLine(Parser parser){
        chk(parser, "3 + 2 + 1", "(+ (+ 3 2) 1)");
        chk(parser, "3 - 2 + 1", "(+ (- 3 2) 1)");
        chk(parser, "3 - ( 2 + 1 )", "(- 3 (+ 2 1))");
        chk(parser, "3 + 2 - 1", "(- (+ 3 2) 1)");
        chk(parser, "3 - 2 - 1", "(- (- 3 2) 1)");
        chk(parser, "3 * 2 + 1", "(+ (* 3 2) 1)");
        chk(parser, "3 + 2 * 1", "(+ 3 (* 2 1))");
        chk(parser, "3 = 2 + 1", "(= 3 (+ 2 1))");
    }

    static void chk(Parser parser, String expr, String lisp){
        Expr e = parser.parse(new LinkedList<>(Util.splitLine(expr)));
        String lispGen = e.toLispString();
        if( ! lispGen.equals(lisp) ){
            throw new RuntimeException(expr+ " => " + lispGen + " NOT " + lisp);
        }
    }
}
