/**
 * 
 */
package snlp.data.yelp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import snlp.data.yelp.entity.ReviewStars;

/**
 * @author Alex
 *
 */
public class ReviewReader {

	String reviewFilePath;

	public ReviewReader(String reviewFilePath) {
		super();
		this.reviewFilePath = reviewFilePath;
	}
	
	public List<ReviewStars> readReviewStars(int amount) throws IOException{
		List<ReviewStars> rsList = new ArrayList<ReviewStars>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		BufferedReader bufferReader = new BufferedReader(new FileReader(this.reviewFilePath));
		
		if (amount < 0) amount = Integer.MAX_VALUE;
		int count = 0;
		for (String line = bufferReader.readLine(); line != null; line = bufferReader.readLine()){
			if (count++ >= amount) break;
//			if (count % 10000 == 0) System.out.printf("Read %d reviews\n", count);
			ReviewStars reviewStars = objectMapper.readValue(line, ReviewStars.class);
			rsList.add(reviewStars);
		}
		System.out.printf("Read %d reviews!\n", count);
		bufferReader.close();
		return rsList;
	}
	
	
}
