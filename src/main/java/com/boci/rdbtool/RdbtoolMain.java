package com.boci.rdbtool;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.boci.rdbtool.util.Entry;
import com.boci.rdbtool.util.KeyValuePair;
import com.boci.rdbtool.util.RdbParser;

public class RdbtoolMain {

	private static int SINGLE_SIZE = 500000;
	private static int EXPORT_CSV_SIZE = 10000;
	private static int CONTENT_LENGTH = 30000;

	private static byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

	public static void main(String[] args) {
		if (null == args || args.length < 2) {
			System.out.println("{'errCd':'1000','errMsg':'未传入参数或入参不完整,调用失败','results':''}");
			return;
		}
		if (null == args[0] || null == args[1]) {
			System.out.println("{'errCd':'1000','errMsg':'未传入参数或入参不完整,调用失败','results':''}");
			return;
		}
		String path = args[0];
		String csvPath = args[1];
		String[] keys = null;
		if (args.length >= 3) {
			keys = args[2].split(",");
		}
		System.out.println(generateCsv(path, csvPath, keys));
	}

	public static String generateCsv(String path, String csvPath, String[] keys) {
		if (!path.endsWith(".rdb")) {
			return "{'errCd':'1002','errMsg':'rdb文件不存在','results':''}";
		}
		File infile = new File(path);
		if (!infile.exists()) {
			return "{'errCd':'1002','errMsg':'rdb文件不存在','results':''}";
		}
		// 生成文件夹不存在，创建文件夹
		File csvpathfile = new File(csvPath);
		if (!csvpathfile.exists()) {
			csvpathfile.mkdir();
		}
		int count = 0, singleSize = 0, fileIndex = 1;

		try (RdbParser parser = new RdbParser(path)) {
			CSVPrinter csvPrinter = createCsvPrinter(
					csvPath + infile.getName() + "_" + String.valueOf(fileIndex) + ".csv");

			boolean filterflag = false;
			if (keys != null && keys.length > 0) {
				filterflag = true;
			}

			List<Object[]> contentList = new ArrayList<Object[]>();
			Entry e;
			while ((e = parser.readNext()) != null) {
				String type = "", key = "";
				StringBuilder values = new StringBuilder();
				switch (e.getType()) {
				case KEY_VALUE_PAIR:
					KeyValuePair kvp = (KeyValuePair) e;
					key = new String(kvp.getKey(), "UTF-8");
					type = kvp.getValueType().toString();

					for (byte[] val : kvp.getValues()) {
						values = values.append(new String(val, "UTF-8")).append(" ");
					}
					if (values == null || values.length() == 0) {
						break;
					}

					if (filterflag) {
						for (String settingKey : keys) {
							if (key.contains(settingKey)) {
								contentList.add(buildContent(type, key, values));
								break;
							}
						}
					} else {
						contentList.add(buildContent(type, key, values));
					}

					if (contentList.size() > EXPORT_CSV_SIZE) {
						try {
							if (singleSize > SINGLE_SIZE) {
								csvPrinter.close();
								fileIndex++;
								singleSize = 0;
								csvPrinter = createCsvPrinter(
										csvPath + infile.getName() + "_" + String.valueOf(fileIndex) + ".csv");
							}
							saveCsv(contentList, csvPrinter);
							count = count + contentList.size();
							singleSize = singleSize + contentList.size();
						} catch (Exception e2) {
							return "{'errCd':'9002','errMsg':'生成csv文件异常,该文件rdb路径:" + path + ",需生成到的csv路径:" + csvPath
									+ "','results':''}";
						}
						contentList = new ArrayList<Object[]>();
					}
					break;

				default:
					break;
				}
			}
			if (contentList.size() > 0) {
				try {
					if (singleSize > SINGLE_SIZE) {
						csvPrinter.close();
						fileIndex++;
						singleSize = 0;
						csvPrinter = createCsvPrinter(
								csvPath + infile.getName() + "_" + String.valueOf(fileIndex) + ".csv");
					}
					saveCsv(contentList, csvPrinter);
					count = count + contentList.size();
					singleSize = singleSize + contentList.size();
				} catch (Exception e2) {
					return "{'errCd':'9002','errMsg':'生成csv文件异常,该文件rdb路径:" + path + ",需生成到的csv路径:" + csvPath
							+ "','results':''}";
				}
			}
			csvPrinter.close();
		} catch (IOException e) {
			return "{'errCd':'9001','errMsg':'rdb文件解析异常,该文件rdb路径:" + path + ",需生成到的csv路径:" + csvPath
					+ "','results':''}";
		}
		return "{'errCd':'0','errMsg':'','results':'" + count + "','fileCount':'" + fileIndex + "'}";

	}

	private static void saveCsv(List<Object[]> contentList, CSVPrinter csvPrinter) throws IOException {
		for (Object[] item : contentList) {
			csvPrinter.printRecord(item);
		}
		csvPrinter.flush();
	}

	private static CSVPrinter createCsvPrinter(String path) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
		writer.write(new String(bom));
		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
		return csvPrinter;

	}

	/**
	 * 
	 * @param type
	 * @param key
	 * @param values
	 * @return
	 */
	private static String[] buildContent(String type, String key, StringBuilder values) {
		if (values != null && values.length() > CONTENT_LENGTH) {
			List<String> content = new ArrayList<String>();
			content.add(type);
			content.add(key);
			int length = 0;
			while (length < values.length()) {
				if (values.length() > length + CONTENT_LENGTH) {
					content.add(values.substring(length, length + CONTENT_LENGTH).toString());
				} else {
					content.add(values.substring(length, values.length()).toString());
				}
				length = length + CONTENT_LENGTH;
			}
			return toArray(content);
		}
		return new String[] { type, key, values.toString() };
	}

	private static String[] toArray(List<String> content) {
		String[] result = new String[content.size()];
		for (int i = 0; i < content.size(); i++) {
			result[i] = content.get(i);
		}
		return result;
	}

}
