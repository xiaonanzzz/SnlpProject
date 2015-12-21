

package snlp.lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

import org.apache.commons.io.filefilter.AndFileFilter;
import org.w3c.dom.css.Counter;

/**
 * jLDADMM: A Java package for the LDA and DMM topic models
 * 
 * Implementation of the Latent Dirichlet Allocation topic model, using
 * collapsed Gibbs sampling, as described in:
 * 
 * Thomas L. Griffiths and Mark Steyvers. 2004. Finding scientific topics.
 * Proceedings of the National Academy of Sciences of the United States of
 * America, 101(Suppl 1):5228–5235.
 * 
 * @author: Dat Quoc Nguyen
 * 
 * @modifier: Qiming Chen
 */

public class GibbsSamplingLDA_reviewid
{
//	public double alpha; // Hyper-parameter alpha
//	public double beta; // Hyper-parameter alpha
	
	public double[] alpha; // Hyper-parameter alpha- doc topic
	public double[][] beta; // Hyper-parameter beta- topic word

	public int numDocuments; // Number of documents in the corpus
	public int numTopics; // Number of topics
	public int numIterations; // Number of Gibbs sampling iterations
	public int numTopWords; // Number of most probable words for each topic

	public double alphaSum; // alpha * numTopics
	public double[] betaSum; // beta * vocabularySize

	public List<List<Integer>> corpus; // Word ID-based corpus
	public List<List<Integer>> topicAssignments; // Topics assignments for words in the corpus
	
	public int numWordsInCorpus; // Number of words in the corpus

	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID given a word
	public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word given an ID
	public int vocabularySize; // The number of word types in the corpus

	public HashMap<String, String> topic2word;
	public HashMap<String, String> word2topic;
	
	public int[][] sentimentAssignment;
	
	// numDocuments * numTopics matrix
	// Given a document: number of its words assigned to each topic
	public int[][] docTopicCount;
	// Number of words in every document
	public int[] sumDocTopicCount;
	// numTopics * vocabularySize matrix
	// Given a topic: number of times a word type assigned to the topic
	public int[][] topicWordCount;
	// Total number of words assigned to a topic
	public int[] sumTopicWordCount;

	// Double array used to sample a topic
	public double[] multiPros;

	// Path to the directory containing the corpus
	public String folderPath;
	// Path to the topic modeling corpus
	public Vector<String> corpusPath;

	public String expName = "LDAmodel";
	public String orgExpName = "LDAmodel";
	public String tAssignsFilePath = "";
	public int savestep = 0;
	
	public Vector<String> review_id;

	public GibbsSamplingLDA_reviewid(Vector<String> pathToCorpus, String pathToVocab, int inNumTopics,
			double inAlpha, double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToOutput)
		throws Exception
	{
		this(pathToCorpus, pathToVocab,inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName,pathToOutput, "", 0);
	}

	public GibbsSamplingLDA_reviewid(Vector<String> pathToCorpus, String pathToVocab, int inNumTopics,
			double inAlpha, double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToOutput, int inSaveStep)
		throws Exception
	{
		this(pathToCorpus, pathToVocab,inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName,pathToOutput, "", inSaveStep);
	}
	
