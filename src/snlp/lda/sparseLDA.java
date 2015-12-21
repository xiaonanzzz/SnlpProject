package snlp.lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;



public class sparseLDA
{
	public double[] alpha; // Hyper-parameter alpha
	public double[][] beta; // Hyper-parameter alpha

	public int numVocab; // Number of vocabulary
	public int numTopics; // Number of topics
	public int numDocuments; // Number of documents in the corpus
	public int numIterations; // Number of Gibbs sampling iterations
	public int numTopicWords; // Number of most probable words for each topic

	public double alphaSum; // alpha * numTopics
	public double betaSum; // beta * vocabularySize

	public List<List<Integer>> corpus; // Word ID-based corpus
	public int numWordsInCorpus; // Number of words in the corpus
	
	public List<List<Integer>> topicAssignments; // Topics assignments for words in the corpus
	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID given a word
	public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word given an ID
	
	public int vocabularySize; // The number of word types in the corpus

	public int fileCount;
	// numDocuments * numTopics matrix
	// Given a document: number of its words assigned to each topic
	// n(z|d)
	public int[][] docTopicCount;
	// Number of words in every document
	public int[] sumDocTopicCount;
	// numTopics * vocabularySize matrix
	// Given a topic: number of times a word type assigned to the topic
	// n(t|z)
	public int[][] topicWordCount;
	// Total number of words assigned to a topic
	public int[] sumTopicWordCount;

	// Double array used to sample a topic
	public double[] multiProbs; //z_d,n = k ~ multinomial

	
	// Path to the directory containing the corpus
	public String folderPath;
	public String vocabPath;
	// Path to the topic modeling corpus
	public Vector<String> corpusPath;
	public String expName = "LDAmodel";
	public String orgExpName = "LDAmodel";
	

	public sparseLDA(Vector<String> pathToCorpus,int inCount,int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName)
		throws Exception
	{
		//initial parameters
		numTopics = inNumTopics;
		numIterations = inNumIterations;
		numTopicWords = inTopWords;
		expName = inExpName;
		orgExpName = expName;
		corpusPath = pathToCorpus;
		fileCount=inCount;
		
		numDocuments=0;
		
		folderPath="/Users/QimingChen/Desktop/output/";
		
		fileCount++;
		
		//create vocabulary
		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		
		
		//read in corpus
		numWordsInCorpus = 0;
		corpus = new ArrayList<List<Integer>>();
		BufferedReader br = null;
		try {
			int indexWord = -1;
			br = new BufferedReader(new FileReader(pathToCorpus.get(0)));
			for (String doc; (doc = br.readLine()) != null;) {

				if (doc.trim().length() == 0)
					continue;

				String[] words = doc.trim().split("\\s+");
				List<Integer> document = new ArrayList<Integer>();

				for (String word : words) {
					if (!word2IdVocabulary.containsKey(word)) {
						indexWord ++;
						word2IdVocabulary.put(word, indexWord);
						id2WordVocabulary.put(indexWord, word);
						document.add(indexWord);
					}else{
						document.add(word2IdVocabulary.get(word));
					}
				}

				numDocuments++;
				numWordsInCorpus += document.size();
				corpus.add(document);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		vocabularySize = word2IdVocabulary.size(); // vocabularySize = indexWord
		
		//initialize counting maps
		docTopicCount = new int[this.numDocuments][numTopics];
		topicWordCount = new int[numTopics][vocabularySize];
		sumDocTopicCount = new int[this.numDocuments];
		sumTopicWordCount = new int[numTopics];

		//initialize probs of topics
		multiProbs = new double[numTopics];
		for (int i = 0; i < numTopics; i++) {
			//sampling from multinomial
			//all the prob should be the same, so just get 1/numTopics
			multiProbs[i] = 1.0 / numTopics;
		}
		
		//get in hyper parameters
		alpha = new double[numDocuments];
		for (int i = 0; i < alpha.length; i++) {
			alpha[i]=inAlpha;
		}
		
		beta=new double[numTopics][vocabularySize];
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				beta[i][j]=inBeta;
			}
		}
		
		//
		alphaSum = numTopics * alpha[0];
		betaSum = vocabularySize * beta[0][0];

		System.out.println("Corpus size: " + this.numDocuments + " docs, "
			+ numWordsInCorpus + " words");
		System.out.println("Vocabuary size: " + vocabularySize);
		System.out.println("Number of topics: " + numTopics);
		System.out.println("alpha: " + alpha[0]);
		System.out.println("beta: " + beta[0][0]);
		System.out.println("Number of sampling iterations: " + numIterations);
		System.out.println("Number of top topical words: " + numTopicWords);

		initialize();
	}

