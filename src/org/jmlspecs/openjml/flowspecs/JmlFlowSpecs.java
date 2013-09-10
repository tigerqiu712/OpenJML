package org.jmlspecs.openjml.flowspecs;

import static com.sun.tools.javac.code.Kinds.VAL;
import static com.sun.tools.javac.code.Kinds.VAR;
import static com.sun.tools.javac.code.TypeTags.ERROR;

import java.io.File;
import java.net.MalformedURLException;
import java.util.EnumMap;

import org.dom4j.DocumentException;
import org.jmlspecs.annotation.NonNull;
import org.jmlspecs.openjml.JmlOption;
import org.jmlspecs.openjml.JmlToken;
import org.jmlspecs.openjml.JmlTree.JmlMethodClauseDecl;
import org.jmlspecs.openjml.Main;
import org.jmlspecs.openjml.Utils;
import org.jmlspecs.openjml.flowspecs.LatticeParser.LatticeParserException;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Pair;
import com.sun.tools.javac.util.PropagatedException;

public class JmlFlowSpecs extends JmlEETreeScanner {

    /**
     * The key used to register an instance of JmlFlowSpecs in the compilation
     * context
     */
    protected static final Context.Key<JmlFlowSpecs> flowspecsKey = new Context.Key<JmlFlowSpecs>();

    /**
     * The method used to obtain the singleton instance of JmlEsc for this
     * compilation context
     */
    public static JmlFlowSpecs instance(Context context) {
        JmlFlowSpecs instance = context.get(flowspecsKey);
        if (instance == null) {
            instance = new JmlFlowSpecs(context);

            context.put(flowspecsKey, instance);
        }
        return instance;
    }

    public static final String     defaultLatticeFile    = "security.xml";

    public EnumMap<JmlToken, Name> tokenToAnnotationName = new EnumMap<JmlToken, Name>(
                                                                 JmlToken.class);

    private Lattice<String>        lattice;

    /**
     * The compilation context, needed to get common tools, but unique to this
     * compilation run
     */
    @NonNull
    Context                        context;

    /** Used to obtain cached symbols, such as basic types */
    @NonNull
    Symtab                         syms;

    /** The tool to log problem reports */
    @NonNull
    Log                            log;

    /** The OpenJML utilities object */
    @NonNull
    Utils                          utils;

    /** true if compiler options are set to a verbose mode */
    boolean                        verbose;

    /** Just for debugging flowspecs */
    public static boolean          flowspecsdebug        = true;                 // May
                                                                                  // be
                                                                                  // set
                                                                                  // externally
                                                                                  // to
                                                                                  // enable
                                                                                  // debugging
                                                                                  // while
                                                                                  // testing

    /**
     * The JmlEsc constructor, which initializes all the tools and other fields.
     */
    public JmlFlowSpecs(Context context) {
        this.context = context;
        this.syms = Symtab.instance(context);
        this.log = Log.instance(context);
        this.utils = Utils.instance(context);
        this.rs = Resolve.instance(context);

        Names names = Names.instance(context);

        for (JmlToken t : JmlToken.modifiers) {
            if (t.annotationType == null) {
                // No class for this token, but we won't complain
                // The result is to silently ignore the token (TODO)
            } else {
                String s = t.annotationType.getName();
                Name n = names.fromString(s);
                tokenToAnnotationName.put(t, n);
            }
        }

        this.verbose = flowspecsdebug
                || JmlOption.isOption(context, "-verbose") // The Java verbose
                                                           // option
                || utils.jmlverbose >= Utils.JMLVERBOSE;
    }

    public void check(JCTree tree) {

        //
        // Loads the security lattice and kicks off the type checking
        //
        File latticeFile = new File(defaultLatticeFile);

        if (latticeFile.exists()) {
            progress(1, 1, String.format("Loading secuirty lattice from %s",
                    latticeFile.getName()));
            LatticeParser p = new LatticeParser(new File(defaultLatticeFile));
            try {
                lattice = p.parse();
                progress(1, 1, "Done.");

                tree.accept(this);

            } catch (MalformedURLException | DocumentException
                    | LatticeParserException e) {
                log.error("jml.flowspecs.lattice.error");
            }
        } else {
            log.error("jml.flowspecs.missing.lattice");
        }
    }

    //
    // Main typechecking logic
    //

    /**
     * Reports progress to the registered IProgressListener; also checks if the
     * progress listener has received a user-cancellation, in which case this
     * method throws an exception to terminate processing
     * 
     * @param ticks
     *            amount of work to report
     * @param level
     *            level of the message (lower levels are more likely to be
     *            printed)
     * @param message
     *            the progress message
     */
    public void progress(int ticks, int level, String message) {
        org.jmlspecs.openjml.Main.IProgressListener pr = context
                .get(org.jmlspecs.openjml.Main.IProgressListener.class);
        boolean cancelled = pr == null ? false : pr.report(ticks, level,
                String.format("[FLOWSPECS] ", message));
        if (cancelled) {
            throw new PropagatedException(new Main.JmlCanceledException(
                    JmlOption.FLOWSPECS + " operation cancelled"));
        }
    }

