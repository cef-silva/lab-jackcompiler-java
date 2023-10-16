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

    // classdef -> 'class' className '{' classVarDec* subroutineDec* '}'
    void parseClass() {
        printNonTerminal("class");
   
        expectPeek(TokenType.CLASS);
        expectPeek(TokenType.IDENTIFIER);
        expectPeek(TokenType.LBRACE);
        while (peekToken.type == FIELD || peekToken.type == STATIC) {
            parseClassVarDec();
        }
        parseSubroutineDec();

        while (peekToken.type == CONSTRUCTOR || peekToken.type == FUNCTION || peekToken.type == METHOD){
            parseSubroutineDec();
        }
        expectPeek(TokenType.RBRACE);

        printNonTerminal("/class");
    }

    void parseSubroutineCall() {

        var nArgs = 0;
        printNonTerminal("/varDec");
    }

    void parseDo() {
        printNonTerminal("doStatement");
    

        expectPeek(DO);
        expectPeek(IDENTIFIER);
        parseSubroutineCall();
        expectPeek(SEMICOLON);

        printNonTerminal("/doStatement");
    }
    


    void parseClassVarDec() {
        printNonTerminal("classVarDec");

        expectPeek(FIELD, STATIC);
        // 'Int' || 'Char' || 'Boolean' || className
        expectPeek(INT, CHAR, BOOLEAN,IDENTIFIER);
        expectPeek(TokenType.IDENTIFIER);
        while (peekToken.type == TokenType.COMMA) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENTIFIER);
        }
        expectPeek(TokenType.SEMICOLON);

        printNonTerminal("/classVarDec");
    }

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



    // //integerConstant | stringConstant | keywordConstant | varName | varName '[' expression ']' | subroutineCall | '(' expression ')' | unaryOp term
    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
            case INTEGER:
                expectPeek(INTEGER);
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
                } else { // variavel comum ou array
                    if (peekTokenIs(LBRACKET)) { // array
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

    // funções auxiliares
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
