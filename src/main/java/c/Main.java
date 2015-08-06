package c;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by denny on 8/6/15.
 */
public class Main {
    public static void main(String[] args) throws Exception{
        BufferedReader br =
                new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream("math.txt")));
	new Parser(br);
    }
}
