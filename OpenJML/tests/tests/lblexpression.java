package tests;

public class lblexpression extends TCBase {


    @Override
    public void setUp() throws Exception {
//        noCollectDiagnostics = true;
//        jmldebug = true;
        super.setUp();
    }
    
    public void testlbl() {
        helpTCF("A.java",
                " class A { int k;  \n" +
                "   //@ invariant (\\lblneg A false);\n" +
                "   //@ invariant (\\lblpos A k);\n" +
                "   void m(double k) {}\n" +
                "}",
        "/A.java:3: incompatible types\nfound   : int\nrequired: boolean",19
        );
    }

    public void testlbl2() {
        helpTCF("A.java",
                " class A { int k;  \n" +
                "   //@ invariant \\lblneg A false;\n" +  // This is not strict JML, but it is difficult to preclude
                "   //@ invariant 0==(\\lblpos A -k);\n" +
                "   void m(double k) {}\n" +
                "}"
        );
    }

    public void testlbl3() {
        helpTCF("A.java",
                " class A { int k;  \n" +
                "   //@ invariant \\lblneg ghost false;\n" +  // This is not strict JML, but it is difficult to preclude
                "   //@ invariant 0==(\\lblpos pure -k);\n" +
                "   void m(double k) {}\n" +
                "}"
        );
    }

}
