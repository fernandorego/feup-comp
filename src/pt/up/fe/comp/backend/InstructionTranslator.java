package pt.up.fe.comp.backend;

import org.specs.comp.ollir.*;

public class InstructionTranslator {
    public String translateInstruction(Instruction instruction, Method ancestorMethod, int indentationLevel) {
        InstructionType instructionType = instruction.getInstType();

        switch (instructionType) {
            case CALL:
                return translateInstruction((CallInstruction) instruction, ancestorMethod, indentationLevel);
            case RETURN:
                return translateInstruction((ReturnInstruction) instruction, indentationLevel);
            case PUTFIELD:
                return translateInstruction((PutFieldInstruction) instruction, ancestorMethod, indentationLevel);
            case GETFIELD:
                return translateInstruction((GetFieldInstruction) instruction, ancestorMethod, indentationLevel);
            case ASSIGN:
                return translateInstruction((AssignInstruction) instruction, ancestorMethod, indentationLevel);
            case BINARYOPER:
                return translateInstruction((BinaryOpInstruction) instruction, ancestorMethod, indentationLevel);
            case NOPER:
                return translateInstruction((SingleOpInstruction) instruction, ancestorMethod, indentationLevel);
            default:
                return "";
        }
    }

    public String translateInstruction(SingleOpInstruction instruction, Method ancestorMethod, int indentationLevel) {
        return getIndentation(indentationLevel) + getCorrespondingLoad(instruction.getSingleOperand(), ancestorMethod);
    }

    public String translateInstruction(GetFieldInstruction instruction, Method ancestorMethod, int indentationLevel) {
        Element destinationObject = instruction.getFirstOperand();
        Element destinationField = instruction.getSecondOperand();

        if (destinationObject.isLiteral() || destinationField.isLiteral()) {
            return "THERE ARE NO FIELD LITERALS";
        }

        return getIndentation(indentationLevel) + getCorrespondingLoad(destinationObject, ancestorMethod) + "\n" +
                getIndentation(indentationLevel) + "getfield " +
                JasminUtils.translateType(ancestorMethod.getOllirClass(), destinationField.getType()) + " " + ((Operand) destinationField).getName();
    }

    public String translateInstruction(PutFieldInstruction instruction, Method ancestorMethod, int indentationLevel) {
        Element destinationObject = instruction.getFirstOperand();
        Element destinationField = instruction.getSecondOperand();

        if (destinationObject.isLiteral() || destinationField.isLiteral()) {
            return "THERE ARE NO FIELD LITERALS";
        }

        StringBuilder jasminInstruction = new StringBuilder();
        Element newFieldValue = instruction.getThirdOperand();

        jasminInstruction.append(getIndentation(indentationLevel)).append(getCorrespondingLoad(newFieldValue, ancestorMethod)).append("\n");
        jasminInstruction.append(getIndentation(indentationLevel)).append("putfield ");

        jasminInstruction.append(((Operand) destinationObject).getName()).append("/").append(((Operand) destinationField).getName());
        jasminInstruction.append(" ").append(JasminUtils.translateType(ancestorMethod.getOllirClass(), destinationField.getType()));

        return jasminInstruction.toString();
    }

    public String translateInstruction(AssignInstruction instruction, Method ancestorMethod, int indentationLevel) {
        Element destination = instruction.getDest();

        if (destination.isLiteral()) {
            return "UNABLE TO STORE TO A LITERAL";
        }

        Instruction rhs = instruction.getRhs();

        if (rhs.getInstType() == InstructionType.CALL) {
            if (((CallInstruction) rhs).getInvocationType() == CallType.NEW) {
                return translateInstruction(rhs, ancestorMethod, indentationLevel);
            }
        }

        return translateInstruction(rhs, ancestorMethod, indentationLevel) + "\n" + getIndentation(indentationLevel) + getCorrespondingStore(destination, ancestorMethod);
    }

    public String translateInstruction(CallInstruction instruction, Method ancestorMethod, int indentationLevel) {
        StringBuilder jasminInstruction = new StringBuilder();
        StringBuilder parametersDescriptor = new StringBuilder();
        Operand caller = (Operand) instruction.getFirstArg();
        LiteralElement methodName = (LiteralElement) instruction.getSecondArg();

        CallType callType = instruction.getInvocationType();

        switch (callType) {
            case invokestatic:
            case invokevirtual:
                for (Element element: instruction.getListOfOperands()) {
                    jasminInstruction.append(getIndentation(indentationLevel)).append(getCorrespondingLoad(element, ancestorMethod)).append("\n");
                    parametersDescriptor.append(JasminUtils.translateType(ancestorMethod.getOllirClass(), element.getType()));
                }

                jasminInstruction.append(getIndentation(indentationLevel));

                if (callType == CallType.invokestatic) {
                    jasminInstruction.append("invokestatic ").append(caller.getName());
                } else {
                    jasminInstruction.append("invokevirtual ");

                    ClassType classType = (ClassType) instruction.getFirstArg().getType();
                    jasminInstruction.append(JasminUtils.getFullClassName(ancestorMethod.getOllirClass(), classType.getName()));
                }


                jasminInstruction.append(".").append(JasminUtils.trimLiteral(methodName.getLiteral()));
                jasminInstruction.append("(").append(parametersDescriptor);


                jasminInstruction.append(")").append(JasminUtils.translateType(ancestorMethod.getOllirClass(), instruction.getReturnType()));
                break;
            case invokespecial:
                if (ancestorMethod.isConstructMethod()) {
                    if (caller.getName().equals("this")) {
                        jasminInstruction.append(getIndentation(indentationLevel)).append(getCorrespondingLoad(caller, ancestorMethod)).append("\n");
                    }
                }

                for (Element element: instruction.getListOfOperands()) {
                    jasminInstruction.append(getIndentation(indentationLevel)).append(getCorrespondingLoad(element, ancestorMethod)).append("\n");
                    parametersDescriptor.append(JasminUtils.translateType(ancestorMethod.getOllirClass(), element.getType()));
                }

                jasminInstruction.append(getIndentation(indentationLevel));


                jasminInstruction.append("invokespecial ");

                ClassType classType = (ClassType) instruction.getFirstArg().getType();
                jasminInstruction.append(JasminUtils.getFullClassName(ancestorMethod.getOllirClass(), classType.getName()));


                jasminInstruction.append(".").append(JasminUtils.trimLiteral(methodName.getLiteral()));
                jasminInstruction.append("(").append(parametersDescriptor);


                jasminInstruction.append(")").append(JasminUtils.translateType(ancestorMethod.getOllirClass(), instruction.getReturnType())).append("\n");
                jasminInstruction.append(getIndentation(indentationLevel));

                if (!ancestorMethod.isConstructMethod()) {
                    jasminInstruction.append(getCorrespondingStore(instruction.getFirstArg(), ancestorMethod));
                }
                break;
            case NEW:
                ElementType elementType = caller.getType().getTypeOfElement();
                if (elementType == ElementType.OBJECTREF || elementType == ElementType.CLASS) {
                    jasminInstruction.append(getIndentation(indentationLevel)).append("new ").append(caller.getName()).append("\n");
                    jasminInstruction.append(getIndentation(indentationLevel)).append("dup");
                }
                break;
            case arraylength:
                break;
        }
        return jasminInstruction.toString();
    }

