package c;

import c.model.Expr;
import c.model.Normalizer;

/**
 * Created by denny on 10/15/15.
 */
public class Tests {
    /*
            Actually our unification is not pure abstract,
            but has knowledge of functions.
            For example, (^ x 2) will be unified with (apply g x)
            and functional construct value for g will be produced,
            though the expressions are not similar on the first sight
    */

    static void allTests(){
        Tests.testParseLine();
        Tests.testUnify();
        Tests.testUnifyMany();

        Parser parser = new Parser();
        Expr e = parser.parse("(x ↦ ( (x ↦ x ^ 3) ((x ↦ 1 + x) (x)) ) )");
        assertEq("(func x (^ (+ 1 x) 3))", e.simplifyApplyFunc().toLispString());
        assertTrue(parser.parse("x ↦ (1 + x)").equals(parser.parse("y ↦ (1 + y)")));
    }

    static void testUnifyMany() {
        Parser parser = new Parser();

        chkUnifyMany(parser, "x ↦ g(x) * h(x)", "y ↦ cos(y) * cos(y) * 2",
                "[{g=(func y (* (apply cos y) (apply cos y))), h=(func y 2)}, {g=(func y (* (apply cos y) 2)), h=cos}]");
        chkUnifyMany(parser, "g + h", "cos(y) + cos(y) + 2",
                "[{g=(+ (apply cos y) (apply cos y)), h=2}, {g=(+ (apply cos y) 2), h=(apply cos y)}]");
        chkUnifyMany(parser, "g * h", "cos(y) * cos(y) * 2",
                "[{g=(* (apply cos y) (apply cos y)), h=2}, {g=(* (apply cos y) 2), h=(apply cos y)}]");
        chkUnifyMany(parser, "(x + y) * z", "(exp(x) + exp(x) + 2) / 4",
                "[{x=(+ (apply exp x) 2), y=(apply exp x), z=(/ 4)}, {x=(+ (apply exp x) (apply exp x)), y=2, z=(/ 4)}]");
        chkUnifyMany(parser, "(x + y) * z", "((1 / exp(x)) ^ 2 + exp(x) ^ 2 + 2) / 4",
                "[{x=(+ (^ (apply exp x) 2) 2), y=(^ (/ (apply exp x)) 2), z=(/ 4)}, {x=(+ (^ (/ (apply exp x)) 2) (^ (apply exp x) 2)), y=2, z=(/ 4)}, {x=(+ (^ (/ (apply exp x)) 2) 2), y=(^ (apply exp x) 2), z=(/ 4)}]");
    }

    static class UTest{
        String template;
        String concrete;
        String resMap;

        UTest(String template, String concrete, String resMap) {
            this.template = template;
            this.concrete = concrete;
            this.resMap = resMap;
        }
        String chkUnify(Parser parser){
            try{
                String res = "" + parser.parse(template).unify(parser.parse(concrete));
                assertEq(resMap, res);
                return null;
            }catch (Exception e){
                e.printStackTrace();
                return template + " => " + concrete + "  " + e;
            }
        }
    }

    static void testUnify(){

        UTest[] rarr = {
                new UTest("( ∂ f ) ( x )", "(∂ ( x ↦ 1 )) ( x )", "{f=(func x 1), x=x}"),
                new UTest("x ↦ g( h(x) )", "x ↦ cos(sin(x))", "{h=sin, g=cos}"),
                new UTest( "x", "x + 1", "{x=(+ x 1)}"),
                new UTest( "x ↦ cos(g(x))", "x ↦ cos(x)", "{g=(func x x)}"),
                new UTest( "x ↦ cos(g(x))", "cos", "{g=(func x x)}"),
                new UTest( "x ↦ h(x)", "x ↦ cos(sin(x))", "{h=(func x (apply cos (apply sin x)))}"),
                new UTest( "x ↦ g( h(x) )", "x ↦ sin(cos(sin(x)))", "{h=(func x (apply cos (apply sin x))), g=sin}"),
                new UTest( "x ↦ g( h(x) )", "x ↦ 1 - sin(sin(x)) * sin(sin(x))",
                    "{h=(func x (- (* (apply sin (apply sin x)) (apply sin (apply sin x))))), g=(func x (+ 1 x))}"),

                new UTest( "( ∂ ( x ↦ x ) )(x)", "( ∂ ( x ↦ x + sin(x) ) )(x)", "null"),
                new UTest( "y ↦ g(y)", "x ↦ x", "{g=(func x x)}"),
                new UTest( "y ↦ (g(y) + h(y))", "x ↦ (x + sin(x))", "{g=(func x x), h=sin}"),

                new UTest( "x + 1", "x", "null"),
                new UTest( "x + 5", "7 + 5", "{x=7}"),
                new UTest( "x + 1 = 5", "4 + 1 = 5", "{x=4}"),
                new UTest( "f ( x ) = x ^ 2 + h", "f ( x ) = x ^ 2 + 22", "{f=f, x=x, h=22}"),
                new UTest( "f ( x ) = g ( x ) + h ( x )", "f ( x ) = x ^ 2 + 1", "{f=f, g=(func x (^ x 2)), h=(func x 1)}"),

                new UTest( "( ∂ ( x ↦ g( h(x) ) ) )(x)", "( ∂ ( x ↦ sin(x ^ 3) ) )(x)", "{h=(func x (^ x 3)), g=sin, x=x}"),
                new UTest( "( ∂ ( x ↦ g( h(x) ) ) )(x)", "( ∂ ( x ↦ (sin(x)) ^ 3 ) )(x)", "{h=sin, g=(func x (^ x 3)), x=x}"),

                new UTest( "( ∂ ( x ↦ x ^ n ) )(x)", "( ∂ ( x ↦ x ^ 4 ) )(x)", "{n=4, x=x}"),
                new UTest( "( ∂ ( x ↦ x ^ n ) )(x)", "( ∂ ( x ↦ x ^ 4 ) )(x + 1)", "{n=4, x=(+ x 1)}"), // (func x ...) inner var x
                new UTest( "( ∂ ( x ↦ x ^ n ) )(x)", "( ∂ ( x ↦ x ^ 4 ) )(y + 1)", "{n=4, x=(+ y 1)}"), // (func x ...) inner var x
        };

        Parser parser = new Parser();
        StringBuilder sb = new StringBuilder();
        int errs=0;
        for( UTest r : rarr ){
           String s = r.chkUnify(parser);
           if( s!=null ){
               errs++;
               sb.append(s+"\n");
           }
        }
        if( sb.length() > 0 ){
            System.err.println(sb);
            throw new RuntimeException("errs="+errs+" out of " + rarr.length + " tests");
        }
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

    static void chkUnifyMany(Parser parser, String template, String concrete, String resMap){
        Expr t = Normalizer.normalize(parser.parse(template));
        Expr c = Normalizer.normalize(parser.parse(concrete));
        String res = "" + t.unifyOptions(c);
        assertEq(resMap, res);
    }

    static void assertTrue(boolean res) {
        if( ! res ){
            throw new RuntimeException();
        }
    }

    static void assertEq(String resMap, String res) {
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