	public GibbsSamplingLDA_reviewid(Vector<String> pathToCorpus, String pathToVocab, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName, String pathToOutput, String pathToTAfile, int inSaveStep)
		throws Exception
	{

		
		//numDocument=pathToCorpus.size();
		//numDocuments=pathToCorpus.size();
		numDocuments=990626;
		//numDocuments=5000;
		//numDocuments=10;
		
		numTopics = inNumTopics;
		numIterations = inNumIterations;
		numTopWords = inTopWords;
		savestep = inSaveStep;
		expName = inExpName;
		orgExpName = expName;
		corpusPath = pathToCorpus;
		folderPath = pathToOutput + "/";

		//System.out.println("Reading topic modeling corpus: " + pathToCorpus);

		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		//get review_id
		BufferedReader brid = new BufferedReader(new FileReader("/Users/QimingChen/Desktop/Yelp_Review2/review_id.txt"));
		review_id=new Vector<String>();
		for (int i = 0; i < numDocuments; i++) {
			String id=brid.readLine();
			review_id.add(id);
		}
		
		//get vocabulary
		BufferedReader br = null;
		try {
			int indexWord = -1;
			br = new BufferedReader(new FileReader(pathToVocab));
			for (String word; (word = br.readLine()) != null;) {
				indexWord ++;
				word2IdVocabulary.put(word.trim(), indexWord);
				id2WordVocabulary.put(indexWord, word.trim());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		vocabularySize = word2IdVocabulary.size();
		
		corpus = new ArrayList<List<Integer>>();
		
		numWordsInCorpus = 0;
		corpus = new ArrayList<List<Integer>>();
		br = new BufferedReader(new FileReader("/Users/QimingChen/Desktop/Yelp_Review2/review.txt"));
		try {
			//for (int i = 0; i < numDocuments; i++) {
				
				int count=0;
				for (String doc; (doc = br.readLine()) != null && count < numDocuments;) {
					System.out.println(count); count++;
					
					List<Integer> document = new ArrayList<Integer>();
					
					if (doc.trim().length() == 0)
						continue;
					
					String[] words = doc.trim().split("\\s+");
					for (String word : words) {
						String[] words2 = word.trim().split("[\\W&&[^\"']]");
						for (String word2 : words2) {
							word2=word2.replaceAll("[0-9]", "");
							word2=word2.toLowerCase();
							
							//if (word2IdVocabulary.containsKey(word2) && !document.contains(word2)) {
							if (word2IdVocabulary.containsKey(word2)) {
								document.add(word2IdVocabulary.get(word2));
							}else{
								//not in vocabulary
								//document.add(word2IdVocabulary.get(word2));
							}
	
						}
					}
					numWordsInCorpus += document.size();
					corpus.add(document);
				//}
				
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//write 
//		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath + "corpus."+ expName));
//		for (int i=0;i<numDocuments;i++) {
//			for(int j=0;j<corpus.get(i).size();j++){
//				writer.write(corpus.get(i).get(j)+" ");
//			}
//			writer.write("\n");
//		}
//		writer.close();
		
		//get in hyper parameters
		
		alpha = new double[numDocuments];
		alphaSum=0;
		for (int i = 0; i < alpha.length; i++) {
			alpha[i]=inAlpha;
			alphaSum+=alpha[i];
		}
		
		beta=new double[numTopics][vocabularySize];
		betaSum=new double[numTopics];
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				beta[i][j]=1.00/numTopics;
				betaSum[i]=betaSum[i]+beta[i][j];
			}
		}
		
		docTopicCount = new int[numDocuments][numTopics];
		topicWordCount = new int[numTopics][vocabularySize];
		sumDocTopicCount = new int[numDocuments];
		sumTopicWordCount = new int[numTopics];

		multiPros = new double[numTopics];
		for (int i = 0; i < numTopics; i++) {
			multiPros[i] = 1.0 / numTopics;
		}

//		System.out.println("Corpus size: " + numDocuments + " docs, "
//			+ numWordsInCorpus + " words");
//		System.out.println("Vocabuary size: " + vocabularySize);
//		System.out.println("Number of topics: " + numTopics);
//		System.out.println("alpha: " + alpha);
//		System.out.println("beta: " + beta);
//		System.out.println("Number of sampling iterations: " + numIterations);
//		System.out.println("Number of top topical words: " + numTopWords);

		tAssignsFilePath = pathToTAfile;
		if (tAssignsFilePath.length() > 0)
			initialize(tAssignsFilePath);
		else
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
				int topic = nextDiscrete(multiPros); // Sample a topic
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

	/**
	 * Initialize topic assignments from a given file
	 */
	public void initialize(String pathToTopicAssignmentFile)
	{
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
				System.out.println(docID);
				List<Integer> topics = new ArrayList<Integer>();
				if (line.length()==0) {
					topicAssignments.add(topics);
					docID++;
					continue;
				}
				for (int j = 0; j < strTopics.length; j++) {
					int topic = Integer.parseInt(strTopics[j]);
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

		for (int iter = 1; iter <= numIterations; iter++) {

			System.out.println("\tSampling iteration: " + (iter));

			sampleInSingleIteration();

			//System.out.println(computePerplexity());
			
			if ((savestep > 0) && (iter % savestep == 0)
				&& (iter < numIterations)) {
				System.out.println("\t\tSaving the output from the " + iter
					+ "^{th} sample");
				expName = orgExpName + "-" + iter;
				write();
			}
		}
		expName = orgExpName;

		writeParameters();
		System.out.println("Writing output from the last sample ...");
		write();

		System.out.println("Sampling completed!");

	}

	public void sampleInSingleIteration()
	{
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				// Get current word and its topic
				int topic = topicAssignments.get(dIndex).get(wIndex);
				int word = corpus.get(dIndex).get(wIndex);

				// Decrease counts
				docTopicCount[dIndex][topic] -= 1;
				sumDocTopicCount[dIndex] -= 1;
				topicWordCount[topic][word] -= 1;
				sumTopicWordCount[topic] -= 1;
				
				
				double Vbeta = vocabularySize * beta[topic][word];
				double Kalpha = numTopics * alpha[dIndex];
				
				// Sample a topic
				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					
					multiPros[tIndex] =  (topicWordCount[tIndex][word] 
							+ beta[tIndex][word])
							/
							(sumTopicWordCount[tIndex]+Vbeta) *
							(docTopicCount[dIndex][tIndex] 
									+ alpha[dIndex]) /
									(sumDocTopicCount[dIndex]+
											Kalpha);
				}
				
				topic = nextDiscrete(multiPros);

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
	
	public static int nextDiscrete(double[] probs)
    {
        double sum = 0.0;
        for (int i = 0; i < probs.length; i++)
            sum += probs[i];

        double r= (new Random()).nextDouble() *sum;

        sum = 0.0;
        for (int i = 0; i < probs.length; i++) {
            sum += probs[i];
            if (sum > r)
                return i;
        }
        return probs.length - 1;
    }

	 public double computePerplexity()
	 {
		 double logliCorpus = 0.0;
		 for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
		 int docSize = corpus.get(dIndex).size();
		 double logliDoc = 0.0;
		 for (int wIndex = 0; wIndex < docSize; wIndex++) {
		 int word = corpus.get(dIndex).get(wIndex);
		 double likeWord = 0.0;
		 for (int tIndex = 0; tIndex < numTopics; tIndex++) {
		 likeWord += ((docTopicCount[dIndex][tIndex] + alpha[dIndex]) /
		 (sumDocTopicCount[dIndex] + alphaSum))
		 * ((topicWordCount[tIndex][word] + beta[tIndex][word]) / (sumTopicWordCount[tIndex] +
		 betaSum[tIndex]));
		 }
		 logliDoc += Math.log(likeWord);
		 }
		 logliCorpus += logliDoc;
		 }
		 double perplexity = Math.exp(-1.0 * logliCorpus / numWordsInCorpus);
		 if (perplexity < 0)
		 throw new RuntimeException("Illegal perplexity value: " + perplexity);
		 return perplexity;
	 }

	public void writeParameters()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".paras"));
		writer.write("-model" + "\t" + "LDA");
		writer.write("\n-corpus" + "\t" + corpusPath);
		writer.write("\n-ntopics" + "\t" + numTopics);
		writer.write("\n-alpha" + "\t" + alpha);
		writer.write("\n-beta" + "\t" + beta);
		writer.write("\n-niters" + "\t" + numIterations);
		writer.write("\n-twords" + "\t" + numTopWords);
		writer.write("\n-name" + "\t" + expName);
		if (tAssignsFilePath.length() > 0)
			writer.write("\n-initFile" + "\t" + tAssignsFilePath);
		if (savestep > 0)
			writer.write("\n-sstep" + "\t" + savestep);

		writer.close();
	}

