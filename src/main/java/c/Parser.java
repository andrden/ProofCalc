package c;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by denny on 8/6/15.
 */
public class Parser {
    Parser(BufferedReader br) throws IOException{
        String line;
        List<String> lines = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();

        while( (line=br.readLine())!=null ){
            if(StringUtils.isBlank(line)){
                if( ! lines.isEmpty() ){
                    rules.add(new Rule(lines));
                    lines.clear();
                }
            }else{
                lines.add(line);
            }
            System.out.println(line);
        }
        if( ! lines.isEmpty() ) {
            rules.add(new Rule(lines));
        }
        System.out.println("Rules="+rules);
    }
}
