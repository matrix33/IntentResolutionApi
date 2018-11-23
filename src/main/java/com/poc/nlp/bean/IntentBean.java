package com.poc.nlp.bean;

import java.util.ArrayList;

public class IntentBean {

	private String intent;
	private String desc;
	private ArrayList<String> paramList;
	

	@Override
	public String toString() {
		return "IntentBean [intent=" + intent + ", desc=" + desc + ", paramList=" + paramList + "]";
	}
	public String getIntent() {
		return intent;
	}
	public void setIntent(String intent) {
		this.intent = intent;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public ArrayList<String> getParamList() {
		return paramList;
	}
	public void setParamList(ArrayList<String> paramList) {
		this.paramList = paramList;
	}
}
