package org.jmlspecs.openjml.ext;

import org.eclipse.jdt.annotation.Nullable;
import org.jmlspecs.openjml.IArithmeticMode;
import org.jmlspecs.openjml.JmlTokenKind;
import org.jmlspecs.openjml.Strings;
import org.jmlspecs.openjml.JmlTree.JmlMethodInvocation;
import org.jmlspecs.openjml.esc.JmlAssertionAdder;
import org.jmlspecs.openjml.esc.Label;

import com.sun.tools.javac.code.JmlTypes;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.JmlType;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.JmlAttr;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.parser.ExpressionExtension;
import com.sun.tools.javac.parser.JmlParser;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import org.jmlspecs.openjml.JmlOption;

abstract public class Arithmetic extends ExpressionExtension {
    
    
    public static enum Mode { MATH(false,false), SAFE(true,true), JAVA(true,false) ;
        
        private boolean wrapsAround;
        private boolean overflowWarnings;
        private Mode(boolean w, boolean o) {
            wrapsAround = w;
            overflowWarnings = o;
        }
        public boolean wrapsAround() { return wrapsAround;}
        public boolean overflowWarnings() { return overflowWarnings; }
    
    };
    
    public Arithmetic(Context context) {
        super(context);
        this.intType = Symtab.instance(context).intType;
    }
    
    static public JmlTokenKind[] tokens() { return new JmlTokenKind[]{
            JmlTokenKind.BSBIGINT_MATH, JmlTokenKind.BSJAVAMATH, JmlTokenKind.BSSAFEMATH}; }
    
    Symbol codeBigintMath = null;
    Symbol codeSafeMath = null;
    Symbol codeJavaMath = null;
    Symbol specBigintMath = null;
    Symbol specJavaMath = null;
    Symbol specSafeMath = null;
    
    Type intType;
    
    boolean javaChecks;
    
    private void initModeSymbols() {
        if (codeBigintMath != null) return;
        ClassReader classReader = ClassReader.instance(context);
        Names names = Names.instance(context);
        specSafeMath = classReader.enterClass(names.fromString(Strings.jmlAnnotationPackage + ".SpecSafeMath"));
        specJavaMath = classReader.enterClass(names.fromString(Strings.jmlAnnotationPackage + ".SpecJavaMath"));
        specBigintMath = classReader.enterClass(names.fromString(Strings.jmlAnnotationPackage + ".SpecBigintMath"));
        codeSafeMath = classReader.enterClass(names.fromString(Strings.jmlAnnotationPackage + ".CodeSafeMath"));
        codeJavaMath = classReader.enterClass(names.fromString(Strings.jmlAnnotationPackage + ".CodeJavaMath"));
        codeBigintMath = classReader.enterClass(names.fromString(Strings.jmlAnnotationPackage + ".CodeBigintMath"));
    }
    
    public static boolean rac;
    
    public IArithmeticMode defaultArithmeticMode(Symbol sym, boolean jml) {
        initModeSymbols();
        if (!jml) {
            if (sym.attribute(codeBigintMath) != null) return Math.instance(context);
            if (sym.attribute(codeSafeMath) != null) return Safe.instance(context);
            if (sym.attribute(codeJavaMath) != null) return Java.instance(context);
            sym = sym.owner;
            if (!(sym instanceof Symbol.PackageSymbol)) return defaultArithmeticMode(sym,jml);
            String v = JmlOption.value(context,JmlOption.CODE_MATH);
            if ("java".equals(v)) return Java.instance(context);
            if ("safe".equals(v)) return Safe.instance(context);
            return Math.instance(context);
        } else {
            if (sym.attribute(specBigintMath) != null) return Math.instance(context);
            if (sym.attribute(specSafeMath) != null) return Safe.instance(context);
            if (sym.attribute(specJavaMath) != null) return Java.instance(context);
            sym = sym.owner;
            Arithmetic.Math.instance(context).rac = rac; // FIXME - HACK FOR NOW
            if (!(sym instanceof Symbol.PackageSymbol)) return defaultArithmeticMode(sym,jml);
            String v = JmlOption.value(context,JmlOption.SPEC_MATH);
            if ("java".equals(v)) return Java.instance(context);
            if ("safe".equals(v)) return Safe.instance(context);
            return Math.instance(context);
        }
    }
    
