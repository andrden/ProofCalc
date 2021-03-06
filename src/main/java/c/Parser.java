package c;

import c.model.Expr;
import c.model.Rule;
import c.model.Type;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by denny on 8/6/15.
 */
@SuppressWarnings("unchecked")
public class Parser {
    static List<String> splitLine(String line){
        line = line.replace(","," , ");
        line = line.replace(":"," : ");
        line = line.replace("+"," + ");
        line = line.replace("ℝ +","ℝ+");
        line = line.replace("-"," - ");
        line = line.replace("("," ( ");
        line = line.replace(")"," ) ");
        line = line.replace("∀"," ∀ ");
        line = line.replace("∃"," ∃ ");

        StringTokenizer st = new StringTokenizer(line, " \t");
        List<String> ret = new ArrayList<>();
        while(st.hasMoreTokens()){
            ret.add(st.nextToken());
        }
        return ret;
    }

    List<Rule> parseMathDoc(BufferedReader br) throws IOException{
        String line;
        List<String> lines = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();

        while( (line=br.readLine())!=null ){
            if(StringUtils.isBlank(line)) {
                if (!lines.isEmpty()) {
                    Rule rule = ruleFromLines(lines);
                    checkDistinct(rule, rules);
                    rules.add(rule);
                    lines.clear();
                }
            } else if ( line.startsWith("#" ) ){
                // comment line, skip
            }else{
                lines.add(line);
            }
            //System.out.println(line);
        }
        if( ! lines.isEmpty() ) {
            Rule rule = ruleFromLines(lines);
            checkDistinct(rule, rules);
            rules.add(rule);
        }
        return rules;
    }

    void checkDistinct(Rule newRule, List<Rule> rules){
        for( Rule r : rules ){
            if( newRule.getSrcLines().equals(r.getSrcLines()) ){
                throw new RuntimeException("duplicate "+newRule);
            }
        }
    }

    Rule ruleFromLines(List<String> lines){
        List<Expr> cond = new ArrayList<>();
        Expr answer = null;
        for( String l : lines ) {
            LinkedList<String> line = new LinkedList<>(splitLine(l));
            if (line.get(0).equals("$=")) {
                line.remove(0);
                answer = parse(line);
            }
        }
        String ruleName = null;
        for( String l : lines ){
            LinkedList<String> line = new LinkedList<>(splitLine(l));
            if( line.get(0).equals("$name") ){
                ruleName = line.get(1);
            }else if( line.get(0).equals("$e") ){
                line.remove(0);
                Expr e = parse(line);
                cond.add(e);
            } else if( line.get(0).equals("$a") ){
                line.remove(0);
                Expr assertion = parse(line);
                if( answer!=null ){
                    throw new IllegalStateException();
                }
                Rule rule = new Rule(assertion, cond, new ArrayList<>(lines));
                rule.setName(ruleName);
                return rule;
            } else if( line.get(0).equals("$?")
                    || line.get(0).equals("$a?")
                    || line.get(0).equals("$a?focus")
                    || line.get(0).equals("$?focus") ){
                boolean reusable = true;//line.get(0).equals("$a?") || line.get(0).equals("$a?focus");
                boolean focus = line.get(0).equals("$?focus") || line.get(0).equals("$a?focus");

                line.remove(0);
                Expr assertion = parse(line);
                QuestRule qr = new QuestRule(assertion, cond, answer, new ArrayList<>(lines));
                qr.setName(ruleName);
                qr.focus = focus;
                if( reusable ){
                    qr.reusable = true;
                }
                if( answer==null ){
                    System.out.println("No answer given for: "+qr);
                }
                return qr;
            }
        }
        throw new IllegalArgumentException(""+lines);
    }

    Expr parse(String line) {
        return parse(new LinkedList<>(splitLine(line)));
    }


    Expr parse(LinkedList<String> line){
        ArrayList lineCopy = new ArrayList<>(line);
        parseAtoms(lineCopy);
        return parseByBrackets(new ArrayList<>(lineCopy));
    }

