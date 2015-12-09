package project1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class CreateVocabulary {
	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID given a word
	public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word given an ID
	public HashMap<Integer,Integer> id2WordCount;
	public List<String> stopWord;
	
	public CreateVocabulary(Vector<String> corpusPath){
		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		id2WordCount=new HashMap<Integer,Integer>();
		stopWord=new ArrayList<String>();
		
		try {
			BufferedReader br = null;
			br = new BufferedReader(new FileReader("/Users/QimingChen/Desktop/stop-words_english_3_en.txt"));
			for (String doc; (doc = br.readLine()) != null;) {
				stopWord.add(doc);
			}
			br.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
			
		//create vocabulary
		int indexWord = -1;
		for (int i = 0; i < corpusPath.size(); i++) {
			System.out.println(i+"\n");
			try {
				BufferedReader br = null;
				br = new BufferedReader(new FileReader(corpusPath.get(i)));
				for (String doc; (doc = br.readLine()) != null;) {
	
					if (doc.trim().length() == 0)
						continue;
	
					String[] words = doc.trim().split("\\s+");
					for (String word : words) {
						String[] words2 = word.trim().split("[\\W&&[^\"']]");
						for (String word2 : words2) {
							word2=word2.replaceAll("[0-9]", "");
							word2=word2.toLowerCase();
							if (!word2IdVocabulary.containsKey(word2)) {
								indexWord ++;
								word2IdVocabulary.put(word2, indexWord);
								id2WordVocabulary.put(indexWord, word2);
								
								id2WordCount.put(indexWord, 1);
							}else{
								id2WordCount.put(word2IdVocabulary.get(word2), id2WordCount.get(word2IdVocabulary.get(word2)) + 1);
							}
								
						}
					}
				}
				br.close();
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void writeVocabulary(List<String> stopWord,HashMap<Integer,Integer> wordCount,HashMap<Integer, String> vocab, String outputPath){
		BufferedWriter writer;
		try {
			int count=0;
			writer = new BufferedWriter(new FileWriter(outputPath + "/" + "vocabulary"));
			for (int i = 0; i <vocab.size() ; i++) {
				if (wordCount.get(i)<100 || stopWord.contains(vocab.get(i))) {
					System.out.println(vocab.get(i));
					continue;
				}
				writer.write(vocab.get(i) + " ");
				writer.write("\n");
				count++;
			}
			writer.close();
			System.out.println(count);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args[])
			throws Exception
		{
			//get parameters
			Vector<String> fileName = GibbsSamplingLDA.getFileName("/Users/QimingChen/Desktop/Yelp_Review");
			CreateVocabulary c= new CreateVocabulary(fileName);
			System.out.println(c.id2WordVocabulary.size());
			writeVocabulary(c.stopWord,c.id2WordCount,c.id2WordVocabulary,"/Users/QimingChen/Desktop");
		}
}


