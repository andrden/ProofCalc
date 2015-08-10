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
public class Parser {
    void parseMathDoc(BufferedReader br) throws IOException{
        String line;
        List<String> lines = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();

        while( (line=br.readLine())!=null ){
            if(StringUtils.isBlank(line)){
                if( ! lines.isEmpty() ){
                    rules.add(ruleFromLines(lines));
                    lines.clear();
                }
            }else{
                lines.add(line);
            }
            System.out.println(line);
        }
        if( ! lines.isEmpty() ) {
            rules.add(ruleFromLines(lines));
        }
        System.out.println("Rules="+rules);
    }

    Rule ruleFromLines(List<String> lines){
        for( String l : lines ){
            LinkedList<String> line = new LinkedList<>(Util.splitLine(l));
            if( line.get(0).equals("$e") ){
                //cond.add(line.subList(1, line.size()));
            } else if( line.get(0).equals("$a") ){
                line.remove(0);
                Expr assertion = parse(line);
                return new Rule(assertion);
            }
        }
        return null;
    }

    List<String> ops = Arrays.asList("*","+","=");

    Expr parse(LinkedList<String> line){
        //return parseSequential(line);
        return parseByOps(line);
    }

    Expr parseByOps(LinkedList<String> line){
        List list = new ArrayList(line);
        while( infixOp(list, "*") );
        while( infixOp(list, "+","-") );
        return (Expr)list.get(0);
    }

    boolean infixOp(List list, String ...  ops){
        for( int i = 1; i<list.size()-1; i++ ){
            Object op = list.get(i);
            if( Arrays.asList(ops).contains(list.get(i))) {
                Expr comb = new Expr((String)op, wrap(list.get(i-1)), wrap(list.get(i+1)));
                list.set(i, comb);
                list.remove(i+1);
                list.remove(i-1);
                return true;
            }
        }
        return false;
    }

    Expr wrap(Object o){
        if( o instanceof Expr ){
            return (Expr)o;
        }
        return new Expr((String)o);
    }

    private Expr parseSequential(LinkedList<String> line) {
        Expr arg=null;
        while( ! line.isEmpty() ){
            String el = line.remove(0);
            if( line.isEmpty() ){
                return new Expr(el);
            }
            if( arg==null ){
                arg = new Expr(el);
            }else if( ops.contains(el) ){
                return new Expr(el, arg, parse(line));
            }
        }
        return null;
    }
}
