package tests;


public class arith extends TCBase {

    static String testspecpath = "$A"+z+"$B"+z+"specs/specs16";

    @Override
    public void setUp() throws Exception {
//        noCollectDiagnostics = true;
//        jmldebug = true;
        super.setUp();
    }
    
    /** See the FIXME in BigInteger.spec */
    public void testSomeJava() {
        options.put("-specs",   testspecpath);
        helpTCF("A.java","public class A { java.math.BigInteger list; }");
    }

}