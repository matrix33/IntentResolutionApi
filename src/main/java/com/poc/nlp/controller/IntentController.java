package com.poc.nlp.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.poc.nlp.IntentResolutionApiApplication;
import com.poc.nlp.bean.IntentBean;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;



@RestController
@RequestMapping(value = "/intentRecognition")
public class IntentController {
	
	@Value(value = "classpath:models/en-token.bin")
	private Resource token;
	
	/*@Value(value = "D:\\training-models\\train-flight")
	private Resource trainingFile;*/
	
	@RequestMapping(value = "/insertIntentSample/{intentName}/{intentDesc}", method = RequestMethod.GET)
	public ResponseEntity insertIntentSample(@PathVariable String intentName, @PathVariable String intentDesc/*@RequestBody IntentBean intentBean*/){
		try {
			IntentBean intentBean = new IntentBean();
			intentBean.setIntent(intentName);
			intentBean.setDesc(intentDesc);
			System.out.println(intentBean);
			File trainingDirectory =  new File("D://training-models/train-flight");
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
					 return new ResponseEntity("added successfully", HttpStatus.OK);
				}
				else if(null!=intentBean.getIntent() && null!=intentBean.getDesc())
				{
				}
	        }
			if(flag==false)
			{
				ArrayList<String> lines=new ArrayList<>();
				lines.add(intentBean.getDesc());
				Path file = Paths.get(trainingDirectory.getPath()+"/"+intentBean.getIntent()+".txt");
				Files.write(file, lines, Charset.forName("UTF-8"));
				return new ResponseEntity("Success", HttpStatus.OK);
			}
		}catch (IOException e) {
			return new ResponseEntity("FileNotFoundException", HttpStatus.BAD_REQUEST);
			
		}
		return new ResponseEntity("some exception", HttpStatus.BAD_REQUEST);

    }
	
	@RequestMapping(value = "/searchIntent/{textInput}", method = RequestMethod.GET)
	public ResponseEntity searchIntent(@PathVariable String textInput){
		 InputStream modelIn;
		 IntentBean bean = new IntentBean();
		ArrayList<String> paramList=new ArrayList<>();
		try {
			modelIn = token.getInputStream();
			TokenizerModel model = new TokenizerModel(modelIn);
	        Tokenizer tokenizer = new TokenizerME(model);
	            double[] outcome = IntentResolutionApiApplication.categorizer.categorize(tokenizer.tokenize(textInput));
	            bean.setIntent(IntentResolutionApiApplication.categorizer.getBestCategory(outcome));
	            String[] tokens = tokenizer.tokenize(textInput);
	            for (NameFinderME nameFinderME : IntentResolutionApiApplication.nameFinderMEs) {
	                Span[] spans = nameFinderME.find(tokens);
	                String[] names = Span.spansToStrings(spans, tokens);
	                for (int i = 0; i < spans.length; i++) {
	                    if(i > 0) {}
	                    paramList.add(names[i]);
	                }
	            }
	            bean.setParamList(paramList);
		} catch (FileNotFoundException e) {
			return new ResponseEntity("FileNotFoundException", HttpStatus.BAD_REQUEST);
		} catch (IOException e) {
			return new ResponseEntity("FileNotFoundException", HttpStatus.BAD_REQUEST);
		}
	        
        return new ResponseEntity(bean, HttpStatus.OK);
    }

}
