package c;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by denny on 8/6/15.
 */
public class Parser {
    Parser(BufferedReader br) throws IOException{
        String line;
        while( (line=br.readLine())!=null ){
            System.out.println(line);
        }
    }
}
