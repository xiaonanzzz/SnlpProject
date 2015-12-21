/**
 * 
 */
package snlp.data.yelp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
			if (count % 10000 == 0) System.out.printf("Processed %d reviews\n", count);
			ReviewStars reviewStars = objectMapper.readValue(line, ReviewStars.class);
			fileWriter.write(objectMapper.writeValueAsString(reviewStars));
			fileWriter.write("\n");
		}
		System.out.printf("Cleaned %d reviews!\n", count);
		bufferReader.close();
		fileWriter.close();
	}
	
	/**
	 * 
	 * @param outputName
	 * @param amount
	 * @throws IOException
	 */
	public void retrieveUsers(String outputName, int amount) throws IOException{
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BufferedReader bufferReader = new BufferedReader(new FileReader(this.reviewPath));
		FileWriter fileWriter = new FileWriter(this.outputPath + "/" + outputName);
		if (amount < 0) amount = Integer.MAX_VALUE;
		int count = 0;
		Set<String> users = new HashSet<String>();
		for (String line = bufferReader.readLine(); line != null; line = bufferReader.readLine()){
			if (count++ >= amount) break;
			if (count % 10000 == 0) System.out.printf("Processed %d reviews\n", count);
			ReviewStars reviewStars = objectMapper.readValue(line, ReviewStars.class);
			users.add(reviewStars.getUser_id());
		}
		for (String user : users){
			fileWriter.write(user);
			fileWriter.write("\n");
		}
		System.out.printf("Retrieve %d users!\n", users.size());
		bufferReader.close();
		fileWriter.close();
	}
	
	/***
	 * 
	 * @param outputName
	 * @param amount
	 * @throws IOException
	 */
	public void retrieveBusiness(String outputName, int amount) throws IOException{
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BufferedReader bufferReader = new BufferedReader(new FileReader(this.reviewPath));
		FileWriter fileWriter = new FileWriter(this.outputPath + "/" + outputName);
		if (amount < 0) amount = Integer.MAX_VALUE;
		int count = 0;
		Set<String> businesses = new HashSet<String>();
		for (String line = bufferReader.readLine(); line != null; line = bufferReader.readLine()){
			if (count++ >= amount) break;
//			if (count % 10000 == 0) System.out.printf("Processed %d reviews\n", count);
			ReviewStars reviewStars = objectMapper.readValue(line, ReviewStars.class);
			businesses.add(reviewStars.getBusiness_id());
		}
		for (String business : businesses){
			fileWriter.write(business);
			fileWriter.write("\n");
		}
		System.out.printf("Retrieve %d business!\n", businesses.size());
		bufferReader.close();
		fileWriter.close();
	}
	
	public void filterReviewsByBusinessIds(String[] businessIds, String outputName) throws IOException{
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BufferedReader bufferReader = new BufferedReader(new FileReader(this.reviewPath));
		FileWriter fileWriter = new FileWriter(this.outputPath + "/" + outputName);
		
		Set<String> idset = new HashSet<String>();
		for (String id : businessIds){
			idset.add(id);
		}
		
		int count = 0;
		for (String line = bufferReader.readLine(); line != null; line = bufferReader.readLine()){
			count++;
			if (count % 10000 == 0) System.out.printf("Processed %d reviews\n", count);
			ReviewStars reviewStars = objectMapper.readValue(line, ReviewStars.class);
			
			if (idset.contains(reviewStars.getBusiness_id())){
				fileWriter.write(line);
				fileWriter.write("\n");
			}
			
		}
		System.out.printf("Cleaned %d reviews!\n", count);
		bufferReader.close();
		fileWriter.close();
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String work = "retri_stars";
		if (work.equals("filterbid")){
			BusinessIdFile idfile = new BusinessIdFile("./dirty data/idList");
			ReviewCleaner reviewCleaner = new ReviewCleaner("./dirty data/yelp_academic_dataset_review.json", "./localdata");
			try {
				reviewCleaner.filterReviewsByBusinessIds(idfile.readBusinessIds(), "reviews_rest.json");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return ;
		}
		if (work.equals("retri_stars")){
			ReviewCleaner reviewCleaner = new ReviewCleaner("./dirty data/reviews_rest.json", "./dirty data");
			try {
				reviewCleaner.retriveStars("rest-review-stars.json", -1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return ;
		}
		
		
		ReviewCleaner reviewCleaner = new ReviewCleaner("./dirty data/yelp_academic_dataset_review.json", "./data");
		
		try {
			reviewCleaner.retriveStars("reviewstars.json", -1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			reviewCleaner.retrieveUsers("users.text", -1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			reviewCleaner.retrieveBusiness("business.text", -1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
}