    Expr parseByBrackets(List line){
        for( int i=0; i<line.size(); i++ ){
            if( "(".equals(line.get(i)) ){
                boolean parsed=false;
                int depth=1;
                for( int j=i+1; j<line.size(); j++ ){
                    if( "(".equals(line.get(j)) ) depth++;
                    if( ")".equals(line.get(j)) ) depth--;
                    if( depth==0 ){
                        List before = line.subList(0, i);
                        List after = line.subList(j+1, line.size());
                        Expr inBrackets = parseByBrackets(line.subList(i + 1, j));
                        line = new ArrayList<>();
                        line.addAll(before);
                        line.add(inBrackets);
                        line.addAll(after);
                        parsed = true;
                        break;
                    }
                }
                if( ! parsed ){
                    throw new RuntimeException("i="+i+" line="+line);
                }
            }
        }

        return parseByOps(line);
    }

    Expr parseByOps(List line){
        List list = new ArrayList(line);
        while( parseApply(list) );
        if( list.get(0).equals("∀") || list.get(0).equals("∃") ){
            if( ! list.get(2).equals("∈") ){
                throw new IllegalStateException();
            }
            Expr var = (Expr) (list.get(1));
            Expr set = (Expr) (list.get(3));
            int s = 4;
            if( list.get(s).equals(":") ){
                // skip colon after quantifier
                // colon can always be used but sometimes is optional
                s ++;
            }
            Expr right = parseByOps(list.subList(s, list.size()));
            return new Expr((String)list.get(0), var, set, right);
        }
        while( infixOp(list, "^") );
        unaryMinus(list, "*", "/");
        while( infixOp(list, "*") );
        unaryMinus(list, "+", "-");
        while( infixOp(list, "+") );
        unaryPlus(list);
        while( infixOp(list, ",") );
        while( infixOp(list, "=","<",">","≥","≤","↦") );
        prefixOp(list, "real");
        while( infixOp(list, "∈") );
        if(list.size()==1 && list.get(0) instanceof Expr) {
            return (Expr) list.get(0);
        }
        throw new RuntimeException("parseByOps: "+list);
    }

    void parseAtoms(List list){
        for( int i = 0; i<list.size(); i++ ) {
            String s = (String)list.get(i);
            if( (s.charAt(0)>='0' && s.charAt(0)<='9') || Type.isVarOrConst(s) ){
                list.set(i, new Expr(s));
            }
        }
    }

    boolean parseApply(List list){
        // fff xxx => apply(fff,xxx)
        for( int i=list.size()-1; i>=1; i-- ){ // associative right to left
            if( list.get(i) instanceof Expr && list.get(i-1) instanceof Expr ){
                Expr apply = new Expr("apply", (Expr)list.get(i-1), (Expr)list.get(i));
                Expr args = apply.rightChild();
                if( args.node.equals(",") ){
                    // function of more than 1 argument, flattening
                    apply = new Expr("apply", apply.child(0), args.child(0), args.child(1));
                }
                list.set(i-1, apply);
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    boolean prefixOp(List list, String ...  ops){
        for( int i = 0; i<list.size()-1; i++ ){
            Object op = list.get(i);
            if( Arrays.asList(ops).contains(list.get(i))) {
                Object right = list.get(i+1);
                if( right instanceof Expr ) {
                    Expr comb = new Expr((String) op, (Expr)right);
                    list.set(i, comb);
                    list.remove(i + 1);
                    return true;
                }
            }
        }
        return false;
    }
    void unaryMinus(List list, String rolePlus, String roleMinus){
        for( int i = 0; i<list.size(); i++ ){
            Object op = list.get(i);
            if( roleMinus.equals(op) ){
                list.set(i, rolePlus);
                list.set(i+1, new Expr(roleMinus, (Expr)list.get(i+1)));
            }
        }
    }

    void unaryPlus(List list){
        for( int i = 0; i<list.size(); i++ ){
            Object op = list.get(i);
            if( "+".equals(op) ){
                list.remove(i);
            }
        }
    }


    boolean infixOp(List list, String ...  ops){
        for( int i = 1; i<list.size()-1; i++ ){
            Object op = list.get(i);
            if( Arrays.asList(ops).contains(list.get(i))) {
                Object left = list.get(i-1);
                Object right = list.get(i+1);
                if( left instanceof Expr && right instanceof Expr ) {
                    String exprOp = (String) op;
                    if( exprOp.equals("↦") ){
                        exprOp = "func"; // better readability
                    }
                    Expr comb = new Expr(exprOp, (Expr)left, (Expr)right);
                    list.set(i, comb);
                    list.remove(i + 1);
                    list.remove(i - 1);
                    return true;
                }
            }
        }
        return false;
    }

}
