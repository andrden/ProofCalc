package c;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by denny on 8/6/15.
 */
@SuppressWarnings("unchecked")
public class Parser {
    List<Rule> parseMathDoc(BufferedReader br) throws IOException{
        String line;
        List<String> lines = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();

        while( (line=br.readLine())!=null ){
            if(StringUtils.isBlank(line)) {
                if (!lines.isEmpty()) {
                    rules.add(ruleFromLines(lines));
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
            rules.add(ruleFromLines(lines));
        }
        return rules;
    }

    Rule ruleFromLines(List<String> lines){
        List<Expr> cond = new ArrayList<>();
        Expr answer = null;
        for( String l : lines ) {
            LinkedList<String> line = new LinkedList<>(Util.splitLine(l));
            if (line.get(0).equals("$=")) {
                line.remove(0);
                answer = parse(line);
            }
        }
        for( String l : lines ){
            LinkedList<String> line = new LinkedList<>(Util.splitLine(l));
            if( line.get(0).equals("$e") ){
                line.remove(0);
                Expr e = parse(line);
                cond.add(e);
            } else if( line.get(0).equals("$a") ){
                line.remove(0);
                Expr assertion = parse(line);
                if( answer!=null ){
                    throw new IllegalStateException();
                }
                return new Rule(assertion, cond, new ArrayList<>(lines));
            } else if( line.get(0).equals("$?") || line.get(0).equals("$a?") ){
                boolean reusable = line.get(0).equals("$a?");
                line.remove(0);
                Expr assertion = parse(line);
                QuestRule qr = new QuestRule(assertion, cond, answer, new ArrayList<>(lines));
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
        return parse(new LinkedList<>(Util.splitLine(line)));
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

    Expr parseByOps(List<String> line){
        List list = new ArrayList(line);
        while( parseApply(list) );
        while( infixOp(list, "^") );
        unaryMinus(list, "*", "/");
        while( infixOp(list, "*") );
        unaryMinus(list, "+", "-");
        while( infixOp(list, "+") );
        unaryPlus(list);
        while( infixOp(list, "=","≥","≤") );
        prefixOp(list, "real");
        if(list.size()==1 && list.get(0) instanceof Expr) {
            return (Expr) list.get(0);
        }
        throw new RuntimeException(""+list);
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
                    Expr comb = new Expr((String) op, (Expr)left, (Expr)right);
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
