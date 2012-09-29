package org.jmlspecs.openjml.esc;


import static com.sun.tools.javac.code.TypeTags.CLASS;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.tools.JavaFileObject;

import org.jmlspecs.annotation.NonNull;
import org.jmlspecs.openjml.DiagnosticPositionSES;
import org.jmlspecs.openjml.JmlPretty;
import org.jmlspecs.openjml.JmlSpecs;
import org.jmlspecs.openjml.JmlToken;
import org.jmlspecs.openjml.JmlTree;
import org.jmlspecs.openjml.JmlTree.JmlBinary;
import org.jmlspecs.openjml.JmlTree.JmlChoose;
import org.jmlspecs.openjml.JmlTree.JmlClassDecl;
import org.jmlspecs.openjml.JmlTree.JmlCompilationUnit;
import org.jmlspecs.openjml.JmlTree.JmlConstraintMethodSig;
import org.jmlspecs.openjml.JmlTree.JmlDoWhileLoop;
import org.jmlspecs.openjml.JmlTree.JmlEnhancedForLoop;
import org.jmlspecs.openjml.JmlTree.JmlForLoop;
import org.jmlspecs.openjml.JmlTree.JmlGroupName;
import org.jmlspecs.openjml.JmlTree.JmlImport;
import org.jmlspecs.openjml.JmlTree.JmlLblExpression;
import org.jmlspecs.openjml.JmlTree.JmlMethodClause;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseCallable;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseConditional;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseDecl;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseExpr;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseGroup;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseSignals;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseSignalsOnly;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseStoreRef;
import org.jmlspecs.openjml.JmlTree.JmlMethodDecl;
import org.jmlspecs.openjml.JmlTree.JmlMethodInvocation;
import org.jmlspecs.openjml.JmlTree.JmlMethodSpecs;
import org.jmlspecs.openjml.JmlTree.JmlModelProgramStatement;
import org.jmlspecs.openjml.JmlTree.JmlPrimitiveTypeTree;
import org.jmlspecs.openjml.JmlTree.JmlQuantifiedExpr;
import org.jmlspecs.openjml.JmlTree.JmlSetComprehension;
import org.jmlspecs.openjml.JmlTree.JmlSingleton;
import org.jmlspecs.openjml.JmlTree.JmlSpecificationCase;
import org.jmlspecs.openjml.JmlTree.JmlStatement;
import org.jmlspecs.openjml.JmlTree.JmlStatementDecls;
import org.jmlspecs.openjml.JmlTree.JmlStatementExpr;
import org.jmlspecs.openjml.JmlTree.JmlStatementLoop;
import org.jmlspecs.openjml.JmlTree.JmlStatementSpec;
import org.jmlspecs.openjml.JmlTree.JmlStoreRefArrayRange;
import org.jmlspecs.openjml.JmlTree.JmlStoreRefKeyword;
import org.jmlspecs.openjml.JmlTree.JmlStoreRefListExpression;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseConditional;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseConstraint;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseDecl;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseExpr;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseIn;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseInitializer;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseMaps;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseMonitorsFor;
import org.jmlspecs.openjml.JmlTree.JmlTypeClauseRepresents;
import org.jmlspecs.openjml.JmlTree.JmlVariableDecl;
import org.jmlspecs.openjml.JmlTree.JmlWhileLoop;
import org.jmlspecs.openjml.JmlTreeScanner;
import org.jmlspecs.openjml.JmlTreeUtils;
import org.jmlspecs.openjml.Utils;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.JmlAttr;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCSkip;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;

/** This method translates an attributed Java+JML AST, creating a new Java-compatible AST
 * that includes assertions to check for all the various JML conditions that need checking.
 * The resulting AST is a complete copy - it does not share any structure with the original
 * AST, so the original AST can be reused. It also represents each identifier in a separate
 * JCIdent, so that the succeeding Single-assignment step can change identifier names in place.
 *
 */
// FIXME - should we use the copier instead??
public class JmlAssertionAdder extends JmlTreeScanner {
    
    // FIXME - all fields need documentation
    
    final protected Context context;
    final protected Log log;
    final protected JmlSpecs specs;
    final protected JmlTree.Maker M;
    final protected Names names;
    final protected Symtab syms;
    final protected Types types;
    
    /** The JmlTreeUtils object, holding a bunch of tree-making utilities */
    final protected JmlTreeUtils treeutils;
    
    final protected JmlExpressionRewriter jmlrewriter = new JmlExpressionRewriter();
    
    public Map<String,DiagnosticPositionSES> positionMap = new HashMap<String,DiagnosticPositionSES>();
    
    protected JCMethodDecl methodDecl = null;
    
    int precount = 0;
    final String prePrefix = "Pre_";
    final String assertPrefix = "ASSERT_";
    protected int assertCount = 0;
    
    protected Map<Symbol,JCVariableDecl> preparams = new HashMap<Symbol,JCVariableDecl>();
    protected Map<JmlSpecificationCase,JCIdent> preconditions = new HashMap<JmlSpecificationCase,JCIdent>();
    

    final public java.util.List<String> labels = new LinkedList<String>();
    
    final protected String resultString = "RESULT";
    final protected Name resultName;
    protected Symbol resultSym = null;
    
    
    final protected String exceptionString = "EXCEPTION";
    final protected Name exceptionName;
    protected Symbol exceptionSym = null;
    
    static final public String terminationString = "TERMINATION";
    final protected Name terminationName;
    protected Symbol terminationSym = null;

    final protected String tmp = "tmp";

    protected LinkedList<ListBuffer<JCStatement>> statementStack = new LinkedList<ListBuffer<JCStatement>>();
    protected ListBuffer<JCStatement> currentStatements;
    protected int count;
    JCExpression eresult;
    protected boolean esc ; // if false, then translating for rac
    
    Map<Symbol,Name> newnames = new HashMap<Symbol,Name>();

    /** Returns a name for a given symbol that is unique across all names but fixed for
     * that symbol; the Java name is not unique since one name can be used in multiple scopes
     */
    public Name uniqueName(Symbol sym) {
        Name n = newnames.get(sym);
        if (n == null) { // FIXME - do we need to incorporate the file location?
            String s = sym.name.toString();// + "__" + (++count); // FIXME - Temporarily just use the same name, but will need to fix this
            n = names.fromString(s);
            newnames.put(sym,n);
        }
        return n;
    }
    
    /** Returns a new JCBlock representing the rewritten body of the given method declaration */
    public JCBlock convertMethodBody(JCMethodDecl decl) {
        JCMethodDecl prev = methodDecl;
        try {
            this.methodDecl = decl;
            return convert();
        } catch (RuntimeException e) {
            Log.instance(context).error("jml.internal.notsobad",e.getMessage());
            return null;
        } finally {
            methodDecl = prev;
        }
    }
    
    /** Creates an object to do the rewriting and assertion insertion. This object
     * can be reused to translate different method bodies, so long as the arguments
     * to this constructor remain appropriate.
     * @param context the compilation context to be used
     * @param esc true if the resulting AST is to be used for ESC, false if it is to be used for RAC
     */
    public JmlAssertionAdder(Context context, boolean esc) {
        this.context = context;
        this.esc = esc;
        this.log = Log.instance(context);
        this.M = JmlTree.Maker.instance(context);
        this.names = Names.instance(context);
        this.syms = Symtab.instance(context);
        this.types = Types.instance(context);
        this.specs = JmlSpecs.instance(context);
        this.treeutils = JmlTreeUtils.instance(context);
        this.resultName = names.fromString(resultString);
        this.exceptionName = names.fromString(exceptionString);
        this.terminationName = names.fromString(terminationString);
        this.count = 0;
    }
    
    /** Creates the overall framework for the block:
     * <PRE> 
         preconditions
         try {
            method body
         } finally {
            postconditions
         }
        </PRE>
     * <P>
     * The converted method body is a new block with the following characteristics:
     * <UL>
     * <LI>control flow statements are still present: if-then-else, switch, try-catch-finally blocks
     * <LI>expressions in Java code are decomposed into individual operations, with temporaries. This is so that
     * (a) it is easy to add any assertions prior to a specific operation
     * (b) it is straightforward to handle any operation with side-effects, even in the context of
     * short-circuit operations
     * <LI>assertions are added in for any checks that are desired (see the list below)
     * <LI>specification expressions are not decomposed into individual operations, since all the
     * sub-expressions are supposed to be pure; however, additional assertions are added before any
     * specification expression that checks that the specification expression is well-defined - TODO
     * <LI>conditional and short-circuit expressions (in Java code) are converted into if-then-else statements;
     * again, this is to simplify handling of side-effects in sub-expressions - TODO for short-circuit
     * <LI>all identifiers are made unique by combining the name with location information; but no
     * conversions for single-assignment are performed - TODO
     * </UL>
     * <P>
     * These operations are converted: 
     * <UL>
     * <LI>JML equivalence to boolean =
     * <LI>JML inequivalence to boolean !=
     * <LI>JML reverse implies (p <== q) to (p || !q)
     * <LI>Java assignments with operations are decomposed into the operation and the assignment
     * </UL>
     * <LI>These operations are retained:
     * <UL>
     * <LI>assignment
     * <LI>integer and floating +, -, *, /, %
     * <LI>== and !=
     * <LI>comparison operations (integer and floating)
     * <LI>bit and or xor
     * 
     * </UL>
     * <LI>TODO: mod, integer division, shift operations, bit operations, JML implies, JML subtype,
     * instanceof, short-circuit boolean operations
     * <LI>TODO: handle method calls
     * <LI>TODO: handle method calls in specifications
     * <LI>TODO: new Object, new Array expressions
     * <LI>TODO: quantifier and set comprehension expressions
     * <LI>TODO: \fresh operation
     * <LI>TODO: \nonnullelements
     * </UL>
     * <P>
     * These assumptions and checks are added in:
     * <UL>
     * <LI>assume the method preconditions, including null method parameters
     * <LI>assume any class invariants, including null field declarations
     * <LI>assert the method postconditions
     * <LI>assume any explicit assumption
     * <LI>assert any explicit assertion
     * <LI>assert the method exceptional postconditions - TODO
     * <LI>check for null-dereference on each field access
     * <LI>check for null-dereference on each array access - TODO
     * <LI>check for index out of bounds on each array access - TODO
     * <LI>check for assignment of null to any non-null variable or field
     * <LI>check for out of range arithmetic operations - TODO
     * </UL>
     * 
     * @return JCBlock with all assumptions, assertions, added declarations
     */
    public JCBlock convert() {
        JCMethodDecl decl = methodDecl;
        ListBuffer<JCStatement> initialStatements = new ListBuffer<JCStatement>();
        ListBuffer<JCStatement> outerFinalizeStats = new ListBuffer<JCStatement>();
        if (decl.restype != null && decl.restype.type.tag != TypeTags.VOID) {
            JCVariableDecl d = treeutils.makeVarDef(decl.restype.type,resultName,decl.sym,0);
            resultSym = d.sym;
            initialStatements.add(d);
        }
        
        {
            JCVariableDecl d = treeutils.makeVarDef(syms.exceptionType,exceptionName,decl.sym,treeutils.nulllit);
            exceptionSym = d.sym;
            initialStatements.add(d);
        }
        {
            JCVariableDecl d = treeutils.makeVarDef(syms.intType,terminationName,decl.sym,treeutils.zero);
            d.pos = decl.pos;
            terminationSym = d.sym;
            initialStatements.add(d);
        }
        
        // Give parameters unique names
        for (JCVariableDecl param : decl.params) {
            Name n = uniqueName(param.sym);
            param.name = n;
        }
        
        addPrePostConditions(decl, initialStatements,outerFinalizeStats);
        ListBuffer<JCStatement> bodyStatements = new ListBuffer<JCStatement>();
        currentStatements = bodyStatements;
        decl.body.accept(this);
        JCBlock b = M.at(decl.body.pos).Block(0, bodyStatements.toList());
        JCTry outerTryStatement = M.Try(b,List.<JCCatch>nil(),
                M.Block(0, outerFinalizeStats.toList()));
        initialStatements.add(outerTryStatement);
        return M.at(decl.pos).Block(0,initialStatements.toList());
    }
    