    public String translateInstruction(BinaryOpInstruction instruction, Method ancestorMethod, int indentationLevel) {
        Element first = instruction.getLeftOperand();
        Element second = instruction.getRightOperand();
        Operation operation = instruction.getOperation();
        OperationType operationType = operation.getOpType();

        switch (operationType) {
            case ADD:
            case ADDI32:
            case SUB:
            case SUBI32:
            case MUL:
            case MULI32:
            case DIV:
            case DIVI32:
                if (first.getType().getTypeOfElement() != operation.getTypeInfo().getTypeOfElement() || second.getType().getTypeOfElement() != operation.getTypeInfo().getTypeOfElement()) {
                    return "UNMATCHING TYPES";
                }

                if (operation.getTypeInfo().getTypeOfElement() != ElementType.INT32) {
                    return "INCORRECT TYPE";
                }

                StringBuilder jasminInstruction = new StringBuilder();

                jasminInstruction.append(getIndentation(indentationLevel)).append(getCorrespondingLoad(first, ancestorMethod)).append("\n");
                jasminInstruction.append(getIndentation(indentationLevel)).append(getCorrespondingLoad(second, ancestorMethod)).append("\n");
                jasminInstruction.append(getIndentation(indentationLevel));

                if (operationType == OperationType.ADD || operationType == OperationType.ADDI32) {
                    jasminInstruction.append("iadd");
                } else if (operationType == OperationType.SUB || operationType == OperationType.SUBI32) {
                    jasminInstruction.append("isub");
                } else if (operationType == OperationType.MUL || operationType == OperationType.MULI32) {
                    jasminInstruction.append("imul");
                } else {
                    jasminInstruction.append("idiv");
                }

                return jasminInstruction.toString();
        }
        return "";
    }

    public String translateInstruction(ReturnInstruction instruction, int indentationLevel) {
        StringBuilder jasminInstruction = new StringBuilder(getIndentation(indentationLevel));
        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case BOOLEAN:
            case INT32:
                jasminInstruction.append("ireturn");
                break;
            case OBJECTREF:
            case CLASS:
            case STRING:
                jasminInstruction.append("areturn");
                break;
            case VOID:
                jasminInstruction.append("return");
                break;
        }

        return jasminInstruction.toString();
    }

    private String getCorrespondingLoad(Element element, Method ancestorMethod) {
        if (element.isLiteral()) {
            LiteralElement literalElement = (LiteralElement) element;

            switch (literalElement.getType().getTypeOfElement()) {
                case INT32:
                case STRING:
                    return "ldc " + JasminUtils.trimLiteral(literalElement.getLiteral());
                case BOOLEAN:
                    String literal = JasminUtils.trimLiteral(literalElement.getLiteral());
                    return literal.equals("true") ? "ldc 1" : "ldc 0";
                default:
                    return "";
            }
        } else {
            Operand operand = (Operand) element;

            Descriptor operandDescriptor = ancestorMethod.getVarTable().get(operand.getName());

            switch (operand.getType().getTypeOfElement()) {
                case INT32:
                case BOOLEAN:
                    return "iload " + operandDescriptor.getVirtualReg();
                case CLASS:
                case OBJECTREF:
                case THIS:
                case STRING:
                    return "aload " + operandDescriptor.getVirtualReg();
                default:
                    return "";
            }
        }
    }

    private String getCorrespondingStore(Element element, Method ancestorMethod) {
        if (element.isLiteral()) {
            return "UNABLE TO STORE TO A LITERAL";
        } else {
            Operand operand = (Operand) element;

            Descriptor operandDescriptor = ancestorMethod.getVarTable().get(operand.getName());

            switch (operand.getType().getTypeOfElement()) {
                case INT32:
                case BOOLEAN:
                    return "istore " + operandDescriptor.getVirtualReg();
                case CLASS:
                case OBJECTREF:
                case THIS:
                case STRING:
                    return "astore " + operandDescriptor.getVirtualReg();
                default:
                    return "";
            }
        }
    }

    private String getIndentation(int indentationLevel) {
        return "\t".repeat(indentationLevel);
    }
}