	/**
	 * Randomly initialize topic assignments
	 */
	public void initialize()
		throws IOException
	{
		System.out.println("Randomly initializing topic assignments ...");

		topicAssignments = new ArrayList<List<Integer>>();

		for (int i = 0; i < numDocuments; i++) {
			List<Integer> topics = new ArrayList<Integer>();
			int docSize = corpus.get(i).size();
			for (int j = 0; j < docSize; j++) {
				int topic = nextDiscrete(multiProbs); // Sample a topic
		        
				// Increase counts
				docTopicCount[i][topic] += 1;
				topicWordCount[topic][corpus.get(i).get(j)] += 1;
				sumDocTopicCount[i] += 1;
				sumTopicWordCount[topic] += 1;

				topics.add(topic);
			}
			topicAssignments.add(topics);
		}
	}

	public void newCorpus(int i,double inAlpha, double inBeta) {
		numWordsInCorpus = 0;
		corpus = new ArrayList<List<Integer>>();
		numDocuments=0;
		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		fileCount++;
		
		BufferedReader br = null;
		try {
			int indexWord = -1;
			br = new BufferedReader(new FileReader(corpusPath.get(i)));
			for (String doc; (doc = br.readLine()) != null;) {

				if (doc.trim().length() == 0)
					continue;

				String[] words = doc.trim().split("\\s+");
				List<Integer> document = new ArrayList<Integer>();

				for (String word : words) {
					if (!word2IdVocabulary.containsKey(word)) {
						indexWord++;
						word2IdVocabulary.put(word, indexWord);
						id2WordVocabulary.put(indexWord, word);
						document.add(indexWord);
					}else{
						document.add(indexWord);
					}
				}
				numDocuments++;
				numWordsInCorpus += document.size();
				corpus.add(document);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Corpus size: " + numDocuments + " docs, "
				+ numWordsInCorpus + " words");
		
		
		updatePara(inAlpha,inBeta);
		
	}
	
	public void updatePara(double inAlpha,double inBeta){
		//initial parameters
				
		//create vocabulary
	
		//read in corpus
		
		vocabularySize = word2IdVocabulary.size(); // vocabularySize = indexWord
		
		//initialize counting maps
		docTopicCount = new int[this.numDocuments][numTopics];
		topicWordCount = new int[numTopics][vocabularySize];
		sumDocTopicCount = new int[this.numDocuments];
		sumTopicWordCount = new int[numTopics];

		//initialize probs of topics
		multiProbs = new double[numTopics];
		for (int i = 0; i < numTopics; i++) {
			//sampling from multinomial
			//all the prob should be the same, so just get 1/numTopics
			multiProbs[i] = 1.0 / numTopics;
		}
		
		//get in hyper parameters
		alpha = new double[numDocuments];
		for (int i = 0; i < alpha.length; i++) {
			alpha[i]=inAlpha;
		}
		
		beta=new double[numTopics][vocabularySize];
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				beta[i][j]=inBeta;
			}
		}
		
		//
		alphaSum = numTopics * alpha[0];
		betaSum = vocabularySize * beta[0][0];

		System.out.println("Corpus size: " + this.numDocuments + " docs, "
			+ numWordsInCorpus + " words");
		System.out.println("Vocabuary size: " + vocabularySize);
		System.out.println("Number of topics: " + numTopics);
		System.out.println("alpha: " + alpha[0]);
		System.out.println("beta: " + beta[0][0]);
		System.out.println("Number of sampling iterations: " + numIterations);
		System.out.println("Number of top topical words: " + numTopicWords);