    /** Start collecting statements for a new block; push currentStatements onto a stack for later use */
    protected void push() {
        statementStack.add(0,currentStatements);
        currentStatements = new ListBuffer<JCStatement>();
    }
    
    /** Wrap the active currentStatements as a block (which is returned) and then resume collecting
     * statements with currentStatements value on the top of the stack (which is also removed
     * from the stack) */
    protected JCBlock popBlock(long flags, int pos) {
        JCBlock b = M.at(pos).Block(flags, currentStatements.toList());
        currentStatements = statementStack.removeFirst();
        return b;
    }
    
    /** Add a statement at the end of the active currentStatements sequence */ 
    protected void addStat(JCStatement stat) {
        currentStatements.add(stat);
    }
    
    /** This generates a JmlExpressionStatement comment statement with the given string as
     * text; the statement is not added to any statement list.
     */
    public JmlStatementExpr comment(int pos, String s) {
        return M.at(pos).JmlExpressionStatement(JmlToken.COMMENT,null,M.Literal(s));
    }
    
    /** This generates a comment statement whose content is the
     * given JCTree, pretty-printed; the statement is not added to any statement list
     */
    public JmlStatementExpr comment(JCTree t) {
        return comment(t.pos,JmlPretty.write(t,false));
    }
    
//    public void copyEndPosition(JCTree newnode, JCTree srcnode) {
//        Map<JCTree,Integer> z = log.currentSource().getEndPosTable();
//        if (z != null && srcnode != null) { // srcnode can be null when processing a switch statement
//            int end = srcnode.getEndPosition(z);
//            if (end != Position.NOPOS) z.put(newnode, end);
//        }
//    }
    
    public void error(int pos, String msg) {
        log.error(pos, "esc.internal.error", msg);
        throw new RuntimeException(msg);
    }
    
    // FIXME - have to figure out how to report positions in other files

    /** Adds an assertion, in which the referenced clause might be in another file */
    public void addAssertOther(JmlMethodClause clause, Label label, JCExpression ex, ListBuffer<JCStatement> stats) {
        DiagnosticSource dsource = log.currentSource();
        log.useSource(clause.sourcefile);
        int end = log.currentSource().getEndPosTable().get(clause);
        DiagnosticPositionSES pos = new DiagnosticPositionSES(clause.getStartPosition(),end,log.currentSource());
        log.useSource(dsource.getFile());
        String assertID = assertPrefix + (++assertCount);
        Name assertname = names.fromString(assertID);
        JCVariableDecl decl = treeutils.makeVarDef(syms.booleanType,assertname,null,ex);
        stats.add(decl);
        stats.add(treeutils.makeAssert(clause.pos,label,treeutils.makeIdent(clause.pos,decl.sym)));
        //positionMap.put(assertID, pos);
    }
    
    /** Adds an assertion - the referenced location is within the text of the file containing the method.
     * location.pos() is the location of the warning if ex is false; there is no associated location. */ 
    public void addAssert(JCTree location, Label label, JCExpression ex, ListBuffer<JCStatement> stats) {
        int start = location.getStartPosition();
        Integer end = log.currentSource().getEndPosTable().get(location);
        DiagnosticPositionSES pos = new DiagnosticPositionSES(start,end==null?start:end,log.currentSource());
        String assertID = assertPrefix + (++assertCount);
        Name assertname = names.fromString(assertID);
        JCVariableDecl decl = treeutils.makeVarDef(syms.booleanType,assertname,null,ex);
        stats.add(decl);
        stats.add(treeutils.makeAssert(location.pos,label,treeutils.makeIdent(location.pos,decl.sym)));
        //positionMap.put(assertID, pos);
    }
    
    /** Adds an assertion - the referenced location is within the text of the file containing the method;
     * The location of the warning (if ex is false) is location.pos(); relatedPos is the associated location. */ 
    public void addAssert(JCTree location, Label label, JCExpression ex, ListBuffer<JCStatement> stats, int relatedPos) {
        int start = location.getStartPosition();
        Integer end = log.currentSource().getEndPosTable().get(location);
        DiagnosticPositionSES pos = new DiagnosticPositionSES(start,end==null?start:end,log.currentSource());
        String assertID = assertPrefix + (++assertCount);
        Name assertname = names.fromString(assertID);
        JCVariableDecl decl = treeutils.makeVarDef(syms.booleanType,assertname,null,ex);
        stats.add(decl);
        stats.add(treeutils.makeAssert(location.pos,label,treeutils.makeIdent(location.pos,decl.sym),relatedPos));
        //positionMap.put(assertID, pos);
    }
    
    /** Adds an assertion - the referenced location is within the text of the file containing the method;
     * location.getStartPosition() is the location of the warning, if ex is false; there is no associated location. */ 
    public void addAssertStart(JCTree location, Label label, JCExpression ex, ListBuffer<JCStatement> stats) {
        int start = location.getStartPosition();
        Integer end = log.currentSource().getEndPosTable().get(location);
        DiagnosticPositionSES pos = new DiagnosticPositionSES(start,end==null?start:end,log.currentSource());
        String assertID = assertPrefix + (++assertCount);
        Name assertname = names.fromString(assertID);
        JCVariableDecl decl = treeutils.makeVarDef(syms.booleanType,assertname,null,ex);
        stats.add(decl);
        stats.add(treeutils.makeAssert(start,label,treeutils.makeIdent(location.pos,decl.sym)));
        //positionMap.put(assertID, pos);
    }
    
    /** Creates an assumption that two expressions are equal, adding the assumption to the given statement list. */
    public void addAssumeEqual(int pos, Label label, JCExpression ex, JCExpression exx, ListBuffer<JCStatement> stats) {
        stats.add(treeutils.makeAssume(pos,label,treeutils.makeBinary(pos,JCTree.EQ,ex,exx)));
    }
    
    /** Creates an assumption, adding it to the given statement list */
    public void addAssume(int pos, Label label, JCExpression ex, ListBuffer<JCStatement> stats) {
        stats.add(treeutils.makeAssume(pos,label,ex));
    }
    
    public void addPrePostConditions(JCMethodDecl decl, ListBuffer<JCStatement> initialStats, ListBuffer<JCStatement> finalizeStats) {
        currentStatements = initialStats;
        JmlMethodSpecs denestedSpecs = decl.sym == null ? null : JmlSpecs.instance(context).getDenestedSpecs(decl.sym);
        for (JCVariableDecl d: decl.params) {
            if (specs.isNonNull(d.sym, (Symbol.ClassSymbol)d.sym.owner.owner)) { // FIXME - needs to go through jmlrewriter ?
                addAssume(d.pos,Label.NULL_CHECK,treeutils.makeBinary(d.pos,JCTree.NE, treeutils.makeIdent(d.pos, d.sym), treeutils.nulllit), initialStats);
            }
            JCVariableDecl dd = treeutils.makeVariableDecl(M.Name("PRE_"+d.name.toString()), d.type, 
                    M.Ident(d.sym), decl.pos);
            preparams.put(d.sym,dd);
            addStat(dd);
        }
        JCExpression combinedPrecondition = null;
        ListBuffer<JCStatement> ensureStats = new ListBuffer<JCStatement>();
        ListBuffer<JCStatement> exsureStats = new ListBuffer<JCStatement>();
        for (JmlSpecificationCase scase : denestedSpecs.cases) {
            JCIdent preident = null;
            JCExpression preexpr = null;
            for (JmlMethodClause clause : scase.clauses) {
                switch (clause.token) {
                    case REQUIRES:
                        currentStatements = initialStats;
                        JCExpression ex = ((JmlMethodClauseExpr)clause).expression;
                        if (preexpr == null) preexpr = ex;
                        else preexpr = treeutils.makeAnd(ex.pos, preexpr, ex);
                        break;
                    default:
                }
            }
            if (preexpr == null) {
                preexpr = treeutils.trueLit;
            } else {
                preexpr = jmlrewriter.translate(preexpr);
            }
            precount++;
            Name prename = names.fromString(prePrefix + precount);
            JCVariableDecl d = treeutils.makeVarDef(syms.booleanType, prename, decl.sym, preexpr);
            preident = treeutils.makeIdent(preexpr.pos, d.sym);
            initialStats.add(d);
            preconditions.put(scase, preident);
            if (combinedPrecondition == null || preexpr == treeutils.trueLit) {
                combinedPrecondition = preident;
            } else {
                combinedPrecondition = treeutils.makeOr(combinedPrecondition.pos, combinedPrecondition, preident);
            }
            
            for (JmlMethodClause clause : scase.clauses) {
                switch (clause.token) {
                    case ENSURES:
                    {
                        currentStatements = ensureStats;
                        JCExpression ex = ((JmlMethodClauseExpr)clause).expression;
                        ex = jmlrewriter.translate(ex,preident,true);
                        //JCExpression exx = treeutils.makeUnary(clause.pos, JCTree.NOT, preident);
                        ex = treeutils.makeImplies(clause.pos, preident, ex);
                        // FIXME - if the clause is synthetic, the source file may be null
                        addAssertOther(clause,Label.POSTCONDITION,ex,ensureStats);
                        break;
                    }

                    case SIGNALS:
                    {
                        currentStatements = exsureStats;
                        JCExpression ex = ((JmlMethodClauseSignals)clause).expression;
                        JCVariableDecl vd = ((JmlMethodClauseSignals)clause).vardef;
                        ex = jmlrewriter.translate(ex,preident,true);
                        JCIdent exceptionId = treeutils.makeIdent(clause.pos,exceptionSym);
                        JCExpression nn = treeutils.makeBinary(clause.pos, JCTree.NE,  exceptionId, treeutils.nulllit);
                        ex = treeutils.makeImplies(clause.pos, preident, ex);
                        addAssertOther(clause,Label.SIGNALS,ex,exsureStats);
                        break;
                    }

                    default:
                }
            }
        }
        
        int p = methodDecl.pos;
        JCExpression cond = treeutils.makeEqObject(p,
                treeutils.makeIdent(p, exceptionSym),treeutils.nulllit);
        M.at(p);
        JCStatement ifstat = M.If(cond,M.Block(0, ensureStats.toList()),M.Block(0,exsureStats.toList()));
        finalizeStats.add(ifstat);
        // If combinedPrecondition is null then there were no specs, so the implicit precondition is true and does not
        // need to be checked
        if (combinedPrecondition != null) {
            initialStats.add(treeutils.makeAssume(combinedPrecondition.pos,Label.PRECONDITION,combinedPrecondition));
        }
    }
    
    public void addAssignableChecks(JCAssign that) {
        if (that.lhs instanceof JCIdent) addAssignableChecksVar((JCIdent)that.lhs,that);
    }
    
