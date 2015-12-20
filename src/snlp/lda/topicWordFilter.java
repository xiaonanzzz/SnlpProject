package snlp.lda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class topicWordFilter {
	
	public static void main(String[] args) {
		String topicWordPath="/Users/QimingChen/Desktop/output/numTopWords.positive-500";
		Vector<String[] > corpus= new Vector< String[] >();
		HashMap<String, Integer> wordCount= new HashMap<String, Integer>();
		List<String> outWord=new ArrayList<String>();
		
		//read in topic word
		try {
			BufferedReader br = null;
			br = new BufferedReader(new FileReader(topicWordPath));
			for (String topic; (topic = br.readLine()) != null;) {

				if (topic.trim().length() == 0)
					continue;

				String[] words = topic.trim().split("\\s+");
				for (String word : words) {
					if (!wordCount.containsKey(word)) {
						wordCount.put(word,1);
					}else{
						wordCount.put(word,wordCount.get(word)+1);
					}
							
				}
				
				corpus.add(words);
			}
			br.close();
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//process topic word
		for (int i = 0; i < corpus.size(); i++) {
			String[] topiclist=corpus.get(i);
			for (int j = 0; j < topiclist.length; j++) {
				if (wordCount.get(topiclist[j]) > 10) {
					if (!outWord.contains(corpus.get(i)[j])) {
						outWord.add(corpus.get(i)[j]);
					}
					
					corpus.get(i)[j]="";
					
				}
				
			}
		}
		
		//write to filesystem
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter("/Users/QimingChen/Desktop/output/numTopWords.new2.positive"));
			for (int i = 0; i <corpus.size() ; i++) {
				for (int j = 0; j < corpus.get(i).length; j++) {
					if (corpus.get(i)[j].length()>0) {
						writer.write(corpus.get(i)[j] + " ");
					}
					
					
				}
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		BufferedWriter writer;
//		try {
//			writer = new BufferedWriter(new FileWriter("/Users/QimingChen/Desktop/outList"));
//			for (int i = 0; i <outWord.size() ; i++) {
//					writer.write(outWord.get(i) + " ");
//					writer.write("\n");
//			}
//			
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	
}
