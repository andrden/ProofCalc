package c;

import static c.MainSupport.*;

/**
 * Created by denny on 8/6/15.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        //new Date(1443657600000L).toString();
        Tests.allTests();

        MainSupport.runMainFile();
        //MainSupport.runPieces();
        //MainSupport.runPiece("piece17.txt");

    }

}