	public void writeDictionary()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".vocabulary"));
		for (String word : word2IdVocabulary.keySet()) {
			writer.write(word + " " + word2IdVocabulary.get(word) + "\n");
		}
		writer.close();
	}

	public void writeIDbasedCorpus()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".IDcorpus"));
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
			+ expName + ".topicAssignments"));
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
				+ "numTopWords."+ expName));

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
					if (count < numTopWords) {
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
			+ expName + ".phi"));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				double pro = (topicWordCount[i][j] + beta[i][j])
					/ (sumTopicWordCount[i] + betaSum[i]);
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
			+ expName + ".WTcount"));
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
			+ expName + ".theta"));
	
		for (int i = 0; i < numDocuments; i++) {
			String id=review_id.get(i);
			writer.write(id + " ");
			for (int j = 0; j < numTopics; j++) {
				double pro = (docTopicCount[i][j] + alpha[i])
					/ (sumDocTopicCount[i] + alpha[i]*numTopics);
				writer.write(pro + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeDocTopicCount()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".DTcount"));
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
		writeTopTopicalWords();
		writeDocTopicPros();
		writeTopicAssignments();
		writeTopicWordPros();
		System.out.println(computePerplexity());
	}
	
	public static Vector<String> getFileName(String filePath) throws IOException{
		List<String> restaurant_id = new ArrayList<String>();
		restaurant_id=getRestaurantId();
		
		
//		Vector<String> fileName = new Vector<String>();
//		File folder1 = new File(filePath);
//		String[] list1 = folder1.list();
//		for (int i = 0; i < list1.length; i++) {
//			if (i>0) {
//				
//				fileName.addElement(filePath+"/"+list1[i]);
//			}
//			
//		}
		
		Vector<String> fileName = new Vector<String>();
		File folder1 = new File(filePath);
		String[] list1 = folder1.list();//990626
		for (int i = 0; i < 1000; i++) {
			if (i>0) {
				String temp=list1[i].substring(6, list1[i].length()-4);
				if (restaurant_id.contains(temp)) {
					fileName.addElement(filePath+"/"+list1[i]);
				}	
			}
			
		}
		
		return fileName;
	}
	
	public static List<String> getRestaurantId() throws IOException{
		
		String pathToId="/Users/QimingChen/Desktop/restaurant_id";
		List<String> id = new ArrayList<String>();
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(pathToId));
			for (String word; (word = br.readLine()) != null;) {
				id.add(word);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		br.close();
		return id;
	}
	
	public double sumDouble(double[] input){
		double sum=0;
		for (int i = 0; i < input.length; i++) {
			sum=sum+input[i];
		}
		return sum;
	}

	public static void main(String args[])
		throws Exception
	{
		Vector<String> fileName = new Vector<String>();
		//fileName=getFileName("/Users/QimingChen/Desktop/Yelp_Review2");
		//System.out.println(fileName.size());
		String pathToVocab="/Users/QimingChen/Desktop/vocabulary28482";
		String pathToOutput="/Users/QimingChen/Desktop/output";
		//String pathToTA="";
		String pathToTA="/Users/QimingChen/Desktop/output/reviews1-450.topicAssignments";
		int numOfTopic=100;
		int numOfWord=100;
		int numOfIter=1000;
		double alpha=0.01;
		double beta=0.33; // 1/numoftopic
		GibbsSamplingLDA_reviewid lda=new GibbsSamplingLDA_reviewid(fileName,pathToVocab, numOfTopic, alpha, beta, numOfIter, numOfWord, "reviews3",pathToOutput,pathToTA,10); //save every 20 iteration
//"/Users/QimingChen/Desktop/output/reviews1-450.topicAssignments
		//GibbsSamplingLDA lda = new GibbsSamplingLDA("/Users/QimingChen/Desktop/Statistical NLP/project/jLDADMM_v1.0/src/models/test/corpus.txt", 7, 0.1,
		//	0.01, 2000, 20, "testLDA");
		lda.inference();
	}
}