    public Type maxtype(JmlTypes jmltypes, Type lhs, Type rhs) {
        if (lhs.getTag() == TypeTag.CLASS) lhs = Types.instance(context).unboxedType(lhs); 
        if (rhs.getTag() == TypeTag.CLASS) rhs = Types.instance(context).unboxedType(rhs); 
        TypeTag lt = lhs.getTag();
        TypeTag rt = rhs.getTag(); // FIOXME - is the typetag UNKNOWN or NONE
        if (lt == TypeTag.UNKNOWN && lhs == jmltypes.REAL) return lhs;
        if (rt == TypeTag.UNKNOWN && rhs == jmltypes.REAL) return rhs;
        if (lt == TypeTag.UNKNOWN && lhs == jmltypes.BIGINT) {
            if (rt == TypeTag.DOUBLE || rt == TypeTag.FLOAT) return jmltypes.REAL;
            return lhs;
        }
        if (rt == TypeTag.UNKNOWN && rhs == jmltypes.BIGINT) {
            if (lt == TypeTag.DOUBLE || lt == TypeTag.FLOAT) return jmltypes.REAL;
            return rhs;
        }
        if (lt == TypeTag.DOUBLE) return lhs;
        if (rt == TypeTag.DOUBLE) return rhs;
        if (lt == TypeTag.FLOAT) return lhs; 
        if (rt == TypeTag.FLOAT) return rhs; 
        if (lt == TypeTag.LONG) return lhs; 
        if (rt == TypeTag.LONG) return rhs; 
        return intType;
    }

    
    @Override
    public void checkParse(JmlParser parser, JmlMethodInvocation e) {
        checkOneArg(parser,e);
    }