    public void addAssignableChecksVar(JCIdent id, JCTree location) {
        // Locally declared identifiers and method arguments are owned by the method - we don't check those
        // Class fields are owned by the class - we do check those
        if (!(id.sym.owner instanceof Symbol.ClassSymbol)) return;
        JmlMethodSpecs denestedSpecs = methodDecl.sym == null ? null : JmlSpecs.instance(context).getDenestedSpecs(methodDecl.sym);
        for (JmlSpecificationCase scase : denestedSpecs.cases) {
            JCIdent preident = preconditions.get(scase);
            JCExpression cond = treeutils.falseLit;
            int assignablePos = scase.pos;
            for (JmlMethodClause clause : scase.clauses) {
                if (clause.token != JmlToken.ASSIGNABLE) continue;
                assignablePos = clause.pos;
                JmlMethodClauseStoreRef assignable = (JmlMethodClauseStoreRef)clause;
                for (JCExpression sref: assignable.list) {
                    if (sref instanceof JCIdent) {
                        Symbol target = ((JCIdent)sref).sym;
                        if (target == id.sym) {
                            cond = treeutils.trueLit;
                        }
                    } else if (sref instanceof JCFieldAccess) {
                        JCFieldAccess fa = (JCFieldAccess)sref;
                        JCExpression selected = fa.selected;
                        boolean classRef = (selected instanceof JCIdent && ((JCIdent)selected).sym instanceof Symbol.ClassSymbol) ||
                                (selected instanceof JCFieldAccess && ((JCFieldAccess)selected).sym instanceof Symbol.ClassSymbol);
                        if (fa.name == null || fa.sym == id.sym) {
                            if (classRef && id.sym.isStatic() && id.sym.owner == selected.type.tsym) {
                                // FIXME -should we allow id.sym.owner to be a superclass of fa.selected.sym ?
                                cond = treeutils.trueLit;
                            } else if (!classRef && !id.sym.isStatic()) {
                                // Require that tree.selected == this
                                cond = treeutils.trueLit; // FIXME
                            }
                        }
                    } else if (sref instanceof JCArrayAccess) {
                        // does not match
                    } else {
                        System.out.println("NOT IMPLEMENTED: Can't handle assignable clauses with " + sref);
                        log.warning(sref.pos, "");
                    }
                }
            }
            cond = treeutils.makeImplies(id.pos,preident,cond);
            addAssert(location,Label.ASSIGNABLE,cond,currentStatements,assignablePos);
        }
    }

    // FIXME - figure out where this should be used
    /** This class holds a summary of specification and other useful data about a method.
     * It is only used during BasicBlock, but it is computed and cached on first request
     * (within the compilation context). The 'computeMethodInfo' call fills in the details.
     */
    static public class JmlMethodInfo {
        /** Creates an uninitialized instance from a method declaration */
        public JmlMethodInfo(JCMethodDecl d, Context context) { 
            this.decl = d; 
            this.owner = d.sym; 
            this.source = ((JmlMethodDecl)d).sourcefile;
            findOverrides(owner,context);
        }
        /** Creates an uninitialized instance from a method symbol */
        public JmlMethodInfo(MethodSymbol msym, Context context) { 
            this.decl = null; 
            this.owner = msym; 
            this.source = null;
            findOverrides(owner,context);
        }
        /** The symbol for this method */
        MethodSymbol owner;
        /** The declaration for this method, if there is one */
        /*@Nullable*/ JCMethodDecl decl;
        /** The JmlClassInfo stucture for the containing class */
        JmlClassInfo classInfo;
        /** The file in which the method is declared and implemented */
        JavaFileObject source;
        /** The name used as the result of the method - filled in during translation */
        String resultName;
        /** Whether the result is used - filled in during translation */
        boolean resultUsed = false;
        //FIXME
        JCExpression exceptionDecl;
        VarSymbol exceptionLocal;
        
        /** A list of all the forall predicates */ // FIXME - how interacts with spec cases
        java.util.List<JCVariableDecl> foralls = new LinkedList<JCVariableDecl>();
        /** A list of all the old predicates */ // FIXME - how interacts with spec cases
        ListBuffer<JCVariableDecl> olds = new ListBuffer<JCVariableDecl>();
        /** A list of the one combined requires predicate */ // FIXME - combined across inheritance?
        java.util.List<JmlMethodClauseExpr> requiresPredicates = new LinkedList<JmlMethodClauseExpr>();
        /** A list of desugared ensures predicates (i.e. in the form \old(precondition) ==> postcondition )*/
        java.util.List<JmlMethodClauseExpr> ensuresPredicates = new LinkedList<JmlMethodClauseExpr>();
        /** A list of desugared signals predicates (i.e. in the form \old(precondition) ==> postcondition )*/
        java.util.List<JmlMethodClauseExpr> exPredicates = new LinkedList<JmlMethodClauseExpr>();
        /** A list of desugared signals_only predicates (i.e. in the form \old(precondition) ==> postcondition )*/
        java.util.List<JmlMethodClauseExpr> sigPredicates = new LinkedList<JmlMethodClauseExpr>();
        /** A list of desugared diverges predicates (i.e. in the form \old(precondition) ==> postcondition )*/
        java.util.List<JmlMethodClauseExpr> divergesPredicates = new LinkedList<JmlMethodClauseExpr>();
        
        /** A structure holding information about desugared assignable clauses */
        public static class Entry {
            public Entry(JCExpression pre, java.util.List<JCExpression> list) {
                this.pre = pre;
                this.storerefs = list;
            }
            /** The precondition guarding this list of assignables */ // FIXME - with \old?
            public JCExpression pre;
            /** A list of storerefs for a particular spec case */
            public java.util.List<JCExpression> storerefs;
        }
        
        /** A list of assignable clause information */
        java.util.List<Entry> assignables = new LinkedList<Entry>();
        
        /** A list of overridden methods from super classes */
        java.util.List<MethodSymbol> overrides = new LinkedList<MethodSymbol>();
        
        /** A list of overridden methods from super interfaces */
        java.util.List<MethodSymbol> interfaceOverrides = new LinkedList<MethodSymbol>();
        
        protected void findOverrides(MethodSymbol sym, Context context) {
            MethodSymbol msym = sym;
            Types types = Types.instance(context);
            for (Type t = types.supertype(msym.owner.type); t.tag == CLASS; t = types.supertype(t)) {
                TypeSymbol c = t.tsym;
                Scope.Entry e = c.members().lookup(msym.name);
                while (e.scope != null) {
                    if (msym.overrides(e.sym, (TypeSymbol)msym.owner, types, false)) {
                        msym = (MethodSymbol)e.sym;
                        if (msym != null) overrides.add(msym);
                        break;
                    }
                    e = e.next();
                }
            }
            
        }

    }
    
    // FIXME - meethodInfo objects are going to be recomputed for every instance of BasicBlocker

    // FIXME - review and document
    Map<Symbol,JmlMethodInfo> methodInfoMap = new HashMap<Symbol,JmlMethodInfo>();

    // FIXME - review and document
    JmlMethodInfo getMethodInfo(MethodSymbol msym) {
        JmlMethodInfo mi = methodInfoMap.get(msym);
        if (mi == null) {
            mi = computeMethodInfo(msym);
            methodInfoMap.put(msym,mi);
        }
        return mi;
    }


    // FIXME - when run standalone (not in Eclipse), this method is called with the Object constructor 
    // as its argument, but with method.sym == null - is this because it is Binary?  is it not seeing the specs?
    protected JmlMethodInfo computeMethodInfo(MethodSymbol msym) {
        JmlSpecs.MethodSpecs mspecs = JmlSpecs.instance(context).getSpecs(msym);
        if (mspecs == null) {
            // The specs may be null because none were ever written (and there
            // was not even a declaration of the method to which an empty spec
            // was attached).
            mspecs = JmlSpecs.defaultSpecs(null);
        }
        // Note: The mspecs.decl may be null if the original class is only
        // binary and no specs file was written (so there is no source code
        // declaration anywhere).

        JmlMethodInfo mi = mspecs.cases.decl == null ? new JmlMethodInfo(msym,context) : new JmlMethodInfo(mspecs.cases.decl,context);
        JmlMethodSpecs denestedSpecs = msym == null ? null : JmlSpecs.instance(context).getDenestedSpecs(msym);
        if (JmlEsc.escdebug) log.noticeWriter.println("SPECS FOR " + msym.owner + " " + msym + " " + (denestedSpecs != null));
        if (JmlEsc.escdebug) log.noticeWriter.println(denestedSpecs == null ? "     No denested Specs" : ("   " + denestedSpecs.toString())); // FIXME - bad indenting

//        List<JCStatement> prev = newstatements;
//        newstatements = new LinkedList<JCStatement>();
//        if (denestedSpecs != null) {
//            // preconditions
//            JCExpression pre = denestedSpecs.cases.size() == 0 ? treeutils.makeBooleanLiteral(mspecs.cases.decl==null?0:mspecs.cases.decl.pos,true) : null;
//            int num = 0;
//            JavaFileObject source = null;
//            for (JmlSpecificationCase spc: denestedSpecs.cases) {
//                treetrans.pushSource(spc.sourcefile);
//                if (source == null) source = spc.sourcefile;
//                
//                for (JmlMethodClause c: spc.clauses) {
//                    if (c.token == JmlToken.FORALL) {
//                        JmlMethodClauseDecl d = (JmlMethodClauseDecl)c;
//                        for (JCVariableDecl stat: d.decls) {
//                            JCVariableDecl newstat = treetrans.translate(stat);
//                            mi.foralls.add(newstat);
//                        }
//                    } else if (c.token == JmlToken.OLD) {
//                        JmlMethodClauseDecl d = (JmlMethodClauseDecl)c;
//                        for (JCVariableDecl stat: d.decls) {
//                            JCVariableDecl newstat = treetrans.translate(stat);
//                            mi.olds.append(newstat);
//                        }
//                    }
//                }
//                JCExpression spre = null;
//                for (JmlMethodClause c: spc.clauses) {
//                    if (c.token == JmlToken.REQUIRES) {
//                        num++;
//                        JCExpression e = treetrans.translate((((JmlMethodClauseExpr)c).expression));
//                        e.pos = c.pos;
//                        copyEndPosition(e,c); // FIXME - these lines should be part of translate
//                        if (spre == null) {
//                            spre = e;
//                        } else {
//                            spre = treeutils.makeBinary(spre.pos,JCTree.AND,spre,e);
//                            copyEndPosition(spre,e);
//                        }
//                    }
//                }
//                if (spre == null) {
//                    spre = treeutils.makeBooleanLiteral(spc.pos,true);
//                    copyEndPosition(spre,spc);
//                }
//                if (pre == null) pre = spre;
//                else {
//                    pre = treeutils.makeBinary(pre.pos,JCTree.BITOR,pre,spre);
//                    copyEndPosition(pre,spre);
//                }
//                treetrans.popSource();
//            }{
//                JmlMethodClauseExpr p = factory.at(pre.pos).JmlMethodClauseExpr(JmlToken.REQUIRES,pre);
//                p.sourcefile = source != null ? source : log.currentSourceFile();
//                if (num == 1) copyEndPosition(p,pre);
//                mi.requiresPredicates.add(p);  // Just one composite precondition for all of the spec cases
//            }
//            
//            // postconditions
//            for (JmlSpecificationCase spc: denestedSpecs.cases) {
//                JCExpression spre = null;
//                for (JmlMethodClause c: spc.clauses) {
//                    if (c.token == JmlToken.REQUIRES) {
//                        if (spre == null) {
//                            JCExpression cexpr = ((JmlMethodClauseExpr)c).expression;
//                            spre = treetrans.translate((cexpr));
//                            copyEndPosition(spre,cexpr); // FIXME _ included in translate?
//                        } else {
//                            int pos = spre.getStartPosition();
//                            JCExpression cexpr = ((JmlMethodClauseExpr)c).expression;
//                            spre = treeutils.makeBinary(pos,JCTree.AND,spre,treetrans.translate((cexpr)));
//                            copyEndPosition(spre,cexpr);
//                        }
//                    }
//                }
//                if (spre == null) {
//                    spre = treeutils.makeBooleanLiteral(Position.NOPOS, true);
//                    // FIXME - fix position?
//                }
//                // FIXME - set startpos for JmlMethodInvocation
//                JCExpression nspre = factory.at(spre.getStartPosition()).JmlMethodInvocation(JmlToken.BSOLD,com.sun.tools.javac.util.List.of(spre));
//                copyEndPosition(nspre,spre);
//                nspre.type = spre.type;
//                spre = nspre;
//                for (JmlMethodClause c: spc.clauses) {
//                    if (c.token == JmlToken.ENSURES) {
//                        JCExpression e = treetrans.translate(((JmlMethodClauseExpr)c).expression);
//                        copyEndPosition(e,((JmlMethodClauseExpr)c).expression); // FIXME - fold into translate
//                        JCExpression post = treeutils.makeJmlBinary(e.getStartPosition(),JmlToken.IMPLIES,spre,e);
//                        copyEndPosition(post,e);// FIXME - fold into translate; wathc out for different source files
//                        JmlMethodClauseExpr p = factory.at(c.pos).JmlMethodClauseExpr(JmlToken.ENSURES,post);
//                        p.sourcefile = c.source();
//                        copyEndPosition(p,c);
//                        int sp1 = post.getStartPosition();
//                        int ep1 = post.getEndPosition(log.currentSource().getEndPosTable());
//                        int sp2 = spre.getStartPosition();
//                        int ep2 = spre.getEndPosition(log.currentSource().getEndPosTable());
//                        int sp3 = e.getStartPosition();
//                        int ep3 = e.getEndPosition(log.currentSource().getEndPosTable());
//                        mi.ensuresPredicates.add(p);
//                    } else if (c.token == JmlToken.ASSIGNABLE) {
//                        JmlMethodClauseStoreRef mod = (JmlMethodClauseStoreRef)c;
//                        // spre is the precondition under which the store-refs are modified
//                        List<JCExpression> list = mod.list; // store-ref expressions
//                        mi.assignables.add(new JmlMethodInfo.Entry(spre,list));
//                    } else if (c.token == JmlToken.SIGNALS) {
//                        // FIXME - what if there is no variable? - is there one already inserted or is it null?
//                        JmlMethodClauseSignals mod = (JmlMethodClauseSignals)c;
//                        JCExpression e = treetrans.translate(mod.expression);
//                        copyEndPosition(e,mod.expression); // FIXME - fold into translate
//                        boolean isFalse = (e instanceof JCLiteral) && ((JCLiteral)e).value.equals(0);
//                        // If vardef is null, then there is no declaration in the signals clause (e.g. it is just false).
//                        // We use the internal \exception token instead; we presume the type is java.lang.Exception 
//                        JCExpression post = treeutils.makeJmlBinary(e.getStartPosition(),JmlToken.IMPLIES,spre,e);
//                        copyEndPosition(post,e);// FIXME - fold into translate; wathc out for different source files
//                        if (!isFalse) {
//                            if (mod.vardef != null) {
//                                JCIdent id = treeutils.makeIdent(mod.vardef.pos,mod.vardef.sym);
//                                e = makeNNInstanceof(id,c.pos, mod.vardef.type, mod.vardef.pos);
//                                post = treeutils.makeJmlBinary(c.pos,JmlToken.IMPLIES,e,post);
//                            } else {
//                                JCExpression id = factory.at(c.pos).JmlSingleton(JmlToken.BSEXCEPTION);
//                                e = makeNNInstanceof(id,c.pos, syms.exceptionType, c.pos);
//                                post = treeutils.makeJmlBinary(c.pos,JmlToken.IMPLIES,e,post);
//                            }
//                            copyEndPosition(post,mod.expression);
//                        }
//                        JmlMethodClauseExpr p = factory.at(c.pos).JmlMethodClauseExpr(JmlToken.SIGNALS,post);
//                        copyEndPosition(p,c);
//                        p.sourcefile = c.source();
//                        // expression is    pre ==> post
//                        // or               (\typeof(exVar) <: ExTYPE) ==> (pre ==> post)
//                        mi.exPredicates.add(p);
//                    } else if (c.token == JmlToken.DIVERGES) {
//                        JCExpression e = treetrans.translate(((JmlMethodClauseExpr)c).expression);
//                        JCExpression post = treeutils.makeJmlBinary(e.getStartPosition(),JmlToken.IMPLIES,spre,e);
//                        JmlMethodClauseExpr p = factory.at(post.pos).JmlMethodClauseExpr(JmlToken.DIVERGES,post);
//                        p.sourcefile = c.source();
//                        mi.divergesPredicates.add(p);
//                    } else if (c.token == JmlToken.SIGNALS_ONLY) {
//                        JCExpression post = makeSignalsOnly((JmlMethodClauseSignalsOnly)c);
//                        JmlMethodClauseExpr p = factory.at(post.pos).JmlMethodClauseExpr(JmlToken.SIGNALS,post);
//                        p.sourcefile = c.source();
//                        mi.sigPredicates.add(p);
//                    }
//                    // FIXME - is signals_only desugared or handled here?
//                    // FIXME - we are ignoring forall old when duration working_space callable accessible measured_by captures
//                }
//            }
//        }
//        newstatements = prev;
        return mi;
    }
    


