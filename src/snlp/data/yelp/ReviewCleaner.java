/**
 * 
 */
package snlp.data.yelp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import snlp.data.yelp.entity.ReviewStars;

/**
 * @author Alex
 * This object is used to remove useless components in the data
 */
public class ReviewCleaner {

	String reviewPath;
	String outputPath;
	
	public ReviewCleaner(String reviewPath, String outputPath){
		this.reviewPath = reviewPath;
		this.outputPath = outputPath;
	}
	
	/**
	 * 
	 * @param outputName the name of output file
	 * @param amount how many entries to retrive, -1 means all of them (maxInt)
	 * @throws IOException 
	 */
	public void retriveStars(String outputName, int amount) throws IOException{
		
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BufferedReader bufferReader = new BufferedReader(new FileReader(this.reviewPath));
		FileWriter fileWriter = new FileWriter(this.outputPath + "/" + outputName);
		if (amount < 0) amount = Integer.MAX_VALUE;
		int count = 0;
		for (String line = bufferReader.readLine(); line != null; line = bufferReader.readLine()){
			if (count++ >= amount) break;
			if (count % 1000 == 0) System.out.printf("Processed %d reviews\n", count);
			ReviewStars reviewStars = objectMapper.readValue(line, ReviewStars.class);
			fileWriter.write(objectMapper.writeValueAsString(reviewStars));
			fileWriter.write("\n");
		}
		System.out.printf("Cleaned %d reviews!\n", count);
		bufferReader.close();
		fileWriter.close();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ReviewCleaner reviewCleaner = new ReviewCleaner("./dirty data/yelp_academic_dataset_review.json", "./data");
		
		try {
			reviewCleaner.retriveStars("reviewstars.json", -1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
}
