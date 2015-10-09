package c;

import c.calc.Calc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by denny on 8/6/15.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        //new Date(1443657600000L).toString();

        testParseLine();
        testUnify();

        //runPieces();
        runMainFile();

        //runMath(new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream("piece6.txt"))));
    }

    static void runPieces() throws Exception{
        List<String> okPieces = new ArrayList<>();
        for( int p=1; ; p++ ) {
            String fname = "piece" + p + ".txt";
            InputStream streamMath = Main.class.getClassLoader().getResourceAsStream(fname);
            if( streamMath==null ){
                break;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(streamMath));
            System.out.println("### ### running "+fname);
            try {
                runMath(br);
            }catch (Exception e){
                throw new RuntimeException(fname, e);
            }
            okPieces.add(fname);
        }
        System.out.println("--- SUMMARY ---");
        okPieces.forEach(s -> {
            System.out.println("SUCCESS " + s);
        });
    }

    private static void runMainFile() throws Exception {
        InputStream streamMath = Main.class.getClassLoader().getResourceAsStream("math.txt");
        InputStream streamMathOverwrite = Main.class.getClassLoader().getResourceAsStream("mathOverwrite.txt");
        if (streamMathOverwrite != null && streamMathOverwrite.available() < 1) {
            streamMathOverwrite = null;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(
                streamMathOverwrite == null ? streamMath : streamMathOverwrite));
        runMath(br);
    }

    public static void runMath(BufferedReader br) throws Exception{

        Parser parser = new Parser();
        List<Rule> rulesAndQuests = parser.parseMathDoc(br);
        System.out.println("===========================================");

        List<Rule> rules = new ArrayList<>();

        long countFocus = rulesAndQuests.stream()
                .filter(r -> (r instanceof QuestRule) && ((QuestRule)r).focus).count();
        if( countFocus>1 ){
            throw new IllegalStateException();
        }

//        List<Rule> rules = rulesAndQuests.stream()
//                .filter(r -> !(r instanceof QuestRule))
//                .collect(Collectors.toCollection(ArrayList::new));

        //System.out.println("Rules="+rules);
        for( Rule r : rulesAndQuests ){
            if( r instanceof QuestRule ){
                QuestRule qrule = (QuestRule) r;
                if( countFocus==1 && ! qrule.focus ){
                    System.out.println("skipped non-focus quest rule");
                }else {
                    Calc calc = new Calc(rules, qrule.localConditionsAsRules());
                    Expr ret = calc.quest(r.assertion,
                            e -> qrule.answer != null && e.toLispString().equals(qrule.answer.toLispString()),
                            1000);
                    if (qrule.answer == null) {
                        System.out.println("Correct answer was not specified!");
                    } else if (!ret.toLispString().equals(qrule.answer.toLispString())) {
                        throw new IllegalStateException("Not reached answer=" + qrule.answer + "\nsrcLines=" + qrule.srcLines);
                    }
                }

                if( qrule.answer!=null && qrule.reusable ){
                    Rule provenRule = new Rule(new Expr("=", qrule.assertion, qrule.answer), qrule.cond, null);
                    rules.add(provenRule);
                }
            }else{
                rules.add(r); // add a new given rule to the list of all known Math
            }
        }
        System.out.println("SUCCESS");
    }

    static void testUnify(){
        Parser parser = new Parser();
        chkUnify(parser, "x", "x + 1", "{x=(+ x 1)}");
        chkUnify(parser, "x + 1", "x", "null");
        chkUnify(parser, "x + 5", "7 + 5", "{x=7}");
        chkUnify(parser, "x + 1 = 5", "4 + 1 = 5", "{x=4}");
        chkUnify(parser,"f ( x ) = x ^ 2 + h", "f ( x ) = x ^ 2 + 22", "{f=f, x=x, h=22}");
        chkUnify(parser,"f ( x ) = g ( x ) + h ( x )", "f ( x ) = x ^ 2 + 1", "{f=f, g=(func x (^ x 2)), x=x, h=(func x 1)}");
    }

    static void testParseLine(){
        Parser parser = new Parser();
        chk(parser, "3-(2+1)", "(+ 3 (- (+ 2 1)))");
        chk(parser, "g( f(x+1) )", "(apply g (apply f (+ x 1)))");

        chk(parser, "- ( c ^ 2 )", "(- (^ c 2))");
        chk(parser, "3 + 2 + 1", "(+ (+ 3 2) 1)");
        chk(parser, "3 - 2 + 1", "(+ (+ 3 (- 2)) 1)");
        chk(parser, "3 / 2 * 1", "(* (* 3 (/ 2)) 1)");
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
        chk(parser, "x ↦ 1", "(func x 1)");
        chk(parser, "( ∂ ff ) ( x )", "(apply (apply ∂ ff) x)");
        chk(parser, "( ∂ ( x ↦ 1 ) ) ( x )", "(apply (apply ∂ (func x 1)) x)");
    }

    static void chkUnify(Parser parser, String template, String concrete, String resMap){
        String res = "" + parser.parse(template).unify(parser.parse(concrete));
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
