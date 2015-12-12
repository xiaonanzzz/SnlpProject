/**
 * 
 */
package snlp.predict.ibcf;

import java.util.Map;

/**
 * @author Alex
 *
 */
public class ItemSimilarityModel {
	
	Map<String, double[]> itemVectors;
	
	
	
	public ItemSimilarityModel(Map<String, double[]> itemVectors) {
		super();
		this.itemVectors = itemVectors;
	}



	/***
	 * 
	 * @param item1
	 * @param item2
	 * @return
	 */
	double cosineSimilarity(String item1, String item2){
		double[] vec1 = itemVectors.get(item1);
		if (vec1 == null)
			return 0;
		double[] vec2 = itemVectors.get(item2);
		if (vec2 == null)
			return 0;
		
		if (vec1.length != vec2.length)
			throw new RuntimeException("vec1.length != vec2.length");
		
		double norm1 = 0;
		double norm2 = 0;
		double innerProduct = 0;  // inner product
		
		for(int i = 0; i < vec1.length; i++){
			norm1 += Math.pow(vec1[i], 2);
			norm2 += Math.pow(vec2[i], 2);
			innerProduct = vec1[i] * vec2[i];
		}
		if (norm1 == 0 || norm2 == 0)
			return 0;
		
		return innerProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
	}
	
}
