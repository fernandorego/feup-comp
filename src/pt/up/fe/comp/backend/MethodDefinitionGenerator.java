package pt.up.fe.comp.backend;

import org.specs.comp.ollir.*;

public class MethodDefinitionGenerator {
    private Method method;

    public String getMethodDefinition() {
        StringBuilder methodDefinition = new StringBuilder();

        if (method.isConstructMethod()) {
            method.setMethodName("<init>");
        }

        methodDefinition.append(getMethodHeader()).append("\n");

        methodDefinition.append("\t.limit stack 99\n");
        methodDefinition.append("\t.limit locals 99\n");

        method.buildVarTable();
        InstructionTranslator instructionTranslator = new InstructionTranslator();
        boolean hasReturn = false;

        for (Instruction instruction: method.getInstructions()) {
            if (!hasReturn && instruction.getInstType() == InstructionType.RETURN) {
                hasReturn = true;
            }
            methodDefinition.append(instructionTranslator.translateInstruction(instruction, method)).append("\n");
        }

        if (!hasReturn) {
            methodDefinition.append("\t").append("return").append("\n");
        }

        methodDefinition.append(".end method\n");

        return methodDefinition.toString();
    }

    private String getMethodHeader() {
        StringBuilder methodHeader = new StringBuilder(".method ");

        if (method.getMethodAccessModifier().toString().equals("DEFAULT")) {
            methodHeader.append("public ");
        } else {
            methodHeader.append(method.getMethodAccessModifier().toString().toLowerCase()).append(" ");
        }

        if (method.isFinalMethod()) {
            methodHeader.append("final ");
        }

        if (method.isStaticMethod()) {
            methodHeader.append("static ");
        }

        methodHeader.append(method.getMethodName()).append(getMethodDescriptor());

        return methodHeader.toString();
    }

    public String getMethodDescriptor() {
        StringBuilder descriptor = new StringBuilder("(");

        for (Element parameter: method.getParams()) {
            descriptor.append(JasminUtils.translateType(method.getOllirClass(), parameter.getType()));
        }

        descriptor.append(")").append(JasminUtils.translateType(method.getOllirClass(), method.getReturnType()));

        return descriptor.toString();
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
