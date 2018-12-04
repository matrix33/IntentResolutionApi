package com.poc.nlp.bean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntentMap {
    String name;
    Map<String, List<String>> entities;
    Map<String,List<String>>  dictionaryWords;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, List<String>> getEntities() {
		return entities;
	}

	public void setEntities(Map<String, List<String>> entities) {
		this.entities = entities;
	}

	public IntentMap(String name) {
        this.name = name;
        this.entities = new HashMap<String, List<String>>();
    }

	public Map<String, List<String>> getDictionaryWords() {
		return dictionaryWords;
	}

	public void setDictionaryWords(Map<String, List<String>> dictionaryWords) {
		this.dictionaryWords = dictionaryWords;
	}

	@Override
	public String toString() {
		return "IntentMap [name=" + name + ", entities=" + entities + ", dictionaryWords=" + dictionaryWords + "]";
	}

}