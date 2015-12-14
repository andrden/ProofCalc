package c;

import c.model.Expr;

/**
 * Created by denny on 10/15/15.
 */
public class Tests {
    static void allTests(){
        Tests.testParseLine();
        Tests.testUnify();

        Parser parser = new Parser();
        Expr e = parser.parse("(x ↦ ( (x ↦ x ^ 3) ((x ↦ 1 + x) (x)) ) )");
        assertEq("(func x (^ (+ 1 x) 3))", e.simplifyApplyFunc().toLispString());
        assertTrue(parser.parse("x ↦ (1 + x)").equals(parser.parse("y ↦ (1 + y)")));
    }

    static void testUnify(){
        Parser parser = new Parser();
/*
        Actually our unification is not pure abstract,
        but has knowledge of functions.
        For example, (^ x 2) will be unified with (apply g x)
        and functional construct value for g will be produced,
        though the expressions are not similar on the first sight
*/

        chkUnify(parser, "x ↦ cos(g(x))", "x ↦ cos(x)", "{g=(func x x)}");
        chkUnify(parser, "x ↦ cos(g(x))", "cos", "{g=(func x x)}");
        chkUnify(parser, "x ↦ h(x)", "x ↦ cos(sin(x))", "{h=(func x (apply cos (apply sin x)))}");
        chkUnify(parser, "x ↦ g( h(x) )", "x ↦ sin(cos(sin(x)))", "{h=(func x (apply cos (apply sin x))), g=sin}");
        chkUnify(parser, "x ↦ g( h(x) )", "x ↦ 1 - sin(sin(x)) * sin(sin(x))",
                "{h=(func x (- (* (apply sin (apply sin x)) (apply sin (apply sin x))))), g=(func x (+ 1 x))}");
        chkUnify(parser, "x ↦ g( h(x) )", "x ↦ cos(sin(x))", "{h=sin, g=cos}");

        chkUnify(parser, "( ∂ ( x ↦ x ) )(x)", "( ∂ ( x ↦ x + sin(x) ) )(x)", "null");
        chkUnify(parser, "y ↦ g(y)", "x ↦ x", "{g=(func x x)}");
        chkUnify(parser, "y ↦ (g(y) + h(y))", "x ↦ (x + sin(x))", "{g=(func x x), h=sin}");

        chkUnify(parser, "x", "x + 1", "{x=(+ x 1)}");
        chkUnify(parser, "x + 1", "x", "null");
        chkUnify(parser, "x + 5", "7 + 5", "{x=7}");
        chkUnify(parser, "x + 1 = 5", "4 + 1 = 5", "{x=4}");
        chkUnify(parser, "f ( x ) = x ^ 2 + h", "f ( x ) = x ^ 2 + 22", "{f=f, x=x, h=22}");
        chkUnify(parser, "f ( x ) = g ( x ) + h ( x )", "f ( x ) = x ^ 2 + 1", "{f=f, g=(func x (^ x 2)), h=(func x 1)}");

        chkUnify(parser, "( ∂ ( x ↦ g( h(x) ) ) )(x)", "( ∂ ( x ↦ sin(x ^ 3) ) )(x)", "{h=(func x (^ x 3)), g=sin, x=x}");
        chkUnify(parser, "( ∂ ( x ↦ g( h(x) ) ) )(x)", "( ∂ ( x ↦ (sin(x)) ^ 3 ) )(x)", "{h=sin, g=(func x (^ x 3)), x=x}");

        chkUnify(parser, "( ∂ ( x ↦ x ^ n ) )(x)", "( ∂ ( x ↦ x ^ 4 ) )(x)", "{n=4, x=x}");
        chkUnify(parser, "( ∂ ( x ↦ x ^ n ) )(x)", "( ∂ ( x ↦ x ^ 4 ) )(x + 1)", "{n=4, x=(+ x 1)}"); // (func x ...) inner var x
        chkUnify(parser, "( ∂ ( x ↦ x ^ n ) )(x)", "( ∂ ( x ↦ x ^ 4 ) )(y + 1)", "{n=4, x=(+ y 1)}"); // (func x ...) inner var x
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

    static void chkUnify(Parser parser, String template, String concrete, String resMap){
        String res = "" + parser.parse(template).unify(parser.parse(concrete));
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