		//tAssignsFilePath = pathToTAfile;
//		if (tAssignsFilePath.length() > 0)
//			initialize(tAssignsFilePath);
//		else
			try {
				initialize();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
	
	public void getTopicAssignments(String pathToTopicAssignmentFile) {
		
		System.out.println("Reading topic-assignment file: "
				+ pathToTopicAssignmentFile);

			topicAssignments = new ArrayList<List<Integer>>();

			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(pathToTopicAssignmentFile));
				int docID = 0;
				int numWords = 0;
				for (String line; (line = br.readLine()) != null;) {
					String[] strTopics = line.trim().split("\\s+");
					List<Integer> topics = new ArrayList<Integer>();
					for (int j = 0; j < strTopics.length; j++) {
						int topic = new Integer(strTopics[j]);
						// Increase counts
						docTopicCount[docID][topic] += 1;
						topicWordCount[topic][corpus.get(docID).get(j)] += 1;
						sumDocTopicCount[docID] += 1;
						sumTopicWordCount[topic] += 1;

						topics.add(topic);
						numWords++;
					}
					topicAssignments.add(topics);
					docID++;
				}

				if ((docID != numDocuments) || (numWords != numWordsInCorpus)) {
					System.out
						.println("The topic modeling corpus and topic assignment file are not consistent!!!");
					throw new Exception();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		
	}


	
	
	


	public void inference()
		throws IOException
	{
		System.out.println("Running Gibbs sampling inference: ");
		
		if (numDocuments<3) {
			//just skip
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/Users/QimingChen/Desktop/output/skip.txt", true)));
			//PrintWriter out = new PrintWriter("/Users/QimingChen/Desktop/output/skip.txt");
	    	out.print(fileCount + ": ");
			out.println(corpusPath.get(fileCount));
    	    out.close();
		}
		else{
			for (int iter = 1; iter <= numIterations; iter++) {

				//System.out.println("\tSampling iteration: " + (iter));
				// System.out.println("\t\tPerplexity: " + computePerplexity());

				// sample from p(z_i|z_-1,w)
				sampleInSingleIteration();

			}
			expName = orgExpName;

			writeParameters();
			System.out.println("Writing output from the last sample ...");
			write();

			System.out.println("Sampling completed!");
		}
		

	}

	public double getPossionProbability(int k, double lambda) {  //k lambda: average times
        double c = Math.exp(-lambda), sum = 1;  
        for (int i = 1; i <= k; i++) {  
            sum *= lambda / i;  
        }  
        return sum * c;  
    }  
	
	public int getPossionVariable(double lambda) {  
        int x = 0;  
        double y = Math.random(), cdf = getPossionProbability(x, lambda);  
        while (cdf < y) {  
            x++;  
            cdf += getPossionProbability(x, lambda);  
        }  
        return x;  
    }  
	
	public void sampleInSingleIteration()
	{
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			int possionNd=getPossionVariable(1);
			if (possionNd<docSize) {
				docSize=possionNd;
			}
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				// Get current word and its topic
				int topic = topicAssignments.get(dIndex).get(wIndex);
				int word = corpus.get(dIndex).get(wIndex);

				// Decrease counts
				// remove z_i from the count variables
				docTopicCount[dIndex][topic] -= 1;
				sumDocTopicCount[dIndex] -= 1;
				topicWordCount[topic][word] -= 1;
				sumTopicWordCount[topic] -= 1;

				double Vbeta = vocabularySize * beta[topic][word];
				double Kalpha = numTopics * alpha[topic];
				
				// Sample a topic
				// do multinomial sampling via cummulative method		
				// p(z_i|z_-i,w) =(n_k_-i^(t) + beta_t)/(sum n_k_-i+beta_t) *(n_m_-i^(k) + alpha(k))
				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					multiProbs[tIndex] =  (topicWordCount[tIndex][word] 
							+ beta[tIndex][word])
							/
							(sumTopicWordCount[tIndex]+Vbeta) *
							(docTopicCount[dIndex][tIndex] 
									+ alpha[dIndex]) /
									(sumDocTopicCount[dIndex]+
											Kalpha);
				}
				topic = nextDiscrete(multiProbs);
				
				// Increase counts
				docTopicCount[dIndex][topic] += 1;
				sumDocTopicCount[dIndex] += 1;
				topicWordCount[topic][word] += 1;
				sumTopicWordCount[topic] += 1;

				// Update topic assignments
				topicAssignments.get(dIndex).set(wIndex, topic);
			}
		}
	}

	
	
	
	
	
	
	public void writeParameters()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			 + "paras."+ expName));
		writer.write("-model" + "\t" + "LDA");
		writer.write("\n-corpus" + "\t" + corpusPath);
		writer.write("\n-ntopics" + "\t" + numTopics);
		writer.write("\n-alpha" + "\t" + alpha[0]);
		writer.write("\n-beta" + "\t" + beta[0][0]);
		writer.write("\n-niters" + "\t" + numIterations);
		writer.write("\n-twords" + "\t" + numTopicWords);
		writer.write("\n-name" + "\t" + expName);
	
		writer.close();
	}

	public void writeDictionary()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			 + "vocabulary."+ expName));
		for (String word : word2IdVocabulary.keySet()) {
			writer.write(word + " " + word2IdVocabulary.get(word) + "\n");
		}
		writer.close();
	}

	public void writeIDbasedCorpus()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			 + "IDcorpus."+ expName));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				writer.write(corpus.get(dIndex).get(wIndex) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicAssignments()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			 + "topicAssignments."+ expName));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				writer.write(topicAssignments.get(dIndex).get(wIndex) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopTopicalWords()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ "topWords."+ expName));

		for (int tIndex = 0; tIndex < numTopics; tIndex++) {
			writer.write("Topic" + new Integer(tIndex) + ":");

			Map<Integer, Integer> wordCount = new TreeMap<Integer, Integer>();
			for (int wIndex = 0; wIndex < vocabularySize; wIndex++) {
				wordCount.put(wIndex, topicWordCount[tIndex][wIndex]);
			}
			//wordCount = FuncUtils.sortByValueDescending(wordCount);
	        List<Map.Entry<Integer, Integer>> list = new LinkedList<Map.Entry<Integer, Integer>>(wordCount.entrySet());
	        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>()
	        {
	            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2)
	            {
	                int compare = (o1.getValue()).compareTo(o2.getValue());
	                return -compare;
	            }
	        });

	        Map<Integer, Integer> result = new LinkedHashMap<Integer, Integer>();
	        for (Map.Entry<Integer, Integer> entry : list) {
	            result.put(entry.getKey(), entry.getValue());
	        }
	        wordCount= result;
			
			Set<Integer> mostLikelyWords = wordCount.keySet();
			int count = 0;
			for (Integer index : mostLikelyWords) {
				if (count < numTopicWords) {
					writer.write(" " + id2WordVocabulary.get(index));
					count += 1;
				}
				else {
					writer.write("\n\n");
					break;
				}
			}
		}
		writer.close();
	}

	public void writeTopicWordPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			 + "phi."+ expName));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				double pro = (topicWordCount[i][j] + beta[i][j])
					/ (sumTopicWordCount[i] + betaSum);
				writer.write(pro + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicWordCount()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
		 + "WTcount."+ expName));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				writer.write(topicWordCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();

	}

	public void writeDocTopicPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			 + "theta."+ expName));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				double pro = (docTopicCount[i][j] + alpha[j])
					/ (sumDocTopicCount[i] + alphaSum);
				writer.write(pro + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeDocTopicCount()
		throws IOException
	{
//		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
//			+ expName + ".DTcount"));
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
				 + "DTcount."+ expName));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				writer.write(docTopicCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void write()
		throws IOException
	{
		
		expName="testLDA";
		writeTopTopicalWords();
		writeDocTopicPros();
		writeTopicAssignments();
		writeTopicWordPros();
		expName=this.corpusPath.get(fileCount-1).substring(44, this.corpusPath.get(fileCount-1).length()-4);
		writeTopTopicalWords();
		writeDocTopicPros();
		writeTopicAssignments();
		writeTopicWordPros();
	}
	
	public static int nextDiscrete(double[] probs)
    {
        double sum = 0.0;
     // cummulate multinomial parameters
        for (int i = 0; i < probs.length; i++)
            sum += probs[i];

        //double r = MTRandom.nextDouble() * sum;
     // scaled sample because of unnormalized p[]
        double r= (new Random()).nextDouble() *sum;
        
        sum = 0.0;
        for (int i = 0; i < probs.length; i++) {
            sum += probs[i];
            if (sum > r){
                return i;
            }
        }
        return probs.length - 1;
    }
	
	public static Vector<String> getFileName(String filePath){
		Vector<String> fileName = new Vector<String>();
		File folder1 = new File(filePath);
		String[] list1 = folder1.list();
		for (int i = 0; i < list1.length; i++) {
			if (i>0) {
				fileName.addElement(filePath+"/"+list1[i]);
			}
			
		}
		
		return fileName;
	}
	
	public static void main(String args[])
		throws Exception
	{
		//get parameters
		Vector<String> fileName;
		fileName=getFileName("/Users/QimingChen/Desktop/Yelp_Review");
		
		int numOfTopic=3;
		int numOfWord=10;
		int numOfIter=1000;
		double alpha=0.01;
		double beta=0.3; // 1/numoftopic
		sparseLDA lda = null;
		
		// training
		for (int i = 0; i < fileName.size(); i++) {
			System.out.println("file" + (i+1));
			System.out.println("Reading topic modeling corpus: " + fileName.get(i));
			if (i==0) {
				lda=new sparseLDA(fileName,i, numOfTopic, beta, alpha, numOfIter, numOfWord, "testLDA");
			}
			else{
				lda.newCorpus(i,alpha,beta);
			}
			lda.inference();
			
		}
		
	}

	
}
