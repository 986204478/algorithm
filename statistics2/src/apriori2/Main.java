package apriori;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Main {

	/**
	 * 程序入口，可以通过命令行输入，也可以直接运行
	 * 
	 * @param args 输入的参数
	 */
	public static void main(String[] args) {

		if (args.length > 1) {
			String datafile = args[0];
			int minSup = 0;
			try {
				minSup = Integer.parseInt(args[1]);

			} catch (Exception e) {
				System.out.println("请输入正确的最小支持度");
				return;
			}

			File file = new File(datafile);

			if (!file.exists()) {
				System.out.println("没有输入请输入源数据地址或地址错误，按照测试数据运行");
				new Main(2).start(file, 2);
			} else {
				new Main(minSup).start(file, minSup);
			}
		} else {
			System.out.println("没有输入源数据，按照测试数据运行");
			new Main(2).start(new File("s"), 2);
		}

	}

	public final int MIN_SUP;

	
	/** 
	 *  入口类的 主方法
	 * @param minSup 最小支持度阈值
	 */
	public Main(int minSup) {
		MIN_SUP = minSup;
	}

	private List<FrequentCollection> L = new ArrayList<>();
	private List<String[]> D = new ArrayList<>(); // 数据源，数据库的事务列表
	private CandidateCollection C;
	private HashMap<String, Integer> subset = new HashMap<>();

	private HashMap<String, Double> supports = new HashMap<>();

	private int tradeCount = 0;

	/**
	 * 
	 * @param datafile
	 * @param minSup
	 *            最小支持度 阈值
	 */
	private void start(File datafile, int minSup) {
		System.out.println("最小支持度阈值:" + minSup);

		if (datafile.exists()) {
			readFile(datafile);
		} else {
			loadTestData();
		}

		init();

		firstScan(); // 得到L1=1-频繁项集

		for (int k = 2; L.size() >= k - 1; k++) {

			scan(k);
		}

		printRelationRules();
	}

	private void init() {
		tradeCount = D.size();
	}

	private void loadTestData() {
		String[] strs1 = new String[] { "bread", "cream", "milk", "tea" };
		String[] strs2 = new String[] { "bread", "cream", "milk" };
		String[] strs3 = new String[] { "cake", "milk" };
		String[] strs4 = new String[] { "milk", "tea" };
		String[] strs5 = new String[] { "bread", "cake", "milk" };
		String[] strs6 = new String[] { "bread", "tea" };
		String[] strs7 = new String[] { "beer", "milk", "tea" };
		String[] strs8 = new String[] { "bread", "tea" };
		String[] strs9 = new String[] { "bread", "cream", "milk", "tea" };
		String[] strs10 = new String[] { "bread", "milk", "tea" };
		D.add(strs1);
		D.add(strs2);
		D.add(strs3);
		D.add(strs4);
		D.add(strs5);
		D.add(strs6);
		D.add(strs7);
		D.add(strs8);
		D.add(strs9);
		D.add(strs10);
		getSubSet();
	}

	/**
	 * 从文件中读取数据，格式就像老师的那样
	 */
	private void readFile(File file) {
		String data = FileHelper.readSpecificEncodingFile(file.getPath(), "gbk");
		data = data.replaceAll("T\\d*\\s", "");
		// System.out.println(data);
		data = data.replace(" ", "");
		// System.out.println(data);
		data = data.substring(data.indexOf("\n") + 1);

		if (data.indexOf("\r") >= 0)
			data = data.substring(data.indexOf("\r") + 1);
		// System.out.println("去掉第一行:\n" + data);
		String[] strs = data.split("\n");

		for (String s : strs) {
			String[] items = s.split(",");
			D.add(items);
		}

		getSubSet();

	}

	/**
	 * 获取子集，所有事务的项集的子集，并计数
	 */
	private void getSubSet() {
		for (int i = 0; i < D.size(); i++) {
			String[] items = D.get(i);
			Set<Set<String>> result = MathUtil.getSubSetNotNull(items); // 调用方法
			// 输出结果
			for (Set<String> subSet : result) {
				StringBuffer item = new StringBuffer();
				for (String s : subSet)
					item.append(s);

				String s = item.toString();
				if (subset.containsKey(s)) {
					int count = subset.get(s);
					count++;
					subset.put(s, count);
				} else {
					subset.put(s, 1);
				}

			}
		}
	}

	/**
	 * 需要获得每个候选项几其支持度计数 || 在扫描的同时就可以把小于最小支持度的item删掉了
	 * 对事务的每一行进行扫描，把获取里面的项集并计数，最后得到C1候选项集。然后再进行delete操作得到L1频繁项集
	 */
	private void firstScan() {
		System.out.println("第1次扫描");
		C = new CandidateCollection();
		HashMap<String, ItemSet> itemsSet = new HashMap<>();

		// 扫描(非连接) +计数
		for (int i = 0; i < D.size(); i++) {
			for (String s : D.get(i)) {
				ItemSet iset = null;
				if (!itemsSet.containsKey(s)) {
					iset = new ItemSet();
					iset.addItems(s);
					itemsSet.put(s, iset);
				} else {
					iset = itemsSet.get(s);
				}
				if (iset != null) {
					iset.addSupportCount();
				}
			}
		}

		C.setItems(new ArrayList<>(itemsSet.values()));
		// 删掉不满足最小支持度的ItemSet
		cut();
	}

	/**
	 * 对平凡项集Li进行连接，然后再把不符合要求的项集剪掉
	 * 
	 * 连接步的算法说明 为找到 Lk(k>=2),通过Lk-1与【自身】作连接产生候选k-项集的集合Ck。
	 * 设l1和l2是Lk-1中的项集。记li[j]表示li的第j个项。 Apriori算法假定事务或项集中的项按字典次序排序；
	 * 可以连接的要求：l1和l2的前(k-2)个对应的项相等，如果只有一个项，那么就直接进行连接即可。 然后产生项集Ck
	 * 
	 * 
	 */
	private void scan(int k) {
		System.out.println("第" + k + "次扫描");
		join(k);
		count();
		cut();
	}

	/**
	 * 连接
	 * 
	 * @param k
	 *            当前的扫描编号
	 */
	private void join(int k) {
		C = new CandidateCollection();
		if (k == 2) {
			firstJoin(2);
			return;
		}
		FrequentCollection Li = L.get(k - 2); // i = k - 1 注意元素的下标和数组的下标
		CandidateCollection Ci = C; // i = k - 1
		ArrayList<ItemSet> itemsSet = Li.getItems();
		for (int i = 0; i < itemsSet.size(); i++) {

			ArrayList<String> itemElements1 = itemsSet.get(i).getItems();
			for (int j = i + 1; j < itemsSet.size(); j++) {
				ItemSet iset = new ItemSet();
				ArrayList<String> itemElements2 = itemsSet.get(j).getItems();
				String str1kSub2 = itemElements1.get(k - 2);
				String str2kSub2 = itemElements2.get(k - 2);

				if (str1kSub2.equals(str2kSub2)) {
					continue;
				}

				boolean joinable = true;
				for (int l = 0; l < itemElements1.size() - 1; l++) {
					String str1 = itemElements1.get(l);
					String str2 = itemElements2.get(l);
					if (!str1.equals(str2)) {
						joinable = false;
						break;
					} else {
						iset.addItems(str1);
					}
				}
				if (!joinable) {
					continue;
				}
				iset.addItems(str1kSub2);
				iset.addItems(str2kSub2);
				Ci.addItems(iset);
			}
		}

	}

	/**
	 * 第一次连接
	 * 
	 * @param k
	 */
	private void firstJoin(int k) {
		FrequentCollection L1 = L.get(k - 2);
		CandidateCollection C2 = C;
		ArrayList<ItemSet> items = L1.getItems();
		for (int i = 0; i < items.size(); i++) {
			for (int j = i + 1; j < items.size(); j++) {
				ItemSet ist = new ItemSet();
				String e1 = items.get(i).getItems().get(0);
				String e2 = items.get(j).getItems().get(0);
				ist.addItems(e1);
				ist.addItems(e2);
				C2.addItems(ist);
			}
		}
	}

	/**
	 * 对候选项集进行计数
	 */
	private void count() {
		for (int i = 0; i < C.getItems().size(); i++) {
			ItemSet ist = C.getItems().get(i);
			StringBuffer str = new StringBuffer();
			for (String s : ist.getItems()) {
				str.append(s);
			}
			String key = str.toString();
			if (subset.containsKey(key)) {
				ist.setSupportCount(subset.get(key));
			}
		}
	}

	/**
	 * 剪枝，这里不是根据项目集空间理论来剪枝的。而是直接判断生成的项集的支持度是否>MIN_SUP来进行剪枝的
	 */
	private void cut() {
		FrequentCollection Li = new FrequentCollection();
		for (ItemSet items : C.getItems()) {
			if (items.getSupportCount() >= MIN_SUP) {
				Li.addItems(items);
			}
		}

		if (Li.getItems().size() > 0) {
			L.add(Li);

			for (ItemSet is : Li.getItems()) {

				StringBuffer key = new StringBuffer();

				for (String strings : is.getItems()) {
					key.append(strings);
				}

				supports.put(key.toString(), getSupport(is));
			}

			System.out.println(Li);
		} else {
			System.out.println("C" + (L.size() + 1) + "=null，算法终止");
		}

	}

	
	//剪枝的构想
//	private void cut() {
//	
//	//获取候选项集的子集，判断子集是否为频繁项集，然后删掉对应的集合
//	FrequentCollection Li = new FrequentCollection();
//	CandidateCollection C2 = new CandidateCollection();
//	
//	
//	for (ItemSet items : C.getItems()) {
//		if (items.getSupportCount() >= MIN_SUP) {
//			Li.addItems(items);
//		}
//		
//		MathUtil.getSubSetNotNull(set);
//		
//		//遍历子集，然后判断子集是否为频繁项集
//		
//		
//	}
//
//	if (Li.getItems().size() > 0) {
//		L.add(Li);
//
//		for (ItemSet is : Li.getItems()) {
//
//			StringBuffer key = new StringBuffer();
//
//			for (String strings : is.getItems()) {
//				key.append(strings);
//			}
//
//		
//			supports.put(key.toString(), getSupport(is));
//		}
//
//		System.out.println(Li);
//	} else {
//		System.out.println("C" + (L.size() + 1) + "=null，算法终止");
//	}
//}
//
	
	/**
	 * 输出结果，打印关联规则
	 */
	private void printRelationRules() {
		System.out.println("\n\n强关联规则如下");

		if (tradeCount < 1) {
			return;
		}

		StringBuffer result = new StringBuffer();
		result.append("强关联规则如下\r\n");
		for (int i = 1; i < L.size(); i++) {

			System.out.println((i + 1) + "-频繁项集");
			ArrayList<ItemSet> items = L.get(i).getItems();

			for (ItemSet iSet : items) {

				StringBuffer key0 = new StringBuffer();

				String[] set = new String[iSet.getItems().size()];

				for (int j = 0; j < iSet.getItems().size(); j++) {
					key0.append(iSet.getItems().get(j));
					set[j] = iSet.getItems().get(j);
				}
				double support0 = supports.get(key0.toString());

				String[][] realSubSetPair = MathUtil.getRealSubSetPair(set);

				for (int j = 0; j < realSubSetPair.length; j++) {
					String key1 = realSubSetPair[j][0];
					String key2 = realSubSetPair[j][1];
					// System.out.println(realSubSetPair[j][0] + " " + realSubSetPair[j][1]);
					double support1 = supports.get(key1);
					double support2 = supports.get(key2);
					String confidence1 = String.format("%.2f", support0 / support1);
					String confidence2 = String.format("%.2f", support0 / support2);

					String str1 = key1 + " >> " + key2 + "\r\n置信度:" + confidence1 + "\r\n";
					String str2 = key2 + " >> " + key1 + "\r\n置信度:" + confidence2 + "\r\n";
					result.append(str1).append("\r\n").append(str2).append("\r\n");
					System.out.println(str1);
					System.out.println(str2);
				}
			}

		}
		File file = new File("D://apriori");

		file.mkdir();

		FileHelper.write(result.toString(), "D://apriori/result.txt");
	}

	/**
	 * 获取支持度
	 * 
	 * @param item
	 *            相关的项集
	 * @return
	 */
	private double getSupport(ItemSet item) {
		return item.getSupportCount() * 1.0 / tradeCount;
	}

}