    // VISITOR METHODS

    public JCExpression scanret(JCTree tree) {
        if (tree==null) eresult = null;
        else super.scan(tree);
        return eresult;
    }
    
    public JCBlock translateIntoBlock(JCBlock block) {
        if (block == null) return null;
        push();
        scan(block.stats);
        return popBlock(block.flags,block.pos);
    }

    public JCBlock translateIntoBlock(int pos, List<JCStatement> stats) {
        push();
        scan(stats);
        return popBlock(0,pos);
    }

    public JCBlock translateIntoBlock(int pos, JCStatement stat) {
        push();
        scan(stat);
        return popBlock(0,pos);
    }

    @Override
    public void visitTopLevel(JCCompilationUnit that) {
        error(that.pos,"A visit method in JmlAssertionAdder was called that should not be: " + that.getClass());
    }

    @Override
    public void visitImport(JCImport that) {
        // FIXME - can these happen in an anonymous class expression
        error(that.pos,"A visit method in JmlAssertionAdder was called that should not be: " + that.getClass());
    }

    @Override
    public void visitClassDef(JCClassDecl that) {
        // FIXME - can these happen in an anonymous class expression or local class definition
        error(that.pos,"A visit method in JmlAssertionAdder was called that should not be: " + that.getClass());
    }

    @Override
    public void visitMethodDef(JCMethodDecl that) {
        // FIXME - can these happen in an anonymous class expression or local class definition
        error(that.pos,"A visit method in JmlAssertionAdder was called that should not be: " + that.getClass());
    }

    @Override
    public void visitVarDef(JCVariableDecl that) {
        JCExpression init = scanret(that.init);
        if (init != null) init = addImplicitConversion(that.type,init);
        // FIXME - need to make a unique symbol
        JCVariableDecl stat = M.at(that.pos).VarDef(that.sym,init);
        addStat(stat);
    }

    //OK
    @Override
    public void visitSkip(JCSkip that) {
        addStat(M.at(that.pos).Skip());
        // Caution - JML statements are subclasses of JCSkip
    }

