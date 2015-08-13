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
    List<Rule> parseMathDoc(BufferedReader br) throws IOException{
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
        return rules;
    }

    Rule ruleFromLines(List<String> lines){
        List<Expr> cond = new ArrayList<>();
        for( String l : lines ){
            LinkedList<String> line = new LinkedList<>(Util.splitLine(l));
            if( line.get(0).equals("$e") ){
                line.remove(0);
                Expr e = parse(line);
                cond.add(e);
            } else if( line.get(0).equals("$a") ){
                line.remove(0);
                Expr assertion = parse(line);
                return new Rule(false, assertion, cond);
            } else if( line.get(0).equals("$?") ){
                line.remove(0);
                Expr assertion = parse(line);
                return new Rule(true, assertion, cond);
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
        parseAtoms(list);
        parseBracketsAndApply(list);
        while( infixOp(list, "*") );
        while( infixOp(list, "+", "-") );
        while( infixOp(list, "=") );
        prefixOp(list, "real");
        if(list.size()==1 && list.get(0) instanceof Expr) {
            return (Expr) list.get(0);
        }
        throw new RuntimeException(""+list);
    }

    void parseAtoms(List list){
        for( int i = 0; i<list.size(); i++ ) {
            String s = (String)list.get(i);
            if( (s.charAt(0)>='0' && s.charAt(0)<='9') || Type.isVar(s) ){
                list.set(i, new Expr(s));
            }
        }
    }

    void parseBracketsAndApply(List list){
        boolean change;
        do {
            change = false;
            while (parseBrackets(list)) change = true;
            while (parseApply(list)) change = true;
        }while(change);
    }

    boolean parseBrackets(List list){
        // ( vvv ) => vvv
        for( int i = 1; i<list.size()-1; i++ ) {
            if( list.get(i) instanceof Expr && "(".equals(list.get(i-1)) && ")".equals(list.get(i+1)) ){
                list.remove(i+1);
                list.remove(i-1);
                return true;
            }
        }
        return false;
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
                    parseBracketsAndApply(list);
                    return true;
                }
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
/*
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
    */
}
