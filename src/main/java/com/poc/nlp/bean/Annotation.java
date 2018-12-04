package com.poc.nlp.bean;

import opennlp.tools.util.Span;

public class Annotation {
	private String[] tokens;
    private Span span;

    public Annotation(String[] tokens, Span span) {
        this.tokens = tokens;
        this.span = span;
    }

    public String[] getTokens() {
        return tokens;
    }

    public Span getSpan() {
        return span;
    }
}
