package c;

import java.io.BufferedReader;
import java.io.InputStream;
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
        InputStream streamMath = Main.class.getClassLoader().getResourceAsStream("math.txt");
        InputStream streamMathOverwrite = Main.class.getClassLoader().getResourceAsStream("mathOverwrite.txt");
        if( streamMathOverwrite!=null && streamMathOverwrite.available()<1 ){
            streamMathOverwrite=null;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(
                streamMathOverwrite==null ? streamMath : streamMathOverwrite));

        testParseLine();
        testUnify();

        Parser parser = new Parser();
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

    static void testUnify(){
        Parser parser = new Parser();
        chkUnify(parser, "x + 5", "7 + 5", "{x=7}");
        chkUnify(parser, "x + 1 = 5", "4 + 1 = 5", "{x=4}");
    }

    static void testParseLine(){
        Parser parser = new Parser();
        chk(parser, "3 + 2 + 1", "(+ (+ 3 2) 1)");
        chk(parser, "3 - 2 + 1", "(+ (+ 3 (- 2)) 1)");
        chk(parser, "3 - ( 2 + 1 )", "(+ 3 (- (+ 2 1)))");
        chk(parser, "3 + 2 - 1", "(+ (+ 3 2) (- 1))");
        chk(parser, "3 - 2 - 1", "(+ (+ 3 (- 2)) (- 1))");
        chk(parser, "3 * 2 + 1", "(+ (* 3 2) 1)");
        chk(parser, "3 + 2 * 1", "(+ 3 (* 2 1))");
        chk(parser, "3 = 2 + 1", "(= 3 (+ 2 1))");
        chk(parser, "f x", "(apply f x)");
        chk(parser, "sin π", "(apply sin π)");
        chk(parser, "f ( x )", "(apply f x)");
        chk(parser, "f ( x + 1 )", "(apply f (+ x 1))");
        chk(parser, "g ( f ( x + 1 ) )", "(apply g (apply f (+ x 1)))");
        chk(parser, "5 - f ( x + 1 )", "(+ 5 (- (apply f (+ x 1))))");
        chk(parser, "( sh ψ ) ^ 2 * x - ( ch ψ ) ^ 2 * x = - x",
                "(= (+ (* (^ (apply sh ψ) 2) x) (- (* (^ (apply ch ψ) 2) x))) (- x))");
    }

    static void chkUnify(Parser parser, String template, String concrete, String resMap){
        String res = parser.parse(template).unify(parser.parse(concrete)).toString();
        if( ! res.equals(resMap) ){
            throw new RuntimeException(res + " NOT " + resMap);
        }
    }

    static void chk(Parser parser, String expr, String lisp){
        Expr e = parser.parse(expr);
        String lispGen = e.toLispString();
        if( ! lispGen.equals(lisp) ){
            throw new RuntimeException(expr+ " => " + lispGen + " NOT " + lisp);
        }
    }
}