    //OK
    @Override
    public void visitBlock(JCBlock that) {
        addStat(translateIntoBlock(that));
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitWhileLoop(JCWhileLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitForLoop(JCForLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    //OK
    @Override
    public void visitLabelled(JCLabeledStatement that) {
        addStat(comment(that.pos,"label:: " + that.label + ": ..."));

        // Note that the labeled statement will turn into a block
        JCBlock block = translateIntoBlock(that.pos,that.body);
        JCLabeledStatement stat = M.Labelled(that.label, block);
        addStat(stat);
    }

    //OK
    @Override
    public void visitSwitch(JCSwitch that) {
        JCExpression selector = scanret(that.selector);
        ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
        for (JCCase c: that.cases) {
            JCExpression pat = scanret(c.pat);
            JCBlock b = translateIntoBlock(c.pos,c.stats);
            JCCase cc = M.at(c.pos).Case(pat,b.stats);
            cases.add(cc);
        }
        addStat(M.at(that.pos).Switch(selector, cases.toList()));
    }

    //OK
    @Override
    public void visitCase(JCCase that) {
        // JCCase is handled directly in visitSwitch
        log.error(that.pos,"esc.internal.error","JmlAssertionAdder.visitCase should not be called");
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }
    
    //OK except concurrency checks
    @Override
    public void visitSynchronized(JCSynchronized that) {
        JCExpression lock = scanret(that.lock);
        if (that.lock instanceof JCParens && ((JCParens)that.lock).expr instanceof JCIdent && ((JCIdent)((JCParens)that.lock).expr).name.toString().equals("this")) {
            // Don't need to check that 'this' is not null
        } else {
            JCExpression e = treeutils.makeNeqObject(that.lock.pos, lock, treeutils.nulllit);
            addAssert(that.lock, Label.POSSIBLY_NULL, e, currentStatements);
        }
        JCBlock block = translateIntoBlock(that.body);
        addStat(M.at(that.pos).Synchronized(lock, block));
        // FIXME - need to add concurrency primitives
    }

    // OK - except instanceof
    @Override
    public void visitTry(JCTry that) {
        JCBlock body = translateIntoBlock(that.body);
        
        ListBuffer<JCStatement> finalizerstats = new ListBuffer<JCStatement>();

        if (that.catchers != null && !that.catchers.isEmpty()) {
            // If we have catch clauses, we create the following structure
            // if (EXCEPTION == NULL) {
            //      -- this is for non-exception returns and continued execution
            // } else if (EXCEPTION instanceof ETYPE) { -- where ETYPE is the type in the catch clause declaration
            //      ETYPE [catch declaration variable] = EXCEPTION;
            //      EXCEPTION = null ;   -- because we don't have an exceptional exit any more once the exception is caught
            //      TERMINATION = 0;     -- ditto
            //      all the statements of the catch clause
            // } else if (... -- for each catch clause in order
            // }
            // -- now do all the statements of the finally clause, if any
            // if (TERMINATION > 0) return RESULT;
            // if (TERMINATION < 0) throw EXCEPTION;
            
            // FIXME - not sure we are properly executing the finally statements when there is a return or throw from a catch clause
            
            JCIdent id = M.at(that.pos).Ident(exceptionSym);
            JCExpression ret = treeutils.makeEqObject(that.pos, id, treeutils.nulllit);
            M.at(that.pos);
            JCIf ifstat = M.If(ret, M.Block(0, List.<JCStatement>nil()), null);
            finalizerstats.add(ifstat);

            for (JCCatch catcher: that.catchers) {
                ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
                
                // [type of catch clause] [catch clause id ] = EXCEPTION
                id = M.at(that.pos).Ident(exceptionSym);
                JCVariableDecl vd = treeutils.makeVarDef(catcher.param.type, catcher.param.name, catcher.param.sym.owner, id);
                stats.add(vd);
                
                // EXCEPTION = null
                id = M.at(that.pos).Ident(exceptionSym);
                stats.add(M.Exec(M.Assign(id, treeutils.nulllit)));
                
                // TERMINATION = 0
                id = M.at(that.pos).Ident(terminationSym);
                stats.add(M.Exec(M.Assign(id, treeutils.zero)));
                
                // FIXME - need to put in the instanceof operation

                // add statements from the catch block
                JCBlock catchblock = translateIntoBlock(catcher.body);
                stats.addAll(catchblock.stats);
                
                M.at(catcher.pos);
                JCIf ifstatc = M.If(treeutils.trueLit, M.Block(catcher.body.flags, stats.toList()), null);
                ifstat.elsepart = ifstatc;
                ifstat = ifstatc;
            }
        }
        
        if (that.finalizer != null) {
            JCBlock finalizer = translateIntoBlock(that.finalizer);
            finalizerstats.add(finalizer);
        }
        
        // if (TERMINATION > 0) return ...
        JCIdent id = treeutils.makeIdent(that.pos, terminationSym);
        JCBinary cond = treeutils.makeBinary(that.pos,JCTree.GT, id, treeutils.zero );
        JCIf ifstat = M.If(cond,  M.Return(M.Ident(resultSym)), null);
        finalizerstats.add(ifstat);
        
        // if (TERMINATION < 0) throw ...
        id = treeutils.makeIdent(that.pos, terminationSym);
        cond = treeutils.makeBinary(that.pos,JCTree.LT, id, treeutils.zero );
        ifstat = M.If(cond,  M.Throw(M.Ident(exceptionSym)), null);
        finalizerstats.add(ifstat);
        
        
        JCStatement stat = M.at(that.pos).Try(body, List.<JCCatch>nil(), M.Block(0, finalizerstats.toList()));
        addStat(stat);
    }

    @Override
    public void visitCatch(JCCatch that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    // FIXME - check this
    @Override
    public void visitConditional(JCConditional that) {
        JCExpression cond = scanret(that.cond);
        Name ifname = names.fromString("conditionalResult" + (++count));
        JCVariableDecl vdecl = treeutils.makeVarDef(that.type, ifname, null, that.pos);
        currentStatements.add(vdecl);
        push();
        scan(that.truepart);
        addAssumeEqual(that.truepart.pos, Label.ASSIGNMENT, 
                  treeutils.makeIdent(that.truepart.pos, vdecl.sym), eresult, currentStatements);
        JCBlock trueblock = popBlock(0,that.truepart.pos);
        push();
        scan(that.falsepart);
        addAssumeEqual(that.falsepart.pos, Label.ASSIGNMENT, 
                treeutils.makeIdent(that.falsepart.pos, vdecl.sym), eresult, currentStatements);
        JCStatement stat = M.If(cond, trueblock, popBlock(0,that.falsepart.pos));
        currentStatements.add(stat);
        eresult = treeutils.makeIdent(that.pos, vdecl.sym);
    }

    @Override
    public void visitIf(JCIf that) {
        JCExpression cond = scanret(that.cond);
        push();
        scan(that.thenpart);
        JCBlock thenpart = popBlock(0,that.thenpart.pos);
        if (that.elsepart == null) {
            addStat(M.at(that.pos).If(cond,thenpart,null));
        } else {
            push();
            scan(that.elsepart);
            JCBlock elsepart = popBlock(0,that.elsepart.pos);
            addStat(M.at(that.pos).If(cond,thenpart,elsepart));
        }
    }

    @Override
    public void visitExec(JCExpressionStatement that) {
        addStat(comment(that));
        JCExpression arg = scanret(that.getExpression());
        
        // FIXME p- not sure this is ever needed - it isn't for assignments; is it  needed for method calls?
        // is there anything else that gets wrapped in an Exec?
        if (!(arg instanceof JCIdent)) {
            JCStatement stat = M.at(that.pos).Exec(arg);
            addStat(stat);
        }
    }

    @Override
    public void visitBreak(JCBreak that) {
        addStat(M.at(that.pos).Break(that.label));
    }

    @Override
    public void visitContinue(JCContinue that) {
        addStat(M.at(that.pos).Continue(that.label));
    }
    

    @Override
    public void visitReturn(JCReturn that) {
        addStat(comment(that));
        JCExpression arg = null;
        JCExpression retValue = that.getExpression();
        if (retValue != null) {
            arg = scanret(retValue);
            JCIdent resultid = M.at(that.pos).Ident(resultSym);
            JCStatement stat = treeutils.makeAssignStat(that.pos,resultid,arg);
            addStat(stat);
            arg = resultid;
        }
        JCIdent id = treeutils.makeIdent(that.pos,terminationSym);
        JCLiteral intlit = treeutils.makeIntLiteral(that.pos,that.pos);
        JCStatement stat = treeutils.makeAssignStat(that.pos,id,intlit);
        addStat(stat);
        // If the return statement is in a finally block, there may have been an exception
        // in the process of being thrown - so we set EXCEPTION to null.
        id = treeutils.makeIdent(that.pos,exceptionSym);
        stat = treeutils.makeAssignStat(that.pos,id,treeutils.nulllit);
        addStat(stat);
        
        stat = M.at(that.pos).Return(arg);
        addStat(stat);
    }

    @Override
    public void visitThrow(JCThrow that) {
        addStat(comment(that));
        // assert expr != null;
        JCExpression e = treeutils.makeNeqObject(that.expr.pos, scanret(that.expr), treeutils.nulllit);
        addAssert(that.expr, Label.POSSIBLY_NULL, e, currentStatements);
        if (that.expr.type.tag != TypeTags.BOT) {
            // ???Exception EXCEPTION_??? = expr;
            Name local = names.fromString(exceptionString + "L_" + that.pos);
            JCVariableDecl decl = treeutils.makeVarDef(that.expr.type,local,exceptionSym.owner,eresult); 
            // EXCEPTION = EXCEPTION_???;
            JCIdent id = treeutils.makeIdent(that.pos,exceptionSym);
            JCIdent localid = treeutils.makeIdent(that.pos,decl.sym);
            JCStatement assign = M.at(that.pos).Exec(treeutils.makeAssign(that.pos,id,localid));
            // TERMINATION = ???
            JCIdent tid = treeutils.makeIdent(that.pos,terminationSym);
            JCStatement term = M.Exec(M.Assign(tid,treeutils.makeIntLiteral(that.pos,-that.pos)));
            // throw EXCEPTION_??? ;
            JCThrow thrw = M.at(that.pos).Throw(localid);
            
            JCBlock block = M.at(that.pos).Block(0, List.<JCStatement>of(decl,assign,term,thrw));
            addStat(block);
        }
    }

    @Override
    public void visitAssert(JCAssert that) {
        // TODO: This automatically converts a Java assert statement to a JML one - is that what we always want?
        addStat(comment(that));
        scan(that.getCondition());
        addAssertStart(that.getCondition(),Label.EXPLICIT_ASSERT,eresult,currentStatements);
        scan(that.getDetail());
        // FIXME - what do wwe do with detail
    }

    @Override
    public void visitApply(JCMethodInvocation that) {
        JCExpression method = scanret(that.meth);
        ListBuffer<JCExpression> newargs = new ListBuffer<JCExpression>();
        for (JCExpression e: that.args) {
            newargs.add(scanret(e));
        }
        MethodSymbol msym = null;
//        if (that.meth instanceof JCIdent) msym = (MethodSymbol)((JCIdent)that.meth).sym;
//        else if (that.meth instanceof JCFieldAccess) msym = (MethodSymbol)((JCFieldAccess)that.meth).msym;
//        else {
//            //FIXME ERROR
//        }
//        JmlMethodSpecs calleeSpecs = JmlSpecs.instance(context).getDenestedSpecs(msym).deSugared;
//        calleeSpecs.
        JCExpression e = M.Apply(that.typeargs,method,newargs.toList());
        e.pos = that.pos;
        e.type = that.type;
        if (that.type != null) eresult = newTemp(e);
        // TODO Auto-generated method stub
        //throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }
    
    @Override
    public void visitNewClass(JCNewClass that) {
        // TODO Auto-generated method stub
        // FIXME - need arguments
        JCVariableDecl id = treeutils.makeVarDef(that.type,names.fromString("NEWOBJECT_" + that.pos), null, 0);
        currentStatements.add(id);
        eresult=treeutils.makeIdent(that.pos, id.sym);
        addAssume(that.pos,Label.NULL_CHECK,treeutils.makeBinary(that.pos,JCTree.NE, eresult, treeutils.nulllit), currentStatements);
    }

    @Override
    public void visitNewArray(JCNewArray that) {
        // TODO Auto-generated method stub
        // FIXME - need arguments
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitParens(JCParens that) {
        scan(that.getExpression());
        JCExpression arg = eresult;
        eresult = M.at(that.pos).Parens(arg);
        eresult.setType(that.type);
    }
    
    public JCExpression addImplicitConversion(Type lhstype, JCExpression rhs) {
        if (types.isSameType(lhstype,rhs.type)) return rhs;
        if (lhstype.isPrimitive() && !rhs.type.isPrimitive()) {
            // int = Integer and the like
            eresult = newTemp(rhs.pos, lhstype);
            // assert TValue(rhs) == eresult
            JCIdent id = M.Ident(names.fromString("intValue"));
            JCExpression e = treeutils.makeEquality(rhs.pos,
                    M.JmlMethodInvocation(id, List.<JCExpression>of(rhs)),
                    eresult);
            addAssume(rhs.pos,Label.EXPLICIT_ASSUME,e,currentStatements);
        } else if (!lhstype.isPrimitive() && rhs.type.isPrimitive()) {
            // Integer = int and the like
            eresult = newTemp(rhs.pos, lhstype);
            // assert TValue(eresult) == rhs
            JCIdent id = M.Ident(names.fromString("intValue"));
            JCExpression e = treeutils.makeEquality(rhs.pos,
                    M.JmlMethodInvocation(id, List.<JCExpression>of(eresult)),
                    rhs);
            addAssume(rhs.pos,Label.EXPLICIT_ASSUME,e,currentStatements);
        } else {
            
        }
        return eresult;
    }

    @Override
    public void visitAssign(JCAssign that) {
        if (that.lhs instanceof JCIdent) {
            JCIdent id = (JCIdent)that.lhs;
            JCExpression lhs = scanret(that.lhs);
            JCExpression rhs = scanret(that.rhs);
            rhs = addImplicitConversion(lhs.type,rhs);

            if (specs.isNonNull(id.sym,methodDecl.sym.enclClass())) {
                JCExpression e = treeutils.makeNeqObject(that.pos, rhs, treeutils.nulllit);
                addAssert(that, Label.POSSIBLY_NULL_ASSIGNMENT, e, currentStatements);
            }
            addAssignableChecks(that);
            
            JCExpression assign = treeutils.makeAssign(that.pos,  lhs, rhs);
            addStat(M.at(that.pos).Exec(assign));
            eresult = lhs;
            
        } else if (that.lhs instanceof JCFieldAccess) {
            // FIXME - needs a declaration of the new array
            JCFieldAccess fa = (JCFieldAccess)(that.lhs);
            JCExpression obj = scanret(fa.selected);
            JCExpression e = treeutils.makeNeqObject(obj.pos, obj, treeutils.nulllit);
            addAssert(that, Label.POSSIBLY_NULL, e, currentStatements);
            
            JCFieldAccess newfa = M.at(fa.pos).Select(obj, fa.name);
            newfa.sym = fa.sym;
            newfa.type = fa.type;
            JCExpression rhs = scanret(that.rhs);
            if (specs.isNonNull(fa.sym,methodDecl.sym.enclClass())) {
                e = treeutils.makeNeqObject(fa.pos, rhs, treeutils.nulllit);
                addAssert(that, Label.POSSIBLY_NULL_ASSIGNMENT, e, currentStatements);
            }
            JCExpression assign = treeutils.makeAssign(that.pos,newfa, rhs);
            eresult = assign;
           
        } else if (that.lhs instanceof JCArrayAccess) {
            JCArrayAccess aa = (JCArrayAccess)(that.lhs);
            JCExpression array = scanret(aa.indexed);
            JCExpression e = treeutils.makeNeqObject(array.pos, array, treeutils.nulllit);
            addAssert(that, Label.POSSIBLY_NULL, e, currentStatements);

            JCExpression index = scanret(aa.index);
            e = treeutils.makeBinary(index.pos, JCTree.GE, index, treeutils.zero);
            addAssert(that, Label.POSSIBLY_NEGATIVEINDEX, e, currentStatements);
            JCFieldAccess newfa = M.at(array.pos).Select(array, syms.lengthVar.name);
            newfa.type = syms.intType;
            newfa.sym = syms.lengthVar;
            e = treeutils.makeBinary(index.pos, JCTree.LT, index, newfa);
            addAssert(that, Label.POSSIBLY_TOOLARGEINDEX, e, currentStatements);
            
            JCExpression rhs = scanret(that.rhs);
            JCArrayAccess lhs = M.at(aa.pos).Indexed(array,index);
            lhs.type = aa.type;
            eresult = M.at(that.pos).Assign(lhs,rhs);
            eresult.type = that.type;
            
        } else {
            error(that.pos,"An unknown kind of assignment seen in JmlAssertionAdder: " + that.lhs.getClass());
        }
    }

    @Override
    public void visitAssignop(JCAssignOp that) {
        JCExpression lhs = that.lhs;
        JCExpression rhs = that.rhs;
        int op = that.getTag();
        op -= JCTree.ASGOffset;
        if (lhs instanceof JCIdent) {
            JCIdent newlhs = treeutils.makeIdent(lhs.pos,((JCIdent)lhs).sym);
            lhs = scanret(lhs);
            rhs = scanret(rhs);
            addBinaryChecks(that, op, lhs, rhs);
            
            rhs = treeutils.makeBinary(that.pos,op ,lhs,rhs);
            
            // FIXME - only applies to operators on objects (e.g. Strings?)
            if (specs.isNonNull(((JCIdent)lhs).sym,methodDecl.sym.enclClass())) {
                JCExpression e = treeutils.makeNeqObject(that.pos, rhs, treeutils.nulllit);
                addAssert(that, Label.POSSIBLY_NULL_ASSIGNMENT, e, currentStatements);
            }
            // Note that we need to introduce the temporary since the rhs contains
            // identifiers that will be captured by the lhs.
            JCIdent id = newTemp(rhs);
            addStat(M.Exec(treeutils.makeAssign(that.pos, newlhs, id)));
            eresult = treeutils.makeIdent(lhs.pos,((JCIdent)lhs).sym);
        } else if (lhs instanceof JCFieldAccess) {
            JCFieldAccess fa = (JCFieldAccess)lhs;
            lhs = scanret(fa.selected);
            JCExpression e = treeutils.makeNeqObject(lhs.pos, lhs, treeutils.nulllit);
            addAssert(that, Label.POSSIBLY_NULL, e, currentStatements);
            
            rhs = scanret(rhs);
            if (specs.isNonNull(fa.sym,methodDecl.sym.enclClass())) {
                e = treeutils.makeNeqObject(fa.pos, rhs, treeutils.nulllit);
                addAssert(that, Label.POSSIBLY_NULL_ASSIGNMENT, e, currentStatements);
            }
            lhs = M.at(fa.pos).Select(lhs, fa.sym);
            lhs.type = fa.type;

            addBinaryChecks(that,op,lhs,rhs);

            // Note that we need to introduce the temporary since the rhs contains
            // identifiers that will be captured by the lhs.
            rhs = treeutils.makeBinary(that.pos,op ,lhs,rhs);
            JCIdent id = newTemp(rhs);
            eresult = treeutils.makeAssign(that.pos, lhs, id);
        } else if (lhs instanceof JCArrayAccess) {
            JCArrayAccess aa = (JCArrayAccess)lhs;
            JCExpression array = scanret(aa.indexed);
            JCExpression e = treeutils.makeNeqObject(array.pos, array, treeutils.nulllit);
            addAssert(that, Label.POSSIBLY_NULL, e, currentStatements);

            JCExpression index = scanret(aa.index);
            e = treeutils.makeBinary(index.pos, JCTree.GE, index, treeutils.zero);
            addAssert(that, Label.POSSIBLY_NEGATIVEINDEX, e, currentStatements);
            JCFieldAccess newfa = M.at(array.pos).Select(array, syms.lengthVar.name);
            newfa.type = syms.intType;
            newfa.sym = syms.lengthVar;
            e = treeutils.makeBinary(index.pos, JCTree.LT, index, newfa);
            addAssert(that, Label.POSSIBLY_TOOLARGEINDEX, e, currentStatements);

            rhs = scanret(rhs);
            lhs = M.at(aa.pos).Indexed(array,index);
            lhs.type = aa.type;

            addBinaryChecks(that,op,lhs,rhs);
            // Note that we need to introduce the temporary since the rhs contains
            // identifiers that will be captured by the lhs.
            rhs = treeutils.makeBinary(that.pos,op ,lhs,rhs);
            JCIdent id = newTemp(rhs);
            
            eresult = treeutils.makeAssign(that.pos, lhs, id);
        } else {
            // FIXME - better error message
            throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
        }
    }

    @Override
    public void visitUnary(JCUnary that) {
        scan(that.getExpression());
        JCExpression arg = eresult;
        JCUnary expr = M.at(that.pos).Unary(that.getTag(),arg);
        expr.operator = that.operator;
        expr.setType(that.type);
        eresult = newTemp(expr);
        // FIXME - check arithmetic error
    }
    
    public JCIdent newTemp(int pos, Type t) {
        Name n = M.Name(tmp + (++count));
        JCVariableDecl d = treeutils.makeVarDef(t, n, null, pos); // FIXME - null or methodDecl.sym?
        currentStatements.add(d);
        JCIdent id = M.at(pos).Ident(d.sym);
        return id;
    }
    
    public JCIdent newTemp(JCExpression expr) {
        return newTemp(tmp + (++count),expr);
    }
    
    public JCIdent newTemp(String name, JCExpression expr) {
        Name n = M.Name(name);
        JCVariableDecl d = treeutils.makeVarDef(expr.type, n, null, expr); // FIXME - null or methodDecl.sym?
        currentStatements.add(d);
        JCIdent id = M.at(expr.pos).Ident(d.sym);
        return id;
    }
    
    public void addBinaryChecks(JCExpression that, int op, JCExpression lhs, JCExpression rhs) {

        if (op == JCTree.DIV || op == JCTree.MOD) {
            JCExpression nonzero = treeutils.makeBinary(that.pos, JCTree.NE, rhs, treeutils.makeIntLiteral(that.pos, 0));
            addAssert(that,Label.POSSIBLY_DIV0,nonzero,currentStatements);
        }
        // FIXME - add checks for numeric overflow
        
    }

    @Override
    public void visitBinary(JCBinary that) {
        JCExpression lhs = scanret(that.lhs);
        JCExpression rhs = scanret(that.rhs);
        addBinaryChecks(that,that.getTag(),lhs,rhs);
        JCBinary bin = M.at(that.pos).Binary(that.getTag(),lhs,rhs);
        bin.operator = that.operator;
        bin.setType(that.type);
        eresult = newTemp(bin);
        // FIXME - do this differently for short-circuit operations
    }

    @Override
    public void visitTypeCast(JCTypeCast that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitTypeTest(JCInstanceOf that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitIndexed(JCArrayAccess that) {
        scan(that.indexed);
        JCExpression indexed = eresult;
        JCExpression nonnull = treeutils.makeBinary(that.indexed.pos, JCTree.NE, indexed, 
                treeutils.nulllit);
        addAssert(that,Label.POSSIBLY_NULL,nonnull,currentStatements);

        scan(that.index);
        JCExpression index = eresult;
        JCExpression compare = treeutils.makeBinary(that.index.pos, JCTree.LE, treeutils.zero, 
                index);
        addAssert(that,Label.POSSIBLY_NEGATIVEINDEX,compare,currentStatements);
        JCExpression length = treeutils.makeLength(that.indexed.pos,indexed);
        compare = treeutils.makeBinary(that.pos, JCTree.LT, index, 
                length);
        addAssert(that,Label.POSSIBLY_TOOLARGEINDEX,compare,currentStatements);

        JCArrayAccess aa = M.at(that.pos).Indexed(indexed,index);
        aa.setType(that.type);
        eresult = newTemp(aa);
    }

    @Override
    public void visitSelect(JCFieldAccess that) {
        scan(that.selected);
        JCExpression selected = eresult;
        // Check that the selected expression is not null
        JCExpression nonnull = treeutils.makeNeqObject(that.pos, selected, 
                treeutils.nulllit);
        addAssert(that,Label.POSSIBLY_NULL,nonnull,currentStatements);
        JCFieldAccess fa = M.at(that.pos).Select(selected,that.name);
        fa.sym = that.sym;
        fa.setType(that.type);
        eresult = newTemp(fa);
    }
    
    @Override
    public void visitIdent(JCIdent that) {
        // Creates a new Ident node that uses the unique name for the symbol.
        // The symbol itself (and its name) is not changed.
        JCIdent id = treeutils.makeIdent(that.pos, that.sym);
        Name n = uniqueName(that.sym);
        id.name = n;
        eresult = id;
    }

    @Override
    public void visitLiteral(JCLiteral that) {
        eresult = treeutils.makeDuplicateLiteral(that.pos, that);
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitTypeApply(JCTypeApply that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitTypeParameter(JCTypeParameter that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitWildcard(JCWildcard that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitAnnotation(JCAnnotation that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitModifiers(JCModifiers that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitErroneous(JCErroneous that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitLetExpr(LetExpr that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlBinary(JmlBinary that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlChoose(JmlChoose that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlClassDecl(JmlClassDecl that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlCompilationUnit(JmlCompilationUnit that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlConstraintMethodSig(JmlConstraintMethodSig that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlDoWhileLoop(JmlDoWhileLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlEnhancedForLoop(JmlEnhancedForLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlForLoop(JmlForLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlGroupName(JmlGroupName that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlImport(JmlImport that) {
        // TODO Auto-generated method stub
        // FIXME - can this occur in an anonymous expression
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlLblExpression(JmlLblExpression that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodClauseCallable(JmlMethodClauseCallable that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodClauseConditional(JmlMethodClauseConditional that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodClauseDecl(JmlMethodClauseDecl that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodClauseExpr(JmlMethodClauseExpr that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodClauseGroup(JmlMethodClauseGroup that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodClauseSignals(JmlMethodClauseSignals that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodClauseSigOnly(JmlMethodClauseSignalsOnly that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodClauseStoreRef(JmlMethodClauseStoreRef that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodDecl(JmlMethodDecl that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodInvocation(JmlMethodInvocation that) {
        // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlMethodSpecs(JmlMethodSpecs that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlModelProgramStatement(JmlModelProgramStatement that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlPrimitiveTypeTree(JmlPrimitiveTypeTree that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlQuantifiedExpr(JmlQuantifiedExpr that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlSetComprehension(JmlSetComprehension that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    // FIXME _ should not be called here, just in the rewriter
    @Override
    public void visitJmlSingleton(JmlSingleton that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
//        switch (that.token) {
//            case BSRESULT:
//                if (resultSym == null) {
//                    throw new RuntimeException(); // FIXME - do something more informative - should not ever get here
//                } else {
//                    eresult = treeutils.makeIdent(that.pos, resultSym);
//                }
//                break;
//
//            case INFORMAL_COMMENT:
//                eresult = treeutils.makeBooleanLiteral(that.pos,true);
//                break;
//                
//            case BSEXCEPTION:
//                if (exceptionVar == null) {
//                    // FIXME -error
//                    log.noticeWriter.println("EXCEPTION VAR IS NULL");
//                    result = null;
//                } else {
//                    result = newIdentUse((VarSymbol)exceptionVar.sym,that.pos);
//                }
//                break;
//
//            case BSINDEX:
//            case BSVALUES:
//                if (that.info == null) {
//                    log.error(that.pos,"esc.internal.error","No loop index found for this use of " + that.token.internedName());
//                    result = null;
//                } else {
//                    result = newIdentUse((VarSymbol)that.info,that.pos);
//                }
//                break;
//                
//            case BSLOCKSET:
//            case BSSAME:
//            case BSNOTSPECIFIED:
//            case BSNOTHING:
//            case BSEVERYTHING:
//                Log.instance(context).error(that.pos, "jml.unimplemented.construct",that.token.internedName(),"BasicBlocker.visitJmlSingleton");
//                // FIXME - recovery possible?
//                break;
//
//            default:
//                Log.instance(context).error(that.pos, "jml.unknown.construct",that.token.internedName(),"BasicBlocker.visitJmlSingleton");
//            // FIXME - recovery possible?
//                break;
//        }
        //result = that; // TODO - can all of these be present in a basic block?
        //toLogicalForm.put(that,result);
    }
    

    @Override
    public void visitJmlSpecificationCase(JmlSpecificationCase that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlStatement(JmlStatement that) {
        switch (that.token) {
            case SET:
                scan(that.statement);
                break;
            case DEBUG:
                // FIXME - condition this on some command-line option?
                scan(that.statement);
                break;
            default:
                String msg = "Unknown token in JmlAssertionAdder.visitJmlStatement: " + that.token.internedName();
                log.error(that.pos, "esc.internal.error", msg);
                throw new RuntimeException(msg);
        }
    }

    @Override
    public void visitJmlStatementDecls(JmlStatementDecls that) {
        for (JCStatement stat: that.list) {
            stat.accept(this);
        }
    }

    @Override
    public void visitJmlStatementExpr(JmlStatementExpr that) {
        switch (that.token) {
            case ASSERT:
                addStat(comment(that));
                JCExpression e = jmlrewriter.translate(that.expression);
                addAssertStart(that,Label.EXPLICIT_ASSERT,e,currentStatements);
                break;
            case ASSUME:
                addStat(comment(that));
                JCExpression ee = jmlrewriter.translate(that.expression);
                addAssume(that.expression.pos,Label.EXPLICIT_ASSUME,ee,currentStatements);
                break;
            case COMMENT:
                addStat(that);
                break;
            case UNREACHABLE:
                addAssertStart(that,Label.UNREACHABLE,treeutils.falseLit,currentStatements);
                break;
            default:
                log.error("jml.internal","Unknown token type in JmlAssertionAdder.visitJmlStatementExpr: " + that.token.internedName());
                break;
        }
    }

    @Override
    public void visitJmlStatementLoop(JmlStatementLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlStatementSpec(JmlStatementSpec that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlStoreRefArrayRange(JmlStoreRefArrayRange that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlStoreRefKeyword(JmlStoreRefKeyword that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlStoreRefListExpression(JmlStoreRefListExpression that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseConditional(JmlTypeClauseConditional that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseConstraint(JmlTypeClauseConstraint that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseDecl(JmlTypeClauseDecl that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseExpr(JmlTypeClauseExpr that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseIn(JmlTypeClauseIn that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseInitializer(JmlTypeClauseInitializer that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseMaps(JmlTypeClauseMaps that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseMonitorsFor(JmlTypeClauseMonitorsFor that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlTypeClauseRepresents(JmlTypeClauseRepresents that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }

    @Override
    public void visitJmlVariableDecl(JmlVariableDecl that) {
        if ( JmlAttr.instance(context).isGhost(that.mods)) {
            JCExpression init = jmlrewriter.translate(that.init);
            // FIXME - need to make a unique symbol
            JmlVariableDecl stat = M.at(that.pos).VarDef(that.sym,init);
            addStat(stat);
        } else {
            JCExpression init = scanret(that.init);
            if (init != null) init = addImplicitConversion(that.type,init);
            // FIXME - need to make a unique symbol
            JmlVariableDecl stat = M.at(that.pos).VarDef(that.sym,init);
            addStat(stat);
        }
    }

    @Override
    public void visitJmlWhileLoop(JmlWhileLoop that) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Unexpected visit call in JmlAssertionAdder: " + that.getClass());
    }
    
    /** This class rewrites (making a full copy) JML expressions (but not Java statements or the
     * Java expressions in Java statements).
     * Since JML expressions are pure (at least have no visible side-effects), we do not dismember expressions
     * into individual subexpressions as we do for expressions in Java statements. However, we do issue 
     * JML assert statements that check whether the JML expression being translated is well-defined; also
     * identifiers are changed in the same as as they are changed for Java expressions.
     * <P>
     * This class is purposely not static, so it can use the context of the enclosing Java rewriter.
     * <P>
     * It would be possible to combine this rewriter with the visitor in JmlAssertionAdder; we would
     * then need a field that serves as a flag to distinguish the one mode from the other. I chose not
     * to do that, so that the two kinds of rewriting can be separated, at the cost of some similar code.
     */
    // TODO - this expression rewriter would be better off with a visitor that returned a value from the visit/scan/accept methods
    public class JmlExpressionRewriter extends JmlTreeScanner {
        
        /** Contains an expression that is used as a guard in determining whether expressions
         * are well-defined. For example, suppose we are translating the expression 
         * a != null && a[i] == 0. Then condition is 'true' when a!=null is translated.
         * But when a[i] is translated, 'condition' will be a != null. The well-definedness
         * check for a[i] will then be (a != null) ==> (a != null && i >= 0 && i < a.length).
         * So the full expression is well-defined only if that implication can be proved given 
         * other pre-conditions.
         */
        JCExpression condition;
        
        /** Set to true when we are translating a normal or exceptional postcondition. It is used
         * to be sure the correct scope is used when method parameters are used in the postcondition.
         * If a method parameter is used in a postcondition it is evaluated in the pre-state since
         * any changes to the parameter within the body of the method are discarded upon exit and
         * are invisible outside the method (i.e. in the postcondition).
         */
        boolean isPostcondition;
        
        /** Every visit method that translates an expression must put its result in this
         * variable.
         */
        JCExpression ejmlresult;
        
        /** The translate methods are the entry point into the rewriter; they make a rewritten
         * copy of the given expression, not changing the original. */
        JCExpression translate(JCExpression that, boolean isPostcondition) {
            return translate(that,treeutils.trueLit,isPostcondition);
        }
        
        /** The translate methods are the entry point into the rewriter; they make a rewritten
         * copy of the given expression, not changing the original. */
        JCExpression translate(JCExpression that) {
            return translate(that,treeutils.trueLit,false);
        }

        /** The translate methods are the entry point into the rewriter; they make a rewritten
         * copy of the given expression, not changing the original. */
        JCExpression translate(JCExpression that, JCExpression condition, boolean isPostcondition) {
            if (that == null) return null;
            this.isPostcondition = isPostcondition;
            this.condition = condition;
            this.ejmlresult = null; // Just so it is initialized in case assignment is forgotten
            that.accept(this);
            return ejmlresult;
        }

        // VISITOR METHODS

        @Override
        public void visitTopLevel(JCCompilationUnit that) {
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitImport(JCImport that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitClassDef(JCClassDecl that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());

        }

        @Override
        public void visitMethodDef(JCMethodDecl that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());

        }

        @Override
        public void visitVarDef(JCVariableDecl that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
//            scan(that.init);
//            JCExpression init = that.init == null ? null : eresult;
//            // FIXME - need to make a unique symbol
//            JCVariableDecl stat = M.at(that.pos).VarDef(that.sym,init);
//            addStat(stat);
        }

        @Override
        public void visitSkip(JCSkip that) {
            //addStat(that); // using the same statement - no copying
                            // Caution - JML statements are subclasses of JCSkip
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitBlock(JCBlock that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
//            push();
//            scan(that.stats);
//            JCBlock block = popBlock(that.flags,that.pos);
//            addStat(block);
        }

        @Override
        public void visitDoLoop(JCDoWhileLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitWhileLoop(JCWhileLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitForLoop(JCForLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitForeachLoop(JCEnhancedForLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitLabelled(JCLabeledStatement that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitSwitch(JCSwitch that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitCase(JCCase that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitSynchronized(JCSynchronized that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitTry(JCTry that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitCatch(JCCatch that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitConditional(JCConditional that) {
            that.cond.accept(this);
            JCExpression cond = ejmlresult;
            JCExpression prev = condition;
            try {
                condition = treeutils.makeAnd(that.pos, prev, cond);
                that.truepart.accept(this);
                JCExpression truepart = ejmlresult;
                condition = treeutils.makeAnd(that.pos, prev, treeutils.makeNot(that.falsepart.pos, cond));
                that.falsepart.accept(this);
                JCExpression falsepart = ejmlresult;
                ejmlresult = M.at(that.pos).Conditional(cond,truepart,falsepart);
                ejmlresult.type = that.type;
            } finally {
                condition = prev;
            }
        }

        @Override
        public void visitIf(JCIf that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
//            scan(that.cond);
//            JCExpression cond = ejmlresult;
//            push();
//            scan(that.thenpart);
//            JCBlock thenpart = popBlock(0,that.thenpart.pos);
//            push();
//            scan(that.elsepart);
//            JCBlock elsepart = popBlock(0,that.elsepart.pos);
//            addStat(M.at(that.pos).If(cond,thenpart,elsepart));
        }

        @Override
        public void visitExec(JCExpressionStatement that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
//           addStat(comment(that));
//            scan(that.getExpression());
//            JCExpression arg = ejmlresult;
//            JCStatement stat = M.at(that.pos).Exec(arg);
//            addStat(stat);
        }

        @Override
        public void visitBreak(JCBreak that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitContinue(JCContinue that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitReturn(JCReturn that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitThrow(JCThrow that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitAssert(JCAssert that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
//            // TODO: This automatically converts a Java assert statement to a JML one - is that what we always want?
//            addStat(comment(that));
//            scan(that.getCondition());
//            addAssertStart(that.getCondition(),Label.EXPLICIT_ASSERT,ejmlresult,currentStatements);
//            scan(that.getDetail());
        }

        @Override
        public void visitApply(JCMethodInvocation that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitNewClass(JCNewClass that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitNewArray(JCNewArray that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitParens(JCParens that) {
            scan(that.getExpression());
            ejmlresult = M.at(that.pos).Parens(ejmlresult);
            ejmlresult.setType(that.type);
        }

        @Override
        public void visitAssign(JCAssign that) {
            // FIXME
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitAssignop(JCAssignOp that) {
            // FIXME
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitUnary(JCUnary that) {
            scan(that.getExpression());
            JCExpression arg = ejmlresult;
            JCExpression expr = treeutils.makeUnary(that.pos,that.getTag(),arg);
            ejmlresult = expr;
            // FIXME - check arithmetic error
        }

        @Override
        public void visitBinary(JCBinary that) {
            scan(that.lhs);
            JCExpression lhs = ejmlresult;
            JCExpression rhs;
            int tag = that.getTag();
            if (tag == JCTree.AND) {
                JCExpression prev = condition;
                try {
                    condition = treeutils.makeAnd(that.lhs.pos, condition, lhs);
                    scan(that.rhs);
                    rhs = ejmlresult;
                } finally {
                    condition = prev;
                }
            } else if (tag == JCTree.OR) { 
                JCExpression prev = condition;
                try {
                    condition = treeutils.makeAnd(that.lhs.pos, condition, treeutils.makeNot(that.lhs.pos,lhs));
                    scan(that.rhs);
                    rhs = ejmlresult;
                } finally {
                    condition = prev;
                }
            } else if (tag == JCTree.DIV || tag == JCTree.MOD) {
                scan(that.rhs);
                rhs = ejmlresult;
                JCExpression nonzero = treeutils.makeBinary(that.pos, JCTree.NE, rhs, treeutils.makeIntLiteral(that.rhs.pos, 0));
                addAssert(that,Label.UNDEFINED_DIV0,treeutils.makeImplies(that.pos, condition, nonzero),currentStatements);
            } else {
                scan(that.rhs);
                rhs = ejmlresult;
            }
            // FIXME - add checks for numeric overflow
            JCExpression bin = treeutils.makeBinary(that.pos,that.getTag(),lhs,rhs);
            ejmlresult = bin;
            
        }

        @Override
        public void visitTypeCast(JCTypeCast that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitTypeTest(JCInstanceOf that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitIndexed(JCArrayAccess that) {
            scan(that.indexed);
            JCExpression indexed = ejmlresult;
            JCExpression nonnull = treeutils.makeBinary(that.indexed.pos, JCTree.NE, indexed, 
                    treeutils.nulllit);
            nonnull = treeutils.makeImplies(that.pos, condition, nonnull);
            addAssert(that,Label.UNDEFINED_NULL,nonnull,currentStatements);

            scan(that.index);
            JCExpression index = ejmlresult;
            JCExpression compare = treeutils.makeBinary(that.index.pos, JCTree.LE, treeutils.zero, 
                    index);
            compare = treeutils.makeImplies(that.pos, condition, compare);
            addAssert(that,Label.UNDEFINED_NEGATIVEINDEX,compare,currentStatements);
            
            JCExpression length = treeutils.makeLength(that.indexed.pos,indexed);
            compare = treeutils.makeBinary(that.pos, JCTree.LT, index, 
                    length);
            compare = treeutils.makeImplies(that.pos, condition, compare);
            addAssert(that,Label.UNDEFINED_TOOLARGEINDEX,compare,currentStatements);

            JCArrayAccess aa = M.at(that.pos).Indexed(indexed,index);
            aa.setType(that.type);
            ejmlresult = aa;
        }

        @Override
        public void visitSelect(JCFieldAccess that) {
            scan(that.selected);
            JCExpression selected = ejmlresult;
            // Check that the selected expression is not null
            JCExpression nonnull = treeutils.makeNeqObject(that.pos, selected, 
                    treeutils.nulllit);
            nonnull = treeutils.makeImplies(that.pos, condition, nonnull);
            addAssert(that,Label.UNDEFINED_NULL,nonnull,currentStatements);
            
            JCFieldAccess fa = M.at(that.pos).Select(selected,that.name);
            fa.sym = that.sym;
            fa.setType(that.type);
            
            ejmlresult = fa;
        }
        
        @Override
        public void visitIdent(JCIdent that) {
            JCVariableDecl decl;
            if (isPostcondition && (decl=preparams.get(that.sym)) != null) {
                JCIdent id = treeutils.makeIdent(that.pos,decl.sym);
                ejmlresult = id;            
            } else {
                JCIdent id = treeutils.makeIdent(that.pos,that.sym);
                Name n = uniqueName(that.sym);
                id.name = n;
                ejmlresult = id;
            }
        }

        @Override
        public void visitLiteral(JCLiteral that) {
            // Note that that.typetag can be different from that.type.tag - e.g for null values
            ejmlresult = treeutils.makeDuplicateLiteral(that.pos, that);
        }

        @Override
        public void visitTypeIdent(JCPrimitiveTypeTree that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitTypeArray(JCArrayTypeTree that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitTypeApply(JCTypeApply that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitTypeParameter(JCTypeParameter that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitWildcard(JCWildcard that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitTypeBoundKind(TypeBoundKind that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitAnnotation(JCAnnotation that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitModifiers(JCModifiers that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitErroneous(JCErroneous that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitLetExpr(LetExpr that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlBinary(JmlBinary that) {
            scan(that.lhs);
            JCExpression lhs = ejmlresult;
            JCExpression rhs,t;
            switch (that.op) {
                case IMPLIES:
                    condition = treeutils.makeAnd(that.pos, condition, lhs);
                    scan(that.rhs);
                    rhs = ejmlresult;
                    t = treeutils.makeNot(lhs.pos, lhs);
                    t = treeutils.makeOr(that.pos, t, rhs);
                    ejmlresult = t;
                    break;
                    
                case EQUIVALENCE:
                    scan(that.rhs);
                    rhs = ejmlresult;
                    t = treeutils.makeBinary(that.pos, JCTree.EQ, lhs, rhs);
                    ejmlresult = t;
                    break;
                    
                case INEQUIVALENCE:
                    scan(that.rhs);
                    rhs = ejmlresult;
                    t = treeutils.makeBinary(that.pos, JCTree.NE, lhs, rhs);
                    ejmlresult = t;
                    break;
                    
                case REVERSE_IMPLIES:
                    t = treeutils.makeNot(lhs.pos, lhs);
                    condition = treeutils.makeAnd(that.pos, condition, t);
                    scan(that.rhs);
                    rhs = treeutils.makeNot(that.rhs.pos, ejmlresult);
                    t = treeutils.makeOr(that.pos, lhs, rhs);
                    ejmlresult = t;
                    break;
                    
                 // FIXME - need <: operator
                    
                 default:
                     // TODO Auto-generated method stub
                     throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
                    
            }
        }

        @Override
        public void visitJmlChoose(JmlChoose that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlClassDecl(JmlClassDecl that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlCompilationUnit(JmlCompilationUnit that) {
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlConstraintMethodSig(JmlConstraintMethodSig that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlDoWhileLoop(JmlDoWhileLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlEnhancedForLoop(JmlEnhancedForLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlForLoop(JmlForLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlGroupName(JmlGroupName that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlImport(JmlImport that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlLblExpression(JmlLblExpression that) {
            String nm = "LABEL_" + that.token.internedName().substring(1) + "_" + that.label;
            scan(that.expression);
            JCIdent id = newTemp(nm,ejmlresult);
            labels.add(nm);
            ejmlresult = id;
        }

        @Override
        public void visitJmlMethodClauseCallable(JmlMethodClauseCallable that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodClauseConditional(JmlMethodClauseConditional that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodClauseDecl(JmlMethodClauseDecl that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodClauseExpr(JmlMethodClauseExpr that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodClauseGroup(JmlMethodClauseGroup that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodClauseSignals(JmlMethodClauseSignals that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodClauseSigOnly(JmlMethodClauseSignalsOnly that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodClauseStoreRef(JmlMethodClauseStoreRef that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodDecl(JmlMethodDecl that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlMethodInvocation(JmlMethodInvocation that) {
            switch (that.token) {
                case BSOLD:
                case BSPRE:
                    JCExpression m = jmlrewriter.translate(that.meth);
                    JCExpression arg = jmlrewriter.translate(that.args.get(0));
                    JmlMethodInvocation meth;
                    if (that.args.size() == 1) {
                        meth = M.JmlMethodInvocation(that.token,arg);
                    } else {
                        // The second argument is a label, not an expression
                        meth = M.JmlMethodInvocation(that.token,arg,that.args.get(1));
                    }
                    meth.type = that.type;
                    meth.pos = that.pos;
                    meth.startpos = that.startpos;
                    meth.varargsElement = that.varargsElement;
                    meth.meth = m;
                    meth.label = that.label;
                    meth.typeargs = that.typeargs; // FIXME - do these need translating?
                    ejmlresult = meth;
                    break;
                case BSTYPEOF :
                case BSELEMTYPE :
                case BSNONNULLELEMENTS :
                case BSMAX :
                case BSFRESH :
                case BSREACH :
                case BSSPACE :
                case BSWORKINGSPACE :
                case BSDURATION :
                case BSISINITIALIZED :
                case BSINVARIANTFOR :
                case BSNOWARN:
                case BSNOWARNOP:
                case BSWARN:
                case BSWARNOP:
                case BSBIGINT_MATH:
                case BSSAFEMATH:
                case BSJAVAMATH:
                case BSNOTMODIFIED:
                case BSTYPELC:
                    // FIXME - implement these
                    Log.instance(context).error("esc.not.implemented","Not yet implemented token in BasicBlocker: " + that.token.internedName());
                    ejmlresult = treeutils.trueLit; // FIXME - may not even be a boolean typed result
                    break;
                default:
                    Log.instance(context).error("esc.internal.error","Unknown token in BasicBlocker: " + that.token.internedName());
                    ejmlresult = treeutils.trueLit; // FIXME - may not even be a boolean typed result
            }
        }

        @Override
        public void visitJmlMethodSpecs(JmlMethodSpecs that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlModelProgramStatement(JmlModelProgramStatement that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlPrimitiveTypeTree(JmlPrimitiveTypeTree that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlQuantifiedExpr(JmlQuantifiedExpr that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlSetComprehension(JmlSetComprehension that) {
            // TODO Auto-generated method stub
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlSingleton(JmlSingleton that) {
            switch (that.token) {
                case BSRESULT:
                    if (resultSym == null) {
                        log.error(that.pos,"esc.internal.error", "It appears that \\result is used where no return value is defined" );
                        throw new RuntimeException("It appears that \\result is used where no return value is defined");
                    } else {
                        ejmlresult = treeutils.makeIdent(that.pos, resultSym);
                    }
                    break;

                case INFORMAL_COMMENT:
                    ejmlresult = treeutils.makeBooleanLiteral(that.pos,true);
                    break;
                    
                case BSEXCEPTION:
                    if (exceptionSym == null) {
                        log.error(that.pos,"esc.internal.error", "It appears that \\exception is used where no exception value is defined" );
                        throw new RuntimeException("It appears that \\exception is used where no exception value is defined");
                    } else {
                        ejmlresult = treeutils.makeIdent(that.pos,exceptionSym);
                    }
                    break;
    //
//                case BSINDEX:
//                case BSVALUES:
//                    if (that.info == null) {
//                        log.error(that.pos,"esc.internal.error","No loop index found for this use of " + that.token.internedName());
//                        result = null;
//                    } else {
//                        result = newIdentUse((VarSymbol)that.info,that.pos);
//                    }
//                    break;
//                    
//                case BSLOCKSET:
//                case BSSAME:
//                case BSNOTSPECIFIED:
//                case BSNOTHING:
//                case BSEVERYTHING:
//                    Log.instance(context).error(that.pos, "jml.unimplemented.construct",that.token.internedName(),"BasicBlocker.visitJmlSingleton");
//                    // FIXME - recovery possible?
//                    break;

                default:
                    Log.instance(context).error(that.pos, "jml.unknown.construct",that.token.internedName(),"BasicBlocker.visitJmlSingleton");
                // FIXME - recovery possible?
                    break;
            }
            //result = that; // TODO - can all of these be present in a basic block?
            //toLogicalForm.put(that,result);
        }
        

        @Override
        public void visitJmlSpecificationCase(JmlSpecificationCase that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlStatement(JmlStatement that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlStatementDecls(JmlStatementDecls that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlStatementExpr(JmlStatementExpr that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlStatementLoop(JmlStatementLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlStatementSpec(JmlStatementSpec that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlStoreRefArrayRange(JmlStoreRefArrayRange that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlStoreRefKeyword(JmlStoreRefKeyword that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlStoreRefListExpression(JmlStoreRefListExpression that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseConditional(JmlTypeClauseConditional that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseConstraint(JmlTypeClauseConstraint that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseDecl(JmlTypeClauseDecl that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseExpr(JmlTypeClauseExpr that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseIn(JmlTypeClauseIn that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseInitializer(JmlTypeClauseInitializer that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseMaps(JmlTypeClauseMaps that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseMonitorsFor(JmlTypeClauseMonitorsFor that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlTypeClauseRepresents(JmlTypeClauseRepresents that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlVariableDecl(JmlVariableDecl that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

        @Override
        public void visitJmlWhileLoop(JmlWhileLoop that) {
            // FIXME - can these happen in an anonymous class expression
            log.error(that.pos,"esc.internal.error", "Unexpected call in JmlExpressionRewriter of " + that.getClass() );
            throw new RuntimeException("Unexpected visit call in JmlExpressionRewriter: " + that.getClass());
        }

    }
}
