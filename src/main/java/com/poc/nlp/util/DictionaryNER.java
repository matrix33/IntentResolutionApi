package com.poc.nlp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.poc.nlp.bean.Annotation;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;
import opennlp.tools.util.StringList;

public class DictionaryNER {
	

	private static String DICTIONARIES = "/dictionaries";
    private static List<DictionaryNameFinder> finders;

    public DictionaryNER() {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

        // Get the directory with the dictionary files
        String currentDir = System.getProperty("user.dir");
        System.out.println("currentDir :"+ currentDir);
        Path directory = Paths.get(currentDir, DICTIONARIES);
        if (!Files.exists(directory))
            throw new RuntimeException("Directory '" + DICTIONARIES + "' not found.");

        finders = new LinkedList<>();
        File[] files = new File(directory.toString()).listFiles();
        for (File file : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                // Create a list with a dictionary for each file
                Dictionary dictionary = new Dictionary();
                for (String line; (line = br.readLine()) != null; ) {
                    dictionary.put(new StringList(tokenizer.tokenize(line)));
                }
                String[] parts = file.toString().split("/");
                String[] fileName = parts[parts.length - 1].split("[.]");
                String type = fileName[0];
                // Use the file name to tag tokens
                finders.add(new DictionaryNameFinder(dictionary, type));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	
	public List<Annotation> find(String[] tokens) {
		System.out.println("Inside find : tokens :" + Arrays.toString(tokens));
        List<Annotation> annotations = new ArrayList<>();
        List<Span> foundSpans = new ArrayList<>();
        System.out.println("Inside find : finders :" + finders.size());
        for (DictionaryNameFinder finder : finders) {
            Span[] spans = finder.find(tokens);
            System.out.println("spans :: "+spans.length);
            for (Span span : spans) {
                foundSpans.add(span);
            }
        }

        Collections.sort(foundSpans, new Comparator<Span>() {
            @Override
            public int compare(Span o1, Span o2) {
                return o1.compareTo(o2);
            }
        });
        System.out.println("Inside find : foundSpans :" + foundSpans.size());
        for (Span span : foundSpans) {
            int start = span.getStart();
            int end = span.getEnd();
            String type = span.getType();
            String[] foundTokens = Arrays.copyOfRange(tokens, start, end);
            annotations.add(new Annotation(foundTokens, span));
        }

        return annotations;
    }


}
