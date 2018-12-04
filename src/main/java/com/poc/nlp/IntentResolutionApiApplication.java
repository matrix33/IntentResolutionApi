package com.poc.nlp;

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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.poc.nlp.trainingToolkit.IntentDocumentSampleStream;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ObjectStreamUtils;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.StringList;
import opennlp.tools.util.TrainingParameters;

@SpringBootApplication
public class IntentResolutionApiApplication  implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(IntentResolutionApiApplication.class, args);
	}

	public static DocumentCategorizerME categorizer;
    public static NameFinderME[] nameFinderMEs;
    public static HashMap<String,List<String>> dictionaryMap;
    private static String DICTIONARIES = "/dictionaries";
 
    @Override
    public void run(String...args) throws Exception {
    	
    	String jarPath = IntentResolutionApiApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    	train(jarPath);
    }
    
    public static void train(String jarPath)  throws Exception{
    	
    	String[] args1= {jarPath.trim()+File.separator+"train-flight","something"};
    	System.out.println("jarPath : "+jarPath);
    	System.out.println("training dir : "+args1[0]);
    	//URL url = IntentResolutionApiApplication.class.getResource("test.txt");
    	//System.out.println(url.getPath());
        File trainingDirectory = new File(args1[0]);
        String[] slots = new String[0];
        if (args1.length > 1) {
            slots = args1[1].split(",");
        }

        if (!trainingDirectory.isDirectory()) {
            throw new IllegalArgumentException("TrainingDirectory is not a directory: " + trainingDirectory.getAbsolutePath());
        }

        List<ObjectStream<DocumentSample>> categoryStreams = new ArrayList<ObjectStream<DocumentSample>>();
        for (File trainingFile : trainingDirectory.listFiles()) {
            String intent = trainingFile.getName().replaceFirst("[.][^.]+$", "");
            ObjectStream<String> lineStream = new PlainTextByLineStream(new MarkableFileInputStreamFactory(trainingFile), "UTF-8");
            ObjectStream<DocumentSample> documentSampleStream = new IntentDocumentSampleStream(intent, lineStream);
            categoryStreams.add(documentSampleStream);
        }

        ObjectStream<DocumentSample> combinedDocumentSampleStream = ObjectStreamUtils.concatenateObjectStream(categoryStreams);

        TrainingParameters trainingParams = new TrainingParameters();
        trainingParams.put(TrainingParameters.ITERATIONS_PARAM, 10);
        trainingParams.put(TrainingParameters.CUTOFF_PARAM, 0);

        DoccatModel doccatModel = DocumentCategorizerME.train("en", combinedDocumentSampleStream, trainingParams, new DoccatFactory());
        combinedDocumentSampleStream.close();

        List<TokenNameFinderModel> tokenNameFinderModels = new ArrayList<TokenNameFinderModel>();

        for (String slot : slots) {
        	//System.out.println("slot :: "+slot);
            List<ObjectStream<NameSample>> nameStreams = new ArrayList<ObjectStream<NameSample>>();
            for (File trainingFile : trainingDirectory.listFiles()) {
                ObjectStream<String> lineStream = new PlainTextByLineStream(new MarkableFileInputStreamFactory(trainingFile), "UTF-8");
                ObjectStream<NameSample> nameSampleStream = new NameSampleDataStream(lineStream);
                nameStreams.add(nameSampleStream);
            }
            ObjectStream<NameSample> combinedNameSampleStream = ObjectStreamUtils.concatenateObjectStream(nameStreams);

            TokenNameFinderModel tokenNameFinderModel = NameFinderME.train("en", null, combinedNameSampleStream, trainingParams, new TokenNameFinderFactory());
            combinedNameSampleStream.close();
            tokenNameFinderModels.add(tokenNameFinderModel);
        }


        categorizer = new DocumentCategorizerME(doccatModel);
        nameFinderMEs = new NameFinderME[tokenNameFinderModels.size()];
        for (int i = 0; i < tokenNameFinderModels.size(); i++) {
            nameFinderMEs[i] = new NameFinderME(tokenNameFinderModels.get(i));
        }
        System.out.println(nameFinderMEs.length + "  After training  : "+Arrays.toString(nameFinderMEs));
        System.out.println("Training complete. Ready.");
        //*****************************************************custom dictionary code*********************************************************//
        String currentDir = System.getProperty("user.dir");
        System.out.println("currentDir :"+ currentDir);
        Path directory = Paths.get(currentDir, DICTIONARIES);
        if (!Files.exists(directory))
            throw new RuntimeException("Directory '" + DICTIONARIES + "' not found.");
        File[] files = new File(directory.toString()).listFiles();
        List<String> mapList;
/*        System.out.println("files size : "+files.length);
*/        for (File file : files) {
        	/* System.out.println("file : "+file.getName());*/
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            	dictionaryMap=new HashMap<>();
                for (String line; (line = br.readLine()) != null; ) {
                	System.out.println("dictionaryMap :: " + line);
                	mapList= Arrays.asList(line.split("=")[1].split(","));
                	/*System.out.println("mapList : "+mapList);
                	System.out.println("line.split(\"=\")[0] : "+(line.split("=")[0]));*/
                	dictionaryMap.put(line.split("=")[0],mapList);
                }
                System.out.println("dictionaryMap  :: "+dictionaryMap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        /*BufferedReader br = new BufferedReader(new FileReader(file));
    	
    	String line = br.readLine();
    	int count=0;
		while (line != null) {
			mapList=(ArrayList<String>) Arrays.asList(line.split(","));
        	dictionaryMap.put(line.split("="),mapList);
			System.out.println(line);
			// read next line
			line = br.readLine();
			count++;
		}
		br.close();*/
        
      //*****************************************************custom dictionary code*********************************************************//
    }
}


