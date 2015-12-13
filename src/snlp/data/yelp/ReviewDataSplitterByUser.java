package snlp.data.yelp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import snlp.data.yelp.entity.ReviewStars;

public class ReviewDataSplitterByUser {

	String filePath;
	String outputPath;
	
	
	
	/**
	 * @param filePath
	 * @param outputPath
	 */
	public ReviewDataSplitterByUser(String filePath, String outputPath) {
		super();
		this.filePath = filePath;
		this.outputPath = outputPath;
	}


	/**
	 * 
	 * @param outputName
	 * @param validateRatio
	 * @param testRatio
	 */
	public void split(String outputName, double validateRatio, double testRatio){
		Set<String> userSet = new HashSet<String>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try
		{
			int dataSize = 0;
			BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
			
			for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()){
				dataSize++;
			}
			bufferedReader.close();
			
			System.out.printf("Read %d lines of data.\n", dataSize);
			
			int testCount = (int)(dataSize * testRatio);
			assert(testCount >= 0);
			
			int validateCount = (int)(dataSize * validateRatio);
			assert(validateCount >= 0);
			assert(1 > testRatio + validateRatio);
			
			int trainCount = dataSize - testCount - validateCount;
			
			System.out.printf("%d lines as test data.\n", testCount);
			System.out.printf("%d lines as validate data.\n", validateCount);
			FileWriter trainFileWriter = trainCount == 0? null : new FileWriter(outputPath + "/train" + outputName);
			FileWriter validateFileWriter = validateCount == 0? null : new FileWriter(outputPath + "/validate" + outputName);
			FileWriter testFileWriter = testCount == 0? null : new FileWriter(outputPath + "/test" + outputName);
			
			bufferedReader = new BufferedReader(new FileReader(filePath));
			int pcount = 0;
			for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()){
				ReviewStars review = objectMapper.readValue(line, ReviewStars.class);
				if (userSet.contains(review.getUser_id())){
					if (validateCount > 0){
						validateCount--;
						validateFileWriter.write(line);
						validateFileWriter.write("\n");
					}else if(testCount > 0){
						testCount--;
						testFileWriter.write(line);
						testFileWriter.write("\n");
					}else{
						trainCount--;
						trainFileWriter.write(line);
						trainFileWriter.write("\n");
					}
				}else{
					trainCount--;
					userSet.add(review.getUser_id());
					trainFileWriter.write(line);
					trainFileWriter.write("\n");
				}
				pcount++;
				if (pcount %10000 == 0)
					System.out.println("Processed " + pcount +" lines.");
			}
			bufferedReader.close();
			if (trainFileWriter != null)
				trainFileWriter.close();
			if (validateFileWriter != null)
				validateFileWriter.close();
			if (testFileWriter != null)
				testFileWriter.close();
			
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	
	public static void main(String[] args) {
		
		ReviewDataSplitterByUser dataSplitter = new ReviewDataSplitterByUser("./dirty data/rest-review-stars.json", "./dirty data");
		dataSplitter.split("-rest-review-stars.json", 0.1, 0.1);
	}

}