    @Override
    public Type typecheck(JmlAttr attr, JCExpression expr,
            Env<AttrContext> env) {
        JmlMethodInvocation tree = (JmlMethodInvocation)expr;
        JmlTokenKind token = tree.token;
        
        // Expect one argument of any type, result type is the same type
        // The argument expression may contain JML constructs
        ListBuffer<Type> argtypesBuf = new ListBuffer<>();
        attr.attribArgs(tree.args, env, argtypesBuf);
        //attr.attribTypes(tree.typeargs, env);
        int n = tree.args.size();
        if (n != 1) {
            error(tree.pos(),"jml.wrong.number.args",token.internedName(),1,n);
        }
        Type t = syms.errType;
        if (n > 0) {
            return tree.args.get(0).type;
        }
        return t;
    }
    /** Makes a expression appropriate to the given newtype and whether the translation
     *  is for rac or esc and whether it uses bit-vectors or not
     * @param rewriter the JmlAssertionAdder instance
     * @param that the original expression
     * @param arg the already translated argument
     * @param newtype the type in which to do the operation
     * @return
     */
    public JCExpression makeUnaryOp(JmlAssertionAdder rewriter, JCUnary that, Type newtype, boolean implementOverflow, boolean warnOverflow) {
        JCExpression arg = rewriter.convertExpr(that.getExpression());
        arg = rewriter.addImplicitConversion(arg,newtype,arg);
        JCTree.Tag optag = that.getTag();
        TypeTag typetag = that.type.getTag();
        JCExpression eresult = null;
        if (warnOverflow && optag == JCTree.Tag.NEG && rewriter.jmltypes.isSameType(that.type,that.getExpression().type)) {
            if (typetag == TypeTag.INT) {
                JCExpression e = rewriter.treeutils.makeNot(arg.pos,rewriter.treeutils.makeEquality(arg.pos, arg, rewriter.treeutils.makeIntLiteral(arg.pos, Integer.MIN_VALUE)));
                rewriter.addAssert(that,Label.ARITHMETIC_OP_RANGE,e,"(int negation)");
            } else if (typetag == TypeTag.LONG) {
                JCExpression e = rewriter.treeutils.makeNot(arg.pos,rewriter.treeutils.makeEquality(arg.pos, arg, rewriter.treeutils.makeLit(arg.pos, that.type, Long.MIN_VALUE)));
                rewriter.addAssert(that,Label.ARITHMETIC_OP_RANGE,e,"(long negation)");
            }
        }
        if (rewriter.rac && rewriter.jmltypes.isJmlType(newtype)) {
            if (optag == JCTree.Tag.NEG){ 
                if (rewriter.jmltypes.isSameType(newtype, rewriter.jmltypes.BIGINT)) {
                    eresult = rewriter.treeutils.makeUtilsMethodCall(that.pos,"bigint_neg",arg);
                }
                if (rewriter.jmltypes.isSameType(newtype, rewriter.jmltypes.REAL)) {
                    eresult = rewriter.treeutils.makeUtilsMethodCall(that.pos,"real_neg",arg);
                }
            } else if (optag == JCTree.Tag.POS) {
                eresult = arg;
            } else if (optag == JCTree.Tag.COMPL) {
                // Assumed to be a bigint (not real) operation - equivalent to -x-1
                JCExpression e = rewriter.treeutils.makeUtilsMethodCall(that.pos,"bigint_neg",arg);
                e = rewriter.treeutils.makeUtilsMethodCall(that.pos,"bigint_sub1",e);
                eresult = e;
            } else { 
                rewriter.log.error(that,"jml.internal","Unknown unary operation in Arithmetic.Java for JML type: " + that);
                eresult = arg;
            }
        } else {
            // Implement overflow when not using bit operations
            // RAC implements bit-limited arithmetic anyway, so don't need to do it here
            // If we have implicitly converted, which would be from a smaller type, then negate won't overflow
            eresult = rewriter.treeutils.makeUnary(that.pos,optag,that.getOperator(),arg);
            if (implementOverflow && rewriter.esc && optag == JCTree.Tag.NEG && !rewriter.useBV && rewriter.jmltypes.isSameType(that.type,that.getExpression().type)) {
                // FIXME - when this was accidentally enabled for rac, the conditional expression did not work correctly; not sure why - perhaps shared ASTs?
                // result == (arg == MIN_VALUE ? MIN_VALUE : -arg) 
                if (typetag == TypeTag.INT) {
                    JCExpression lit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
                    JCExpression eq = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, arg, lit);
                    JCExpression conditional = rewriter.treeutils.makeConditional(that.pos, eq, lit, eresult);
                    eresult = conditional;
                } else if (typetag == TypeTag.LONG) {
                    JCExpression lit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
                    JCExpression eq = rewriter.treeutils.makeEquality(that.pos, arg, lit);
                    JCExpression conditional = rewriter.treeutils.makeConditional(that.pos, eq, lit, eresult);
                    eresult = conditional;
                } else if (rewriter.jmltypes.isIntegral(that.type)) {
                    rewriter.log.error(that,"jml.internal","Unimplemented integral type in Arithmetic.Java: " + that.type);
                }
            }
        }
        return eresult;
    }
    
    public JCExpression makeBinaryOp(JmlAssertionAdder rewriter, JCBinary that, Type newtype, boolean implementOverflow, boolean checkOverflow) {
        this.javaChecks = (rewriter.esc || (rewriter.rac && JmlOption.isOption(context,JmlOption.RAC_JAVA_CHECKS)));

        JCTree.Tag optag = that.getTag();
        if (newtype == null) newtype = that.type;
        
        JCExpression lhs = rewriter.convertExpr(that.getLeftOperand());
        JCExpression rhs = rewriter.convertExpr(that.getRightOperand());

        // Need to do this operation before any implicit conversions, because those conversions may convert
        // to bigint or real, which complicates this test
        if (javaChecks) {
            if (optag == JCTree.Tag.DIV || optag == JCTree.Tag.MOD) {
                @Nullable JCExpression nonzero = rewriter.nonZeroCheck(that,rhs);
                if (nonzero != null) rewriter.addAssert(that,
                        rewriter.translatingJML ? Label.UNDEFINED_DIV0 : Label.POSSIBLY_DIV0,
                        rewriter.condition == null ? nonzero : rewriter.treeutils.makeImplies(that.pos, rewriter.condition, nonzero));
            }
        }
        
        
        lhs = rewriter.addImplicitConversion(lhs,newtype,lhs);
        rhs = rewriter.addImplicitConversion(rhs,newtype,rhs);
        
        // In MATH mode, the types can be promoted to \bigint or \real, even though the .type field is still smaller
        if (rewriter.jmltypes.isJmlType(lhs.type)) newtype = lhs.type;
        else if (rewriter.jmltypes.isJmlType(rhs.type)) newtype = rhs.type;
        

        TypeTag typetag = newtype.getTag();
        
        // Check for overflows
        
        if (checkOverflow) {
            // The overflow checks have to work whether integers are eventually encoded as fixed length bit vectors or as mathematical values
            if (optag == JCTree.Tag.PLUS || optag == JCTree.Tag.MINUS) {
                // For + : a > 0 && b > 0 ==> a <= MAX - b; a < 0 && b < 0 ==> MIN - b <= a;
                // For - : a > 0 && b < 0 ==> a <= MAX + b; a < 0 && b > 0 ==> MIN + b <= a;
                JCTree.Tag optagn = optag == JCTree.Tag.PLUS ? JCTree.Tag.MINUS : JCTree.Tag.PLUS;
                String str = optag == JCTree.Tag.PLUS ? "sum" : "difference";
                if (newtype.getTag() == TypeTag.INT) {
                    Symbol sym = optag == JCTree.Tag.PLUS ? rewriter.treeutils.intminusSymbol : rewriter.treeutils.intplusSymbol;
                    JCExpression maxlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MAX_VALUE);
                    JCExpression minlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
                    JCExpression zerolit = rewriter.treeutils.makeIntLiteral(that.pos, 0);
                    JCExpression a = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, zerolit, lhs, newtype);
                    JCExpression b1 = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, zerolit, rhs, newtype);
                    JCExpression b2 = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, rhs, zerolit, newtype);
                    JCExpression b = optag == JCTree.Tag.PLUS ? b1 : b2;
                    JCExpression c = rewriter.makeBin(that,  optagn,  sym, maxlit, rhs, newtype);
                    JCExpression d = rewriter.makeBin(that, JCTree.Tag.LE, rewriter.treeutils.intleSymbol, lhs, c, newtype);
                    JCExpression x = rewriter.treeutils.makeImplies(that.pos, rewriter.treeutils.makeAnd(that.pos,a,b), d);
                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "overflow in int " + str);
                    a = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, lhs, zerolit, newtype);
                    b = optag == JCTree.Tag.PLUS ? b2 : b1;
                    c = rewriter.makeBin(that,  optagn,  sym, minlit, rhs, newtype);
                    d = rewriter.makeBin(that, JCTree.Tag.LE, rewriter.treeutils.intleSymbol, c, lhs, newtype);
                    x = rewriter.treeutils.makeImplies(that.pos, rewriter.treeutils.makeAnd(that.pos,a,b), d);
                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "underflow in int " + str);
                } else if (newtype.getTag() == TypeTag.LONG) {
                    Symbol sym = optag == JCTree.Tag.PLUS ? rewriter.treeutils.longminusSymbol : rewriter.treeutils.longplusSymbol;
                    JCExpression maxlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MAX_VALUE);
                    JCExpression minlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
                    JCExpression zerolit = rewriter.treeutils.makeLongLiteral(that.pos, 0L);
                    JCExpression a = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.longltSymbol, zerolit, lhs, newtype);
                    JCExpression b1 = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.longltSymbol, zerolit, rhs, newtype);
                    JCExpression b2 = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.longltSymbol, rhs, zerolit, newtype);
                    JCExpression b = optag == JCTree.Tag.PLUS ? b1 : b2;
                    JCExpression c = rewriter.makeBin(that,  optagn,  sym, maxlit, rhs, newtype);
                    JCExpression d = rewriter.makeBin(that, JCTree.Tag.LE, rewriter.treeutils.longleSymbol, lhs, c, newtype);
                    JCExpression x = rewriter.treeutils.makeImplies(that.pos, rewriter.treeutils.makeAnd(that.pos,a,b), d);
                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "overflow in long " + str);
                    a = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.longltSymbol, lhs, zerolit, newtype);
                    b = optag == JCTree.Tag.PLUS ? b2 : b1;
                    c = rewriter.makeBin(that,  optagn,  sym, minlit, rhs, newtype);
                    d = rewriter.makeBin(that, JCTree.Tag.LE, rewriter.treeutils.longleSymbol, c, lhs, newtype);
                    x = rewriter.treeutils.makeImplies(that.pos, rewriter.treeutils.makeAnd(that.pos,a,b), d);
                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "underflow in long " + str);
                }
            } else if (optag == JCTree.Tag.MUL) {
                if (rewriter.useBV || rewriter.rac) {
                    if (newtype.getTag() == TypeTag.INT) {
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MUL, that.getOperator(), lhs, rhs);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.DIV, rewriter.treeutils.intdivideSymbol, a, lhs);
                        JCExpression c = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, lhs, rewriter.treeutils.makeIntLiteral(that.pos, 0));
                        JCExpression d = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, b, rhs);
                        rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, 
                                rewriter.treeutils.makeOr(that.pos, c, d), "int multiply overflow");
                    } else if (newtype.getTag() == TypeTag.LONG) {
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MUL, that.getOperator(), lhs, rhs);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.DIV, rewriter.treeutils.longdivideSymbol, a, lhs);
                        JCExpression c = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.longeqSymbol, lhs, rewriter.treeutils.makeLongLiteral(that.pos, 0L));
                        JCExpression d = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.longeqSymbol, b, rhs);
                        rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, 
                                rewriter.treeutils.makeOr(that.pos, c, d), "long multiply overflow");
                    }
                } else {
                    if (newtype.getTag() == TypeTag.INT) {
                        JCExpression minlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
                        JCExpression maxlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MAX_VALUE);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MUL, that.getOperator(), lhs, rhs);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LE,  minlit, a);
                        JCExpression c = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LE,  a, maxlit);
                        rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, 
                                rewriter.treeutils.makeAnd(that.pos, b, c), "int multiply overflow");
                    } else if (newtype.getTag() == TypeTag.LONG) {
                        JCExpression minlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
                        JCExpression maxlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MAX_VALUE);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MUL, that.getOperator(), lhs, rhs);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LE,  minlit, a);
                        JCExpression c = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LE,  a, maxlit);
                        rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, 
                                rewriter.treeutils.makeAnd(that.pos, b, c), "long multiply overflow");
                    }
                }
            } else if (optag == JCTree.Tag.DIV) {
                // a/b overflows only if  a == min && b == -1
                {
                    if (newtype.getTag() == TypeTag.INT) {
                        JCExpression minlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
                        JCExpression minusonelit = rewriter.treeutils.makeIntLiteral(that.pos, -1);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, lhs, minlit);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, rhs, minusonelit);
                        JCExpression x = rewriter.treeutils.makeNot(that.pos, rewriter.treeutils.makeAnd(that.pos, a, b));
                        rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "overflow in int divide");
                    } else if (newtype.getTag() == TypeTag.LONG) {
                        JCExpression minlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
                        JCExpression minusonelit = rewriter.treeutils.makeLongLiteral(that.pos, -1L);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.longeqSymbol, lhs, minlit);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.longeqSymbol, rhs, minusonelit);
                        JCExpression x = rewriter.treeutils.makeNot(that.pos, rewriter.treeutils.makeAnd(that.pos, a, b));
                        rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "overflow in long divide");
                    }
                }
            } else if (optag == JCTree.Tag.MOD){
                // a%b is always OK
            } else {
                // FIXME: Possibly: shifts
                rewriter.log.error(that,"jml.internal","Operation not implemented in Arithmetic.makeBinaryOp: " + optag.toString());
            }
            
        }
        
        // Implement the operation, correcting for overflow if needed
        
        boolean isBigint = rewriter.jmltypes.isJmlTypeOrRepType(newtype);
        JCExpression bin;
        if (rewriter.rac && rewriter.jmltypes.isJmlType(newtype)) {
            bin = rewriter.makeBin(that,optag,that.getOperator(),lhs,rhs,newtype);
        } else {
            bin = rewriter.treeutils.makeBinary(that.pos,optag,that.getOperator(),lhs,rhs);
            if (isBigint) bin.type = newtype;
            // Now we always will have mathematical values
            if (rewriter.esc && !rewriter.useBV && implementOverflow && !isBigint) {
                if (optag == JCTree.Tag.PLUS || optag == JCTree.Tag.MINUS) {
                    // ( result > MAX ? result + MIN + MIN : result < MIN ? result - MIN - MIN : result
                    if (typetag == TypeTag.INT) {
                        JCExpression minlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
                        JCExpression maxlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MAX_VALUE);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.GT, rewriter.treeutils.intgtSymbol, bin, maxlit);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, bin, minlit);
                        JCExpression c = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MINUS, rewriter.treeutils.intplusSymbol, bin, minlit);
                        JCExpression d = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MINUS, rewriter.treeutils.intplusSymbol, c, minlit);
                        JCExpression e = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.PLUS, rewriter.treeutils.intplusSymbol, bin, minlit);
                        JCExpression f = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.PLUS, rewriter.treeutils.intplusSymbol, e, minlit);
                        JCExpression g = rewriter.treeutils.makeConditional(that.pos, a, f, 
                                rewriter.treeutils.makeConditional(that.pos, b, d, bin));
                        bin = g;
                    } else if (typetag == TypeTag.LONG) {
                        JCExpression minlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
                        JCExpression maxlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MAX_VALUE);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.GT, rewriter.treeutils.intgtSymbol, bin, maxlit);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, bin, minlit);
                        JCExpression c = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MINUS, rewriter.treeutils.intplusSymbol, bin, minlit);
                        JCExpression d = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MINUS, rewriter.treeutils.intplusSymbol, c, minlit);
                        JCExpression e = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.PLUS, rewriter.treeutils.intplusSymbol, bin, minlit);
                        JCExpression f = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.PLUS, rewriter.treeutils.intplusSymbol, e, minlit);
                        JCExpression g = rewriter.treeutils.makeConditional(that.pos, a, f, 
                                rewriter.treeutils.makeConditional(that.pos, b, d, bin));
                        bin = g;
                    }
                } else if (optag == JCTree.Tag.MUL) {
                    if (typetag == TypeTag.INT) {
                        // TODO: This would be simpler if there were a REM operation for Ints in SMTLIB
                        // DIV is OK here because it is exact
                        JCExpression minlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
                        JCExpression maxlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MAX_VALUE);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MUL, that.getOperator(), lhs, rhs);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LE, rewriter.treeutils.intleSymbol,  minlit, a);
                        JCExpression c = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LE, rewriter.treeutils.intleSymbol,  a, maxlit);
                        JCExpression e = rewriter.treeutils.makeAnd(that.pos, b, c);

                        JCExpression f = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MOD, 
                                rewriter.treeutils.findOpSymbol(JCTree.Tag.MOD,syms.intType), bin, 
                                        rewriter.treeutils.makeLongLiteral(that.pos, (-2L)*Integer.MIN_VALUE));
                        JCExpression g = rewriter.treeutils.makeConditional(that.pos, e, bin, f); 
                        bin = g;
                    } else if (typetag == TypeTag.LONG) {
                        JCExpression minlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
                        JCExpression maxlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MAX_VALUE);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MUL, that.getOperator(), lhs, rhs);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LE, rewriter.treeutils.longleSymbol,  minlit, a);
                        JCExpression c = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.LE, rewriter.treeutils.longleSymbol,  a, maxlit);
                        JCExpression e = rewriter.treeutils.makeAnd(that.pos, b, c);

                        // FIXME - the multiplication by -2L below is bigger than a long
                        JCExpression f = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.MOD, 
                                rewriter.treeutils.findOpSymbol(JCTree.Tag.MOD,syms.intType), bin, 
                                        rewriter.treeutils.makeLongLiteral(that.pos, (-2L)*Long.MIN_VALUE));
                        JCExpression g = rewriter.treeutils.makeConditional(that.pos, e, bin, f); 
                        bin = g;
                    }
                } else if (optag == JCTree.Tag.DIV) {
                    if (typetag == TypeTag.INT) {
                        JCExpression minlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
                        JCExpression minusonelit = rewriter.treeutils.makeIntLiteral(that.pos, -1);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, lhs, minlit);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, rhs, minusonelit);
                        JCExpression x = rewriter.treeutils.makeNot(that.pos, rewriter.treeutils.makeAnd(that.pos, a, b));
                        bin = rewriter.treeutils.makeConditional(that.pos, x, bin, minlit);
                    } else if (typetag == TypeTag.LONG) {
                        JCExpression minlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
                        JCExpression minusonelit = rewriter.treeutils.makeLongLiteral(that.pos, -1L);
                        JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.longeqSymbol, lhs, minlit);
                        JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.longeqSymbol, rhs, minusonelit);
                        JCExpression x = rewriter.treeutils.makeNot(that.pos, rewriter.treeutils.makeAnd(that.pos, a, b));
                        bin = rewriter.treeutils.makeConditional(that.pos, x, bin, minlit);
                    }
                }
            }
        }
        
        
        

        //JCExpression bin = rewriter.makeBin(that,optag,that.getOperator(),lhs,rhs,newtype);
        return bin;

    }
    
    /** This implements the bigint (which is also real) mathematical mode */
    public static class Math extends Arithmetic implements IArithmeticMode {
        
        public Math(Context context) {
            super(context);
        }
        
        public static Math instance(Context context) {
            return instance(context,Math.class);
        }
        
        @Override
        public Mode mode() { return Mode.MATH; }
        
        /** Returns the appropriate mathematical type (\bigint or \real) given the input type */
        Type mathType(JmlAssertionAdder rewriter, Type t) {
            TypeTag tag = t.getTag();
            if (rewriter.jmltypes.isJmlType(t)) return t;
            if (rewriter.jmltypes.isIntegral(t)) return rewriter.jmltypes.BIGINT;
            if (tag == TypeTag.DOUBLE || tag == TypeTag.FLOAT) return rewriter.jmltypes.REAL;
            return t;
        }
        
        /** Rewrites unary operations; for NEG, the expression is
         *  promoted to the math type and negated; for PLUS and COMPL the
         *  expression type is unchanged. 
         */
        @Override
        public JCExpression rewriteUnary(JmlAssertionAdder rewriter, JCUnary that) {
            JCTree.Tag tag = that.getTag();
            JCExpression e;
            switch (tag) {
                case NEG: {
                    Type newtype = that.type;
                    if (rewriter.rac) newtype = mathType(rewriter,that.type);
                    e = makeUnaryOp(rewriter,that,newtype,false,false);
                    break;
                } 
                case PLUS:
                case COMPL: {
                    Type newtype = that.type; // No overflows possible - do not need to promote the type
                    e = makeUnaryOp(rewriter,that,newtype,false,false);
                    break;
                } 
                default:
                    e = null;
                    rewriter.log.error(that.pos, "jml.internal", "Unexpected operation in Arithmetic.Math.rewriteUnary");
            }
            return e;
        }
        
        @Override
        public JCExpression rewriteBinary(JmlAssertionAdder rewriter, JCBinary that) {
//            this.javaChecks = rewriter.esc || (rewriter.rac && JmlOption.isOption(context,JmlOption.RAC_JAVA_CHECKS));
//
//            JCTree.Tag op = that.getTag();
//            Type newtype = that.type;
//            if (newtype.getTag() == TypeTag.BOOLEAN) {
//                newtype = maxtype(rewriter.jmltypes,that.getLeftOperand().type,that.getRightOperand().type);
//            } else {
//                newtype = rewriter.jmltypes.BIGINT;
//            }
//            
//            JCExpression lhs = rewriter.convertExpr(that.getLeftOperand());
//            lhs = rewriter.addImplicitConversion(lhs,newtype,lhs);
//            JCExpression rhs = rewriter.convertExpr(that.getRightOperand());
//            rhs = rewriter.addImplicitConversion(rhs,newtype,rhs);
//
//            if (javaChecks) {
//                if (op == JCTree.Tag.DIV || op == JCTree.Tag.MOD) {
//                    @Nullable JCExpression nonzero = rewriter.nonZeroCheck(that,rhs);
//                    if (nonzero != null) rewriter.addAssert(that,
//                            rewriter.translatingJML ? Label.UNDEFINED_DIV0 : Label.POSSIBLY_DIV0,
//                            rewriter.treeutils.makeImplies(that.pos, rewriter.condition, nonzero));
//                }
//            }
//
//            JCExpression bin = rewriter.makeBin(that,op,that.getOperator(),lhs,rhs,newtype);
//            return bin;
            
            // Don't actually need to promote mod operations, but doing it for consistency
            Type newtype = that.type;
            if (rewriter.rac) newtype = mathType(rewriter,that.type);
            
            return makeBinaryOp(rewriter, that, newtype, false, false);

        }

    }

    public static class Safe extends Arithmetic implements IArithmeticMode {
        
        public Safe(Context context) {
            super(context);
        }
        
        public static Safe instance(Context context) {
            return instance(context,Safe.class);
        }
        
        @Override
        public Mode mode() { return Mode.SAFE; }

        @Override
        public JCExpression rewriteUnary(JmlAssertionAdder rewriter, JCUnary that) {
            return makeUnaryOp(rewriter, that, that.type, true, true);
        }
        
        @Override
        public JCExpression rewriteBinary(JmlAssertionAdder rewriter, JCBinary that) {
//            this.javaChecks = rewriter.esc || (rewriter.rac && JmlOption.isOption(context,JmlOption.RAC_JAVA_CHECKS));
//
//            Tag op = that.getTag();
//            Type newtype = that.type;
//            if (newtype.getTag() == TypeTag.BOOLEAN) {
//                newtype = maxtype(rewriter.jmltypes,that.getLeftOperand().type,that.getRightOperand().type);
//            }
//            
//            JCExpression lhs = rewriter.convertExpr(that.getLeftOperand());
//            lhs = rewriter.addImplicitConversion(lhs,newtype,lhs);
//            JCExpression rhs = rewriter.convertExpr(that.getRightOperand());
//            rhs = rewriter.addImplicitConversion(rhs,newtype,rhs);
//
//            if (javaChecks) {
//                if (op == JCTree.Tag.DIV || op == JCTree.Tag.MOD) {
//                    @Nullable JCExpression nonzero = rewriter.nonZeroCheck(that,rhs);
//                    if (nonzero != null) rewriter.addAssert(that,
//                            rewriter.translatingJML ? Label.UNDEFINED_DIV0 : Label.POSSIBLY_DIV0,
//                            rewriter.condition == null ? nonzero : rewriter.treeutils.makeImplies(that.pos, rewriter.condition, nonzero));
//                }
//            }
//            
//            if (op == JCTree.Tag.PLUS || op == JCTree.Tag.MINUS) {
//                // For + : a > 0 && b > 0 ==> a <= MAX - b; a < 0 && b < 0 ==> MIN - b <= a;
//                // For - : a > 0 && b < 0 ==> a <= MAX + b; a < 0 && b > 0 ==> MIN + b <= a;
//                JCTree.Tag opn = op == JCTree.Tag.PLUS ? JCTree.Tag.MINUS : JCTree.Tag.PLUS;
//                String str = op == JCTree.Tag.PLUS ? "sum" : "difference";
//                if (newtype.getTag() == TypeTag.INT) {
//                    Symbol sym = op == JCTree.Tag.PLUS ? rewriter.treeutils.intminusSymbol : rewriter.treeutils.intplusSymbol;
//                    JCExpression maxlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MAX_VALUE);
//                    JCExpression minlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
//                    JCExpression zerolit = rewriter.treeutils.makeIntLiteral(that.pos, 0);
//                    JCExpression a = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, zerolit, lhs, newtype);
//                    JCExpression b1 = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, zerolit, rhs, newtype);
//                    JCExpression b2 = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, rhs, zerolit, newtype);
//                    JCExpression b = op == JCTree.Tag.PLUS ? b1 : b2;
//                    JCExpression c = rewriter.makeBin(that,  opn,  sym, maxlit, rhs, newtype);
//                    JCExpression d = rewriter.makeBin(that, JCTree.Tag.LE, rewriter.treeutils.intleSymbol, lhs, c, newtype);
//                    JCExpression x = rewriter.treeutils.makeImplies(that.pos, rewriter.treeutils.makeAnd(that.pos,a,b), d);
//                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "overflow in int " + str);
//                    a = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.intltSymbol, lhs, zerolit, newtype);
//                    b = op == JCTree.Tag.PLUS ? b2 : b1;
//                    c = rewriter.makeBin(that,  opn,  sym, minlit, rhs, newtype);
//                    d = rewriter.makeBin(that, JCTree.Tag.LE, rewriter.treeutils.intleSymbol, c, lhs, newtype);
//                    x = rewriter.treeutils.makeImplies(that.pos, rewriter.treeutils.makeAnd(that.pos,a,b), d);
//                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "underflow in int " + str);
//                } else if (newtype.getTag() == TypeTag.LONG) {
//                    Symbol sym = op == JCTree.Tag.PLUS ? rewriter.treeutils.longminusSymbol : rewriter.treeutils.longplusSymbol;
//                    JCExpression maxlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MAX_VALUE);
//                    JCExpression minlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
//                    JCExpression zerolit = rewriter.treeutils.makeLongLiteral(that.pos, 0L);
//                    JCExpression a = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.longltSymbol, zerolit, lhs, newtype);
//                    JCExpression b1 = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.longltSymbol, zerolit, rhs, newtype);
//                    JCExpression b2 = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.longltSymbol, rhs, zerolit, newtype);
//                    JCExpression b = op == JCTree.Tag.PLUS ? b1 : b2;
//                    JCExpression c = rewriter.makeBin(that,  opn,  sym, maxlit, rhs, newtype);
//                    JCExpression d = rewriter.makeBin(that, JCTree.Tag.LE, rewriter.treeutils.longleSymbol, lhs, c, newtype);
//                    JCExpression x = rewriter.treeutils.makeImplies(that.pos, rewriter.treeutils.makeAnd(that.pos,a,b), d);
//                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "overflow in long " + str);
//                    a = rewriter.makeBin(that, JCTree.Tag.LT, rewriter.treeutils.longltSymbol, lhs, zerolit, newtype);
//                    b = op == JCTree.Tag.PLUS ? b2 : b1;
//                    c = rewriter.makeBin(that,  opn,  sym, minlit, rhs, newtype);
//                    d = rewriter.makeBin(that, JCTree.Tag.LE, rewriter.treeutils.longleSymbol, c, lhs, newtype);
//                    x = rewriter.treeutils.makeImplies(that.pos, rewriter.treeutils.makeAnd(that.pos,a,b), d);
//                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "underflow in long " + str);
//                }
//            } else if (op == JCTree.Tag.MUL) {
//                // FIXME - not implemented
//            } else if (op == JCTree.Tag.DIV) {
//                // a/b overflows only if  a == min && b == -1
//                if (newtype.getTag() == TypeTag.INT) {
//                    JCExpression minlit = rewriter.treeutils.makeIntLiteral(that.pos, Integer.MIN_VALUE);
//                    JCExpression minusonelit = rewriter.treeutils.makeIntLiteral(that.pos, -1);
//                    JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, lhs, minlit);
//                    JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.inteqSymbol, rhs, minusonelit);
//                    JCExpression x = rewriter.treeutils.makeNot(that.pos, rewriter.treeutils.makeAnd(that.pos, a, b));
//                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "overflow in int divide");
//                } else if (newtype.getTag() == TypeTag.LONG) {
//                    JCExpression minlit = rewriter.treeutils.makeLongLiteral(that.pos, Long.MIN_VALUE);
//                    JCExpression minusonelit = rewriter.treeutils.makeLongLiteral(that.pos, -1L);
//                    JCExpression a = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.longeqSymbol, lhs, minlit);
//                    JCExpression b = rewriter.treeutils.makeBinary(that.pos, JCTree.Tag.EQ, rewriter.treeutils.longeqSymbol, rhs, minusonelit);
//                    JCExpression x = rewriter.treeutils.makeNot(that.pos, rewriter.treeutils.makeAnd(that.pos, a, b));
//                    rewriter.addAssert(that, Label.ARITHMETIC_OP_RANGE, x, "overflow in long divide");
//                }
//            } else if (op == JCTree.Tag.MOD){
//                // a%b is always OK
//            }
//
//            JCExpression bin = rewriter.makeBin(that,op,that.getOperator(),lhs,rhs,newtype);
//            return bin;
            
            return makeBinaryOp(rewriter, that, null, true, true);

        }
        
    }

    public static class Java extends Arithmetic implements IArithmeticMode {

        public Java(Context context) {
            super(context);
        }
        
        public static Java instance(Context context) {
            return instance(context,Java.class);
        }
        
        @Override
        public Mode mode() { return Mode.JAVA; }

        @Override
        public JCExpression rewriteUnary(JmlAssertionAdder rewriter, JCUnary that) {
            return makeUnaryOp(rewriter, that, that.type, true, false);
        }
        
        @Override
        public JCExpression rewriteBinary(JmlAssertionAdder rewriter, JCBinary that) {
//            this.javaChecks = rewriter.esc || (rewriter.rac && JmlOption.isOption(context,JmlOption.RAC_JAVA_CHECKS));
//
//            JCTree.Tag op = that.getTag();
//            Type newtype = that.type;
//            if (newtype.getTag() == TypeTag.BOOLEAN) {
//                newtype = maxtype(rewriter.jmltypes,that.getLeftOperand().type,that.getRightOperand().type);
//            }
//            
//            JCExpression lhs = rewriter.convertExpr(that.getLeftOperand());
//            lhs = rewriter.addImplicitConversion(lhs,newtype,lhs);
//            JCExpression rhs = rewriter.convertExpr(that.getRightOperand());
//            rhs = rewriter.addImplicitConversion(rhs,newtype,rhs);
//
//            if (javaChecks) {
//                if (op == JCTree.Tag.DIV || op == JCTree.Tag.MOD) {
//                    @Nullable JCExpression nonzero = rewriter.nonZeroCheck(that,rhs);
//                    if (nonzero != null) rewriter.addAssert(that,
//                            rewriter.translatingJML ? Label.UNDEFINED_DIV0 : Label.POSSIBLY_DIV0,
//                            rewriter.condition == null ? nonzero : rewriter.treeutils.makeImplies(that.pos, rewriter.condition, nonzero));
//                }
//            }
//
//            JCExpression bin = rewriter.makeBin(that,op,that.getOperator(),lhs,rhs,newtype);
//            return bin;
            
            return makeBinaryOp(rewriter, that, null, true, false);
        }

    }

}
