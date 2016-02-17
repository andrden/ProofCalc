package c;

import c.calc.Calc;
import c.model.Expr;
import c.model.Normalizer;
import c.model.Rule;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by denny on 2/2/16.
 */
public class MainSupport {
    static void runPiece(String pieceFile) throws Exception{
        runMath(new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream("tests/"+pieceFile))));
    }

    static void runPieces() throws Exception{
        List<String> okPieces = new ArrayList<>();
        for( int p=1; ; p++ ) {
            String fname = "piece" + p + ".txt";
            InputStream streamMath = Main.class.getClassLoader().getResourceAsStream("tests/"+fname);
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

    static void runMainFile() throws Exception {
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
            final long t0 = System.currentTimeMillis();
            if( r instanceof QuestRule ){
                QuestRule qrule = (QuestRule) r;
                if( countFocus==1 && ! qrule.focus ){
                    System.out.println("skipped non-focus quest rule");
                }else {
                    Calc calc = new Calc(rules, qrule.localConditionsAsRules());
                    Expr qruleAnswer = qrule.answer==null ? null : Normalizer.normalize(qrule.answer);
                    Expr ret = calc.quest(r.assertion,
                            e -> qruleAnswer != null && e.toLispString().equals(qruleAnswer.toLispString()),
                            150);
                    if (qruleAnswer == null) {
                        System.out.println("Correct answer was not specified!");
                    } else if (!ret.toLispString().equals(qruleAnswer.toLispString())) {
                        throw new IllegalStateException("Not reached answer=" + qrule.answer + "\nsrcLines=" + qrule.getSrcLines());
                    }
                    long sec = (System.currentTimeMillis()-t0)/1000;
                    String secS = sec==0 ? "" : " sec="+sec;
                    System.out.println("FINISHED" + secS + " " + r);
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
}
