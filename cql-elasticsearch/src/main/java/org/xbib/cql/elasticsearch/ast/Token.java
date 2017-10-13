package org.xbib.cql.elasticsearch.ast;

import org.xbib.cql.elasticsearch.Visitor;
import org.xbib.cql.util.QuotedStringTokenizer;
import org.xbib.cql.util.UnterminatedQuotedStringException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Elasticsearch query tokens.
 */
public class Token implements Node {

    private static final Pattern word = Pattern.compile("[\\P{IsWord}]");

    private TokenType type;

    private String value;

    private String stringvalue;

    private Boolean booleanvalue;

    private Long longvalue;

    private Double doublevalue;

    private ZonedDateTime datevalue;

    private List<String> values;

    private final EnumSet<TokenClass> tokenClass;

    public Token(String value) {
        this.value = value;
        this.tokenClass = EnumSet.of(TokenClass.NORMAL);
        this.type = TokenType.STRING;
        // if this string is equal to true/false or on/off or yes/no, convert silently to bool
        if (value.equals("true") || value.equals("yes") || value.equals("on")) {
            this.booleanvalue = true;
            this.value = null;
            this.type = TokenType.BOOL;

        } else if (value.equals("false") || value.equals("no") || value.equals("off")) {
            this.booleanvalue = false;
            this.value = null;
            this.type = TokenType.BOOL;

        }
        if (this.value != null) {
            // quoted?
            if (value.startsWith("\"") && value.endsWith("\"")) {
                this.stringvalue = value;
                this.value = value.substring(1, value.length() - 1).replaceAll("\\\\\"", "\"");
                this.values = parseQuot(this.value);
                tokenClass.add(TokenClass.QUOTED);
            }
            // wildcard?
            if (this.value.indexOf('*') >= 0 || this.value.indexOf('?') >= 0) {
                tokenClass.add(TokenClass.WILDCARD);
                // all?
                if (this.value.length() == 1) {
                    tokenClass.add(TokenClass.ALL);
                }
            }
            // prefix?
            if (this.value.length() > 0 && this.value.charAt(0) == '^') {
                tokenClass.add(TokenClass.BOUNDARY);
                this.value = this.value.substring(1);
            }
        }
    }

    public Token(Boolean value) {
        this.booleanvalue = value;
        this.type = TokenType.BOOL;
        this.tokenClass = EnumSet.of(TokenClass.NORMAL);
    }

    public Token(Long value) {
        this.longvalue = value;
        this.type = TokenType.INT;
        this.tokenClass = EnumSet.of(TokenClass.NORMAL);
    }

    public Token(Double value) {
        this.doublevalue = value;
        this.type = TokenType.FLOAT;
        this.tokenClass = EnumSet.of(TokenClass.NORMAL);
    }

    public Token(ZonedDateTime value) {
        this.datevalue = value;
        // this will enforce dates to get formatted as long values (years)
        this.longvalue = (long) datevalue.getYear();
        this.type = TokenType.DATETIME;
        this.tokenClass = EnumSet.of(TokenClass.NORMAL);
    }

    /**
     * Same as toString(), but ignore string value.
     * @return a string
     */
    public String getString() {
        StringBuilder sb = new StringBuilder();
        if (booleanvalue != null) {
            sb.append(booleanvalue);
        } else if (longvalue != null) {
            sb.append(longvalue);
        } else if (doublevalue != null) {
            sb.append(doublevalue);
        } else if (datevalue != null) {
            sb.append(DateTimeFormatter.ISO_INSTANT.format(datevalue));
        } else if (value != null) {
            sb.append(value);
        }
        return sb.toString();
    }

    public Boolean getBoolean() {
        return booleanvalue;
    }

    public Long getInteger() {
        return longvalue;
    }

    public Double getFloat() {
        return doublevalue;
    }

    public ZonedDateTime getDate() {
        return datevalue;
    }

    public List<String> getStringList() {
        return values;
    }

    @Override
    public TokenType getType() {
        return type;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (booleanvalue != null) {
            sb.append(booleanvalue);
        } else if (longvalue != null) {
            sb.append(longvalue);
        } else if (doublevalue != null) {
            sb.append(doublevalue);
        } else if (datevalue != null) {
            sb.append(DateTimeFormatter.ISO_INSTANT.format(datevalue));
        } else if (stringvalue != null) {
            sb.append(stringvalue);
        } else if (value != null) {
            sb.append(value);
        }
        return sb.toString();
    }

    public boolean isQuoted() {
        return tokenClass.contains(TokenClass.QUOTED);
    }

    public boolean isBoundary() {
        return tokenClass.contains(TokenClass.BOUNDARY);
    }

    public boolean isWildcard() {
        return tokenClass.contains(TokenClass.WILDCARD);
    }

    public boolean isAll() {
        return tokenClass.contains(TokenClass.ALL);
    }

    private List<String> parseQuot(String s) {
        try {
            QuotedStringTokenizer qst = new QuotedStringTokenizer(s, " \t\n\r\f", "\"", '\\', false);
            Iterable<String> iterable = () -> qst;
            Stream<String> stream = StreamSupport.stream(iterable.spliterator(), false);
            return stream.filter(str -> !word.matcher(str).matches()).collect(Collectors.toList());
        } catch (UnterminatedQuotedStringException e) {
            return Collections.singletonList(s);
        }
    }

    /**
     * The token classes.
     */
    public enum TokenClass {

        NORMAL, ALL, WILDCARD, BOUNDARY, QUOTED
    }
}
