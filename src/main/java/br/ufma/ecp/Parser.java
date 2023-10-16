package br.ufma.ecp;

import static br.ufma.ecp.token.TokenType.*;


import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {


    private static class ParseError extends RuntimeException {}

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();

    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    void parse() {
        parseClass();
    }

    void parseClass() {
        printNonTerminal("class");
        expectPeek(CLASS);
        expectPeek(IDENTIFIER);
        expectPeek(LBRACE);
        while (peekToken.type == FIELD || peekToken.type == STATIC) { 
            parseClassVarDec(); 
        }

        while (peekToken.type == CONSTRUCTOR || peekToken.type == FUNCTION || peekToken.type == METHOD){
            parseSubroutineDec(); 
        }
        expectPeek(RBRACE);

        printNonTerminal("/class");
    }


    void parseClassVarDec() {
        printNonTerminal("classVarDec");

        expectPeek(FIELD, STATIC);
   
        expectPeek(INT, CHAR, BOOLEAN, IDENTIFIER);
        expectPeek(IDENTIFIER);
        while (peekToken.type == COMMA) {
            expectPeek(COMMA);
            expectPeek(IDENTIFIER);
        }
        expectPeek(SEMICOLON);

        printNonTerminal("/classVarDec");
    }

    void parseSubroutineDec() {
        printNonTerminal("subroutineDec");

        expectPeek(CONSTRUCTOR, FUNCTION, METHOD);
        
        expectPeek(VOID, INT, CHAR, BOOLEAN, IDENTIFIER);

        expectPeek(IDENTIFIER);

        expectPeek(LPAREN);
        parseParameterList();
        expectPeek(RPAREN);
        parseSubroutineBody();

        printNonTerminal("/subroutineDec");
    }

    void parseParameterList() {
        printNonTerminal("parameterList");

        if (!peekTokenIs(RPAREN)) { 
            expectPeek(INT,CHAR,BOOLEAN,IDENTIFIER);
            expectPeek(IDENTIFIER);
        }
        while (peekTokenIs(COMMA)){ 
            expectPeek(COMMA);
            expectPeek(INT,CHAR,BOOLEAN,IDENTIFIER);
            expectPeek(IDENTIFIER);
        }
   
        printNonTerminal("/parameterList");
    }

    void parseSubroutineCall() {
        var nArgs = 0;

        if (peekTokenIs(LPAREN)) { 
            expectPeek(LPAREN);
            nArgs = parseExpressionList() + 1;
            expectPeek(RPAREN);

        } else {
        
            expectPeek(DOT);
            expectPeek(IDENTIFIER); 

            expectPeek(LPAREN);
            nArgs += parseExpressionList();

            expectPeek(RPAREN);
        }

    }

    void parseDo() {
        printNonTerminal("doStatement");

        expectPeek(DO);
        expectPeek(IDENTIFIER);
        parseSubroutineCall();
        expectPeek(SEMICOLON);

        printNonTerminal("/doStatement");
    }

    void parseVarDec() {
        printNonTerminal("varDec");

        expectPeek(VAR);

        expectPeek(INT,CHAR,BOOLEAN,IDENTIFIER);
        expectPeek(IDENTIFIER); 

        while (peekTokenIs(COMMA)) { 
            expectPeek(COMMA);
            expectPeek(IDENTIFIER);
        }

        expectPeek(SEMICOLON);

        printNonTerminal("/varDec");
    }

    void parseSubroutineBody() {
        printNonTerminal("subroutineBody");

        expectPeek(LBRACE);
        
        while(peekTokenIs(VAR)){
            parseVarDec();
        }
        parseStatements();

        expectPeek(RBRACE);

        printNonTerminal("/subroutineBody");
    }

    void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(LET);
        expectPeek(IDENTIFIER);

        if (peekTokenIs(LBRACKET)) {
            expectPeek(LBRACKET);
            parseExpression();
            expectPeek(RBRACKET);
        }

        expectPeek(EQ);
        parseExpression();
        expectPeek(SEMICOLON);
        printNonTerminal("/letStatement");
    }

    void parseWhile() {
        printNonTerminal("whileStatement");

        expectPeek(WHILE);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);

        printNonTerminal("/whileStatement");
    }

    void parseIf() {
        printNonTerminal("ifStatement");

        expectPeek(IF);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);

        if(peekTokenIs(ELSE)){ 
            expectPeek(LBRACE);
            parseStatements();
            expectPeek(RBRACE);
        }

        printNonTerminal("/ifStatement");
    }

    void parseStatements() {
        printNonTerminal("statements");

        while (peekToken.type == WHILE ||
                peekToken.type == IF ||
                peekToken.type == LET ||
                peekToken.type == DO ||
                peekToken.type == RETURN) {
            parseStatement();
        }

        printNonTerminal("/statements");
    }

    void parseStatement() {
        switch (peekToken.type) {
            case LET:
                parseLet();
                break;
            case WHILE:
                parseWhile();
                break;
            case IF:
                parseIf();
                break;
            case DO:
                parseDo();
                break;
            case RETURN:
                parseReturn();
                break;
            default:
                throw new Error("Syntax error - expected a statement");
        }
    }

    void parseReturn() {
        printNonTerminal("returnStatement");

        expectPeek(RETURN);
        if (!peekTokenIs(SEMICOLON)) { 
            parseExpression();
        }
        expectPeek(SEMICOLON);

        printNonTerminal("/returnStatement");
    }

    int parseExpressionList() {
        printNonTerminal("expressionList");

        var nArgs = 0;

        if (!peekTokenIs(RPAREN)){ 
            parseExpression();
            nArgs = 1;
        }

        
        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            parseExpression();
            nArgs++;
        }

        printNonTerminal("/expressionList");
        return nArgs;

    }

    void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (isOperator(peekToken.type)) {
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
    }

    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
            case INT:
                expectPeek(INT);
                break;
            case STRING:
                expectPeek(STRING);
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(FALSE, NULL, TRUE);
                break;
            case THIS:
                expectPeek(THIS);
                break;
            case IDENTIFIER:
                expectPeek(IDENTIFIER);
                if (peekTokenIs(LPAREN) || peekTokenIs(DOT)) {
                    parseSubroutineCall();
                } else { 
                    if (peekTokenIs(LBRACKET)) { 
                        expectPeek(LBRACKET);
                        
                        parseExpression();

                        expectPeek(RBRACKET);
        
                    }
                }
                break;
            case LPAREN:
                expectPeek(LPAREN);
                parseExpression();
                expectPeek(RPAREN);
                break;
            case MINUS:
            case NOT:
                expectPeek(MINUS, NOT);
                parseTerm();

    
                break;
            default:
                throw error(peekToken, "term expected");
        }

        printNonTerminal("/term");
    }


    public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }


    boolean peekTokenIs(TokenType type) { 
        return peekToken.type == type;
    }

    boolean currentTokenIs(TokenType type) { 
        return currentToken.type == type;
    }

    private void expectPeek(TokenType... types) { 
        for (TokenType type : types) {
            if (peekToken.type == type) { 
                expectPeek(type);
                return;
            }
        }

       throw error(peekToken, "Expected a statement");

    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) { 
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            throw error(peekToken, "Expected "+type.name());
        }
    }


    private static void report(int line, String where,
        String message) {
            System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
    }


    private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.value() + "'", message);
        }
        return new ParseError();
    }
}


