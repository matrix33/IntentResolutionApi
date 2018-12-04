package com.poc.nlp.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.poc.nlp.IntentResolutionApiApplication;
import com.poc.nlp.bean.Annotation;
import com.poc.nlp.bean.IntentBean;
import com.poc.nlp.bean.IntentMap;

import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ObjectStreamUtils;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;



@RestController
@RequestMapping(value = "/nlp/intent")
public class IntentController {
	
	@Value(value = "classpath:models/en-token.bin")
	private Resource token;
	
	@Value(value = "classpath:models/en-pos-maxent.bin")
	private Resource postoken;
	
	@Value(value = "classpath:models/en-lemmatizer.dict")
	private Resource lemmatoken;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	public ResponseEntity insertIntentSample(@RequestBody IntentBean intentBean){
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			String jarPath = IntentResolutionApiApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			File trainingDirectory =  new File(jarPath.trim()+File.separator+"train-flight");
			System.out.println("trainingDirectory :: "+trainingDirectory);
			Boolean flag=false;
			if (!trainingDirectory.isDirectory()) {
	            throw new IllegalArgumentException("TrainingDirectory is not a directory: " + trainingDirectory.getAbsolutePath());
	        }
			for (File trainingFile : trainingDirectory.listFiles()) {
				String intentFile = trainingFile.getName().replaceFirst("[.][^.]+$", "");
				if(null!=intentBean.getIntent() && intentBean.getIntent().equalsIgnoreCase(intentFile))
				{
					System.out.println("intentFile :: "+intentFile);
					return new ResponseEntity("IntentFileAlreadyExists!", HttpStatus.BAD_REQUEST);
				}
				else if(null!=intentBean.getIntent() && null!=intentBean.getDesc())
				{
				}
	        }
			if(flag==false)
			{
				fw = new FileWriter(trainingDirectory.getPath()+"/"+intentBean.getIntent()+".txt");
				bw = new BufferedWriter(fw);
				bw.write(intentBean.getDesc());
				IntentResolutionApiApplication.train(jarPath);
				return new ResponseEntity("Intent File Created Successfully!", HttpStatus.OK);
			}
		}catch (IOException e) {
			return new ResponseEntity("FileNotFoundException", HttpStatus.BAD_REQUEST);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return new ResponseEntity("Exception while training!", HttpStatus.BAD_REQUEST);
		}finally {

			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {

				ex.printStackTrace();

			}

		}
		return new ResponseEntity("Exception!", HttpStatus.BAD_REQUEST);

    }
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@RequestMapping(value = "/update", method = RequestMethod.POST)
	public ResponseEntity updateIntentSample(@RequestBody IntentBean intentBean){
		try {
			String jarPath = IntentResolutionApiApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			System.out.println(intentBean);
			File trainingDirectory =  new File(jarPath.trim()+File.separator+"train-flight");
			System.out.println("trainingDirectory :: "+trainingDirectory);
			Boolean flag=false;
			if (!trainingDirectory.isDirectory()) {
	            throw new IllegalArgumentException("TrainingDirectory is not a directory: " + trainingDirectory.getAbsolutePath());
	        }
			for (File trainingFile : trainingDirectory.listFiles()) {
				String intentFile = trainingFile.getName().replaceFirst("[.][^.]+$", "");
				if(null!=intentBean.getIntent() && intentBean.getIntent().equalsIgnoreCase(intentFile))
				{
					System.out.println("intentFile :: "+intentFile);
					 Files.write(trainingFile.toPath(), (System.lineSeparator() + intentBean.getDesc()).getBytes(), StandardOpenOption.APPEND);
					 flag=true;
					 IntentResolutionApiApplication.train(jarPath);
					 return new ResponseEntity("Intent file updated successfully", HttpStatus.OK);
				}
				else if(null!=intentBean.getIntent() && null!=intentBean.getDesc())
				{
				}
	        }
			if(flag==false)
			{
				return new ResponseEntity("Intent File Not Found", HttpStatus.BAD_REQUEST);
			}
		}catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity("FileNotFoundException", HttpStatus.BAD_REQUEST);
			
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity("Exception while training!", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity("some exception", HttpStatus.BAD_REQUEST);

    }
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping(value = "/get", method = RequestMethod.POST)
	public ResponseEntity getIntent(@RequestBody IntentBean intentBean){
		String textInput = intentBean.getDesc();
		 InputStream modelIn;
		 IntentMap intent = null;
		try {
			List<String> prepList = null;
			modelIn = token.getInputStream();
			TokenizerModel model = new TokenizerModel(modelIn);
	        Tokenizer tokenizer = new TokenizerME(model);
	        String[] tokens = tokenizer.tokenize(textInput);
	        double[] outcome = IntentResolutionApiApplication.categorizer.categorize(tokens);
	        System.out.println("tokens :: "+Arrays.toString(tokens));
	        intent = new IntentMap(IntentResolutionApiApplication.categorizer.getBestCategory(outcome));
	        prepList = getPOS(tokens);
	        if(null!=prepList)
            {
	        System.out.println("prepList :: "+prepList.toString());
            }
	        else
	        System.out.println("prepList :: null");
	        //////////////////////////
	        String jarPath = IntentResolutionApiApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			String trainingDirectoryPath =  jarPath.trim()+File.separator+"train-flight";
	        File trainingFile = new File(trainingDirectoryPath+File.separator+intent.getName()+".txt");
	        
	        
	        System.out.println("Dictionary start*******************");
			/*DictionaryNER ner = new DictionaryNER();
	        SimpleTokenizer tokenizerDictionary = SimpleTokenizer.INSTANCE;
	        List<Annotation> annotations = ner.find(tokenizerDictionary.tokenize(textInput));
	        System.out.println(annotations.size());
	        List<String> dictionaryWords = new ArrayList();
	        for (Annotation annotation : annotations) {
	            for (String token : annotation.getTokens()) {
	                //System.out.printf("%s ", token);
	                dictionaryWords.add(token);
	            }
	            Span span = annotation.getSpan();
	            System.out.println("dictionaryWords : "+dictionaryWords);
	            //System.out.printf("[%d..%d) %s\n", span.getStart(), span.getEnd(), span.getType());
	        }*/
	        
	        
	        String[] tokenDicts = WhitespaceTokenizer.INSTANCE.tokenize(textInput);
	       // List<Annotation> annotations = ner.find(tokenizerDictionary.tokenize(textInput));
	      //  System.out.println(annotations.size());
	        Map<String,List<String>> dictionaryWordsMap = new HashMap<>();
	        System.out.println("tokenDicts :: "+Arrays.toString(tokenDicts) +" :: "+IntentResolutionApiApplication.dictionaryMap.size());
	        for (String tokenDict : tokenDicts) {
	            for(Map.Entry<String, List<String>> entry:IntentResolutionApiApplication.dictionaryMap.entrySet())
	            {
	            	System.out.println("entry :: "+entry.getKey());
	            	System.out.println("value :: "+entry.getValue());
	            	if(entry.getValue().contains(tokenDict))
	            	{
	            		System.out.println("contains :: "+entry.getKey());
	            		if(!dictionaryWordsMap.containsKey(entry.getKey()))
	            		{
	            			ArrayList<String> list = new ArrayList<>();
	            			list.add(tokenDict);
	            			dictionaryWordsMap.put(entry.getKey(),list);
	            		}
	            		else
	            		{
	            			dictionaryWordsMap.get(entry.getKey()).add(tokenDict);
	            		}
	            	}
	            }
	            //System.out.printf("[%d..%d) %s\n", span.getStart(), span.getEnd(), span.getType());
	        }
	        System.out.println("dictionaryWords : "+dictionaryWordsMap);
	        System.out.println("Dictionary end*******************");
	       //***********************************************************************************************************
	        /*System.out.println("trainingFile :: "+trainingFile);
	        TrainingParameters trainingParams = new TrainingParameters();
	        trainingParams.put(TrainingParameters.ITERATIONS_PARAM, 10);
	        trainingParams.put(TrainingParameters.CUTOFF_PARAM, 0);

	        List<TokenNameFinderModel> tokenNameFinderModels = new ArrayList<TokenNameFinderModel>();

	            List<ObjectStream<NameSample>> nameStreams = new ArrayList<ObjectStream<NameSample>>();
	                ObjectStream<String> lineStream = new PlainTextByLineStream(new MarkableFileInputStreamFactory(trainingFile), "UTF-8");
	                ObjectStream<NameSample> nameSampleStream = new NameSampleDataStream(lineStream);
	                nameStreams.add(nameSampleStream);
	            ObjectStream<NameSample> combinedNameSampleStream = ObjectStreamUtils.concatenateObjectStream(nameStreams);

	            TokenNameFinderModel tokenNameFinderModel = NameFinderME.train("en", null, combinedNameSampleStream, trainingParams, new TokenNameFinderFactory());
	            combinedNameSampleStream.close();
	            tokenNameFinderModels.add(tokenNameFinderModel);
	            
	            NameFinderME[] nameFinderMEs = new NameFinderME[tokenNameFinderModels.size()];
		        for (int i = 0; i < tokenNameFinderModels.size(); i++) {
		            nameFinderMEs[i] = new NameFinderME(tokenNameFinderModels.get(i));
		        }
		        */
	        //***********************************************************************************************************

	       
	        
	        
	        /*if(null!= dictionaryWords && dictionaryWords.size()>0)
	        {*/
		        System.out.println("trainingFile : "+trainingFile.getPath());
		        for (NameFinderME nameFinderME : IntentResolutionApiApplication.nameFinderMEs) {
		            Span[] spans = nameFinderME.find(tokens);
		            System.out.println("spans.length :"+spans.length);
		            String[] names = Span.spansToStrings(spans, tokens);
		            System.out.println("names before :: "+Arrays.toString(names));
		            ArrayList<String> nameList = new ArrayList<String>();
		            ArrayList<Span> spanList = new ArrayList<Span>();
		            if(null!=prepList)
		            {
		            	for (int i = 0; i < names.length; i++) 
		            	{
		            		if(prepList.contains(names[i]))
		            		{
		            			System.out.println("prep : "+names[i]);
		            		}
		            		else 
		            		{
		            			nameList.add(names[i]);
		            			spanList.add(spans[i]);
		            			System.out.println("not prep : "+names[i]);
		            		}
		            	}
		            }
		            System.out.println("names :: "+nameList);
		            for (int i = 0; i < nameList.size(); i++) {
		            	if(!dictionaryWordsMap.containsKey(spanList.get(i).getType())){
			            	if(intent.getEntities().containsKey(spanList.get(i).getType()))
			            		intent.getEntities().get(spanList.get(i).getType()).add(nameList.get(i));
			            	else
			            	{
			            		ArrayList<String> list= new ArrayList<>();
			            		list.add(nameList.get(i));
			            		intent.getEntities().put(spanList.get(i).getType(), list);
			            	}
			            	System.out.println(nameList.get(i)+" : "+spanList.get(i).getType());
		            	}
		            }
		           intent.setDictionaryWords(dictionaryWordsMap);
		        }
	        /*}
	        else
	        {
	        	return new ResponseEntity("No Catch Phrase found", HttpStatus.BAD_REQUEST);
	        }*/
		} catch (FileNotFoundException e) {
			return new ResponseEntity("FileNotFoundException", HttpStatus.BAD_REQUEST);
		} catch (IOException e) {
			return new ResponseEntity("FileNotFoundException", HttpStatus.BAD_REQUEST);
		}
	        
        return new ResponseEntity(intent, HttpStatus.OK);
    }
	
	
	@RequestMapping(value = "/getAllIntentNames", method = RequestMethod.GET)
	public ResponseEntity getAllIntent(){
		List<String> intentList = new ArrayList<>();
		try {
		String jarPath = IntentResolutionApiApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		File trainingDirectory =  new File(jarPath.trim()+File.separator+"train-flight");
		System.out.println("trainingDirectory :: "+trainingDirectory);
		Boolean flag=false;
		if (!trainingDirectory.isDirectory()) {
		    throw new IllegalArgumentException("TrainingDirectory is not a directory: " + trainingDirectory.getAbsolutePath());
		}
		for (File trainingFile : trainingDirectory.listFiles()) {
			intentList.add(trainingFile.getName().substring(0,trainingFile.getName().length()-4));
		}
		if(intentList.isEmpty())
			return new ResponseEntity("No Intent Present", HttpStatus.OK);
		}catch (Exception e) {
			 return new ResponseEntity("Exception", HttpStatus.BAD_REQUEST);
		}
        return new ResponseEntity(intentList, HttpStatus.OK);
    }
	
	
	public NameFinderME[] getNameFinderME(File trainingFiles) throws FileNotFoundException, IOException
	{
		TrainingParameters trainingParams = new TrainingParameters();
        trainingParams.put(TrainingParameters.ITERATIONS_PARAM, 10);
        trainingParams.put(TrainingParameters.CUTOFF_PARAM, 0);
        
		List<TokenNameFinderModel> tokenNameFinderModels = new ArrayList<TokenNameFinderModel>();
        List<ObjectStream<NameSample>> nameStreams = new ArrayList<ObjectStream<NameSample>>();
        for (File trainingFile : trainingFiles.listFiles()) {
        	System.out.println("trainingFile.getName() :: "+trainingFile.getName());
			/*if(trainingFile.getName().equalsIgnoreCase("flightCityToCity.txt")) {*/
        	ObjectStream<String> lineStream = new PlainTextByLineStream(new MarkableFileInputStreamFactory(trainingFile), "UTF-8");
		        ObjectStream<NameSample> nameSampleStream = new NameSampleDataStream(lineStream);
		        nameStreams.add(nameSampleStream);
			//}
			
			System.out.println("flightCityToCity-*------------------------");
        }
        /*ObjectStream<String> lineStream = new PlainTextByLineStream(new MarkableFileInputStreamFactory(trainingFile), "UTF-8");
        ObjectStream<NameSample> nameSampleStream = new NameSampleDataStream(lineStream);
        nameStreams.add(nameSampleStream);*/
        ObjectStream<NameSample> combinedNameSampleStream = ObjectStreamUtils.concatenateObjectStream(nameStreams);
        
        TokenNameFinderModel tokenNameFinderModel = NameFinderME.train("en", null, combinedNameSampleStream, trainingParams, new TokenNameFinderFactory());
        combinedNameSampleStream.close();
        tokenNameFinderModels.add(tokenNameFinderModel);
        
        NameFinderME[] nameFinderMEs = new NameFinderME[tokenNameFinderModels.size()];
        System.out.println("tokenNameFinderModels.size() : "+tokenNameFinderModels.size());
	    for (int i = 0; i < tokenNameFinderModels.size(); i++) {
	        nameFinderMEs[i] = new NameFinderME(tokenNameFinderModels.get(i));
	    }
	    
		return nameFinderMEs;
	}
	
	public List<String> getPOS(String[] tokens)
	{
		List<String> prepList = null;
		try {
			prepList = new ArrayList<>();
			InputStream inputStream =postoken.getInputStream(); 
	      POSModel model = new POSModel(inputStream); 
	      POSTaggerME tagger = new POSTaggerME(model); 
	      String[] tags = tagger.tag(tokens);
	      for (int i = 0; i < tokens.length; i++) {
                String word = tokens[i].trim();
                String tag = tags[i].trim();
                if(tag.equalsIgnoreCase("CC") || tag.equalsIgnoreCase("IN") || tag.equalsIgnoreCase("TO") 
                		|| tag.equalsIgnoreCase("RB")|| tag.equalsIgnoreCase("RBR")|| tag.equalsIgnoreCase("RBS")
                		|| tag.equalsIgnoreCase("RP")|| tag.equalsIgnoreCase("UH")|| tag.equalsIgnoreCase("WDT")
                		|| tag.equalsIgnoreCase("WP")|| tag.equalsIgnoreCase("WP$")|| tag.equalsIgnoreCase("WRB")
                		|| tag.equalsIgnoreCase("PRP")|| tag.equalsIgnoreCase("PRP$")|| tag.equalsIgnoreCase("WRB"))
                {
                	prepList.add(word);
                }
            }
	    InputStream dictLemmatizer = lemmatoken.getInputStream();
  	    DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(dictLemmatizer);
  	    String[] lemmas = lemmatizer.lemmatize(tokens, tags);
  	     for(String lemma:lemmas)
  	     {
  	    	/* System.out.println("lemma : "+lemma);*/
  	    	 if(!lemma.equals("0")&&!prepList.contains(lemma))
  	    	 {
  	    		prepList.add(lemma);
  	    	 }
  	     }
		} catch (FileNotFoundException e) {
			prepList = null;
			e.printStackTrace();
		} catch (IOException e) {
			prepList = null;
			e.printStackTrace();
		} 
		return prepList;
	}
	
	
	

}
