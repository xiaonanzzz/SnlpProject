/**
 * 
 */
package snlp.data.yelp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alex
 *
 */
public class TopicBasedBusinessSimilarDataReader {

	String businessIdFilePath;
	String businessTopicVectorFilePath;
	int dimension = 0;
	
	
	
	/**
	 * @param businessIdFilePath
	 * @param businessTopicVectorFilePath
	 * @param dimension If 0, infer from data
	 */
	public TopicBasedBusinessSimilarDataReader(String businessIdFilePath, String businessTopicVectorFilePath,
			int dimension) {
		super();
		this.businessIdFilePath = businessIdFilePath;
		this.businessTopicVectorFilePath = businessTopicVectorFilePath;
		this.dimension = dimension;
	}



	/***
	 * 
	 * @return
	 * @throws IOException
	 */
	public Map<String, double[]> retrieveBusinessTopicVector() throws IOException{
		Map<String, double[]> businessTopicVectors = new HashMap<String, double[]>();
		
		BufferedReader businessIdReader = new BufferedReader(new FileReader(this.businessIdFilePath));
		
		BufferedReader topicVectorReader = new BufferedReader(new FileReader(this.businessTopicVectorFilePath));
		
		for (String businessId = businessIdReader.readLine();
				businessId != null; businessId = businessIdReader.readLine()){
			
			String topicVectorLine = topicVectorReader.readLine();
			if (topicVectorLine == null)
				throw new RuntimeException("topicVectorLine == null");
			
			String[] strVec = topicVectorLine.split(" ");
			if (dimension == 0)
				dimension = strVec.length; // if dimension is not pre-assigned
			if (dimension != strVec.length)
				throw new RuntimeException("dimension != strVec.length");
			
			double[] topicVec = new double[dimension];
			for (int i = 0; i < dimension; i++){
				topicVec[i] = Double.parseDouble(strVec[i]);
			}
			businessTopicVectors.put(businessId, topicVec);
		}
		
		businessIdReader.close();
		topicVectorReader.close();
		
		return businessTopicVectors;
	}
	
	
}