    @Override
    public void enterExec(JCExpressionStatement tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exitExec(JCExpressionStatement tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterIdent(JCIdent tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exitIdent(JCIdent tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterBlock(JCBlock tree) {
        System.out.println(tree);

    }

    @Override
    public void exitBlock(JCBlock tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterVarDef(JCVariableDecl tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exitVarDef(JCVariableDecl tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterTopLevel(JCCompilationUnit tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exitTopLevel(JCCompilationUnit tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterJmlMethodClauseDecl(JmlMethodClauseDecl tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exitJmlMethodClauseDecl(JmlMethodClauseDecl tree) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitIdent(JCIdent tree) {
        result = resolveType(tree);
    }

    private SecurityType resolveType(JCIdent tree) {

        SecurityType t = null;
        if (tree.sym != null) {
            for (Attribute.Compound c : tree.sym.getAnnotationMirrors()) {

                // Case 1, they have an annotation of SOME kind specificed
                if (c.type.tsym.flatName().equals(
                        tokenToAnnotationName.get(JmlToken.LEVEL))
                        || c.type.tsym.flatName().equals(
                                tokenToAnnotationName.get(JmlToken.CHANNEL))) {

                    // Our parser promises us this will exist.
                    String label = extractSpecFromAnnotationValues(c);

                    // do a little more work to figure out the type;
                    if (c.type.tsym.flatName().equals(
                            tokenToAnnotationName.get(JmlToken.CHANNEL))) {

                        // TODO write mapping between channels and levels (no
                        // really, this has to be done)

                    } else {
                        t = new SecurityType(label);
                    }

                }
            }
        }
        // Case 2, we need to infer it.
        if (t == null) {
            t = new SecurityType(lattice.getTop());
        }

        return t;
    }

    private String extractSpecFromAnnotationValues(Attribute.Compound c) {

        String r = null;

        for (Pair<MethodSymbol, Attribute> p : c.values) {
            if (p.fst.toString().equals("value()")) {
                r = p.snd.toString().toUpperCase().replaceAll("\"", "");
                break;
            }
        }
        return r;
    }

    @Override
    public void visitAssign(JCAssign tree) {

        SecurityType lt = attribTree(tree.lhs, null, VAR, Type.noType);

        SecurityType rt = attribExpr(tree.rhs, env, lt);

        //TODO abstract the checking function
        result = check(tree, lt, rt);
        //result = check(tree, lt, VAL, pkind, pt);
    }

    SecurityType upperBound(SecurityType t1, SecurityType t2){
        
        if(t1.level.equals(t2)){
            return t1;
        }
        
        if(lattice.isSubclass(t1.level, t2.level)){
            return t2;
        }
        
        return t1;
    }
    
    public SecurityType check(JCAssign tree, SecurityType lt, SecurityType rt){
        
        if(lt.level.equals(rt.level) || lattice.isSubclass(rt.level, lt.level)){
            return upperBound(lt, rt);
        }

        log.error(tree.pos, "jml.flowspecs.lattice.invalidflow", rt.toString(), lt.toString());

        return SecurityType.wrong();
    }
    
    final Resolve    rs;

    /**
     * Visitor argument: the current environment.
     */
    Env<AttrContext> env;

    /**
     * Visitor argument: the currently expected proto-kind.
     */
    int              pkind;

    /**
     * Visitor argument: the currently expected proto-type.
     */
    Type             pt;

    /**
     * Visitor argument: the error key to be generated when a type error occurs
     */
    String           errKey;

    /**
     * Visitor result: the computed type.
     */
    SecurityType             result;
    
    public SecurityType attribExpr(JCTree tree, Env<AttrContext> env, Type pt) {
        return attribTree(tree, env, VAL, pt.tag != ERROR ? pt : Type.noType);
    }

    /**
     * Visitor method: attribute a tree, catching any completion failure
     * exceptions. Return the tree's type.
     * 
     * @param tree
     *            The tree to be visited.
     * @param env
     *            The environment visitor argument.
     * @param pkind
     *            The protokind visitor argument.
     * @param pt
     *            The prototype visitor argument.
     */
    SecurityType attribTree(JCTree tree, Env<AttrContext> env, int pkind, Type pt) {
        return attribTree(tree, env, pkind, pt, "incompatible.types");
    }

    SecurityType attribTree(JCTree tree, Env<AttrContext> env, int pkind, Type pt,
            String errKey) {
        Env<AttrContext> prevEnv = this.env;
        int prevPkind = this.pkind;
        Type prevPt = this.pt;
        String prevErrKey = this.errKey;
        try {
            this.env = env;
            this.pkind = pkind;
            this.pt = pt;
            this.errKey = errKey;
            tree.accept(this);
            return result;
        } catch (CompletionFailure ex) {
            tree.type = syms.errType;
            return null; // chk.completionError(tree.pos(), ex);
        } finally {
            this.env = prevEnv;
            this.pkind = prevPkind;
            this.pt = prevPt;
            this.errKey = prevErrKey;
        }
    }

    @Override
    public void enterAssign(JCAssign tree) {
    }

    @Override
    public void exitAssign(JCAssign tree) {
    }

    Type check(JCTree tree, Type owntype, int ownkind, int pkind, Type pt) {
        // if (owntype.tag != ERROR && pt.tag != METHOD && pt.tag != FORALL) {
        // if ((ownkind & ~pkind) == 0) {
        // owntype = chk.checkType(tree.pos(), owntype, pt, errKey);
        // } else {
        // log.error(tree.pos(), "unexpected.type",
        // kindNames(pkind),
        // kindName(ownkind));
        // owntype = types.createErrorType(owntype);
        // }
        // }
        // tree.type = owntype;
        return owntype;
    }

}
