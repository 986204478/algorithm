package datamining.statistics;


import java.util.concurrent.ConcurrentHashMap;

public class Test {

	public static void main(String[] args) {

//		double[] values = {13.1, 5.1, 18.0, 8.7, 16.5, 9.8, 6.8, 12.0, 17.8, 25.4, 19.2, 15.8, 23.0};
	//	double[] values = {1,1,1,2,1,2};
//		double[] values = {55.95,56.54,57.56,55.13,57.48,56.06,59.93,58.30,52.57,58.46};
		
		
//		double[] values = {23,5,98,14,66,25,78,34,66,74,56,87,12,39,71,49,58};
//		Arrays.sort(values );
		
//
		ConcurrentHashMap.newKeySet();
		Boolean.valueOf(true);
		double[] values = {1,7,2,8,0,5,3,4,9,11,59,5,9,2,9,2,7};
		
		for (int i = 0; i < values.length; i++) {
			System.out.print(values[i] + " ");
		}
		
		System.out.println();
		
		System.out.println("均值:" + BasicTool.getMeanValue(values));
		
		System.out.println("方差:" + BasicTool.getVariance(values));
		System.out.println("标准差:" + BasicTool.getStandardDeviation(values));
	}

}
