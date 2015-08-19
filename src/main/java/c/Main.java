package c;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by denny on 8/6/15.
 */
public class Main {
    public static void main(String[] args) throws Exception{
        BufferedReader br =
                new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream("math.txt")));
        Parser parser = new Parser();
        testParseLine(parser);

        List<Rule> rulesAndQuests = parser.parseMathDoc(br);

        List<Rule> rules = rulesAndQuests.stream()
                .filter(r -> !(r instanceof QuestRule))
                .collect(Collectors.toCollection(ArrayList::new));

        //System.out.println("Rules="+rules);
        for( Rule r : rulesAndQuests ){
            if( r instanceof QuestRule ){
                List<Rule> allRules = new ArrayList<>();
                allRules.addAll(rules);
                QuestRule qrule = (QuestRule) r;
                allRules.addAll(qrule.localConditionsAsRules());
                Expr ret = new Calc(allRules).quest(r);
                if( qrule.answer==null ){
                    System.out.println("Correct answer was not specified!");
                }else if( ! ret.toLispString().equals(qrule.answer.toLispString()) ){
                    throw new IllegalStateException("Not reached answer="+qrule.answer);
                }
            }
        }

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
        chk(parser, "f x", "(apply f x)");
        chk(parser, "sin π", "(apply sin π)");
        chk(parser, "f ( x )", "(apply f x)");
        chk(parser, "f ( x + 1 )", "(apply f (+ x 1))");
        chk(parser, "g ( f ( x + 1 ) )", "(apply g (apply f (+ x 1)))");
        chk(parser, "5 - f ( x + 1 )", "(- 5 (apply f (+ x 1)))");
    }

    static void chk(Parser parser, String expr, String lisp){
        Expr e = parser.parse(new LinkedList<>(Util.splitLine(expr)));
        String lispGen = e.toLispString();
        if( ! lispGen.equals(lisp) ){
            throw new RuntimeException(expr+ " => " + lispGen + " NOT " + lisp);
        }
    }
}
