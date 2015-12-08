package snlp.data;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;


public class Stopword {
	private static Set<String> stopwords;
	
	public Stopword(String stopwordFile) throws IOException {
		stopwords = new HashSet<String>();
		Scanner sc = new Scanner(new File(stopwordFile));
		while(sc.hasNextLine()) {
			stopwords.add(sc.nextLine());
		}
		stopwords.add("");
		sc.close();
	}
	
	public boolean isStopword(String word) {
		return stopwords.contains(word);
	}
}
