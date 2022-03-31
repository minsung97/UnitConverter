import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class UnitConverter {
	String[][] unitArray;
	String[][] prefixArray;
	int midindex = 0;

	String parseConstant(StringBuilder line) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < line.length(); i++) {
			if ((line.charAt(i) <= '9' && line.charAt(i) >= '0') || line.charAt(i) == '-' || line.charAt(i) == '.') {
				sb.append(line.charAt(i));
			} else {
				line.delete(0, i);
				return sb.toString();
			}
		}
		return null;
	}

	int[] parseExponent(StringBuilder line) {
		String[] phrase = line.toString().split("\\*|\\/|=\\?");
		int[] exponent = new int[phrase.length];

		// determine base exponent for each phrase
		// if it's located before first occurrence of '/', assign 1 for the all phrases
		// assign -1 otherwise
		// '=' sets the exponent sequence to 1, for that right side of equation starts
		int multiplier = 1;
		int count = 1;
		exponent[0] = multiplier;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '*') {
				exponent[count++] = multiplier;
			} else if (line.charAt(i) == '/') {
				multiplier *= -1;
				exponent[count++] = multiplier;
			} else if (line.charAt(i) == '=') {
				multiplier = 1;
				midindex = count;
				exponent[count++] = multiplier;
			}
		}

		// get dimension and multiply to base exponent
		for (int i = 0; i < exponent.length; i++) {
			int multiplier2 = 1;

			// search for pattern like ^(1), ^(-1), and ^1 (not ^-1)
			if (phrase[i].contains("^")) {
				String subString = phrase[i].substring(phrase[i].indexOf("^"));
				StringBuilder sb = new StringBuilder();
				if (Pattern.matches("\\^{1}(([0-9]{1})|(\\(-?[0-9]{1}\\)))", subString)) {
					for (int j = 0; j < phrase[i].length(); j++) {
						if (('0' <= phrase[i].charAt(j) && phrase[i].charAt(j) <= '9') || phrase[i].charAt(j) == '-') {
							sb.append(phrase[i].charAt(j));
						}
					}
				}
				multiplier2 = Integer.parseInt(sb.toString());
			}
			exponent[i] *= multiplier2;
		}

		return exponent;
	}

	StringBuilder[] parsePhrase(StringBuilder line) {
		String[] word = line.toString().split("\\*|\\/|=\\?");
		StringBuilder[] phrase = new StringBuilder[word.length];
		for (int i = 0; i < word.length; i++) {
			phrase[i] = new StringBuilder(word[i]);
		}
		return phrase;
	}

	StringBuilder[] parseBottom(StringBuilder[] phrase) {
		StringBuilder[] bottom = new StringBuilder[phrase.length];
		Pattern pattern = Pattern.compile("([a-z|A-Z|가-힇]+)");
		for (int i = 0; i < phrase.length; i++) {
			Matcher matcher = pattern.matcher(phrase[i]);
			if (matcher.find()) {
				bottom[i] = new StringBuilder(matcher.group());
			}
		}
		return bottom;
	}

	/**
	 * Return array of prefix names (0th element of every 2 element) from the prefix
	 * array that was previously read from file
	 * 
	 * @return array of prefix names
	 */
	String[] getPrefixName() {
		String[] unitName = new String[prefixArray.length];
		for (int i = 0; i < prefixArray.length; i++) {
			unitName[i] = prefixArray[i][0];
		}
		return unitName;
	}

	/**
	 * Return array of prefix names (0th element of every 2 element) from the prefix
	 * array that was previously read from file
	 * 
	 * @return array of prefix names
	 */
	String[] getUnitName() {
		String[] unitName = new String[unitArray.length];
		for (int i = 0; i < unitArray.length; i++) {
			unitName[i] = unitArray[i][0];
		}
		return unitName;
	}

	/**
	 * Takes array of bottom clause containing both unit and prefix, and return
	 * array of strings containing only one unit.
	 * 
	 * @param bottom
	 * @return unit
	 */
	String[] parseUnit(StringBuilder[] bottom) {
		unitArray = readUnitFile("unit.txt");
		String[] units = getUnitName();
		String[] unit = new String[bottom.length];
		for (int i = 0; i < bottom.length; i++) {
			for (int j = 0; j < units.length; j++) {
				if (bottom[i].toString().endsWith(units[j])) {
					unit[i] = units[j];
					// remove detected unit, leaving only prefix inside the bottom clause
					String onlyPrefix = bottom[i].reverse().toString()
							.replaceFirst(new StringBuilder(units[j]).reverse().toString(), "");
					bottom[i] = new StringBuilder(onlyPrefix);
					break;
				}
			}
			if (unit[i] == null) {
//                  System.out.println("No match for " + bottom[i]);
				return null;
			}
		}
		return unit;
	}

	/**
	 * Takes array of bottom clause containing both unit and prefix, and return
	 * array of strings containing null or one prefix.
	 * 
	 * @param bottom
	 * @return prefix
	 */
	String[] parsePrefix(StringBuilder[] bottom) {
		prefixArray = readPrefixFile("prefix.txt");
		String[] prefixs = getPrefixName();
		String[] prefix = new String[bottom.length];
		for (int i = 0; i < bottom.length; i++) {
			for (int j = 0; j < prefixs.length; j++) {
				if (bottom[i].toString().equals(prefixs[j])) {
					prefix[i] = prefixs[j];
				}
			}
		}
		return prefix;
	}

	/**
	 * Count and return line number of given file
	 * 
	 * @param aFile
	 * @return line number of file
	 * @throws IOException
	 */
	int countLines(File aFile) throws IOException {
		LineNumberReader reader = null;
		try {
			reader = new LineNumberReader(new FileReader(aFile));
			while ((reader.readLine()) != null)
				;
			return reader.getLineNumber();
		} catch (Exception ex) {
			return -1;
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/**
	 * Read the file of given path and return all of its unit data
	 * 
	 * @param path to file
	 * @return 2-dimensional array of unit data
	 */
	String[][] readUnitFile(String path) {
		File file = new File(path);
		int lineNo = 0;
		try {
			lineNo = countLines(file);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String[][] unitArray = new String[lineNo][5];

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));) {
			String line = null;
			int i = 0;
			while ((line = br.readLine()) != null) {
				unitArray[i++] = line.split("\t");
			}
			return unitArray;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Read the file of given path and return all of its prefix data
	 * 
	 * @param path to file
	 * @return 2-dimensional array of prefix data
	 */
	String[][] readPrefixFile(String path) {
		File file = new File(path);
		int lineNo = 0;
		try {
			lineNo = countLines(file);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String[][] prefixArray = new String[lineNo][2];

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));) {
			String line = null;
			int i = 0;
			while ((line = br.readLine()) != null) {
				prefixArray[i++] = line.split("\t");
			}
			return prefixArray;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Take in an unit and compare with unit preset. If the match for the unit found
	 * in the preset, return dimension for g, m, s and ratio for proper conversion.
	 * If no match, return null.
	 * 
	 * @param unit
	 * @return gmsr
	 */
	String[] solveDimension(String unit) {
		String[] gmsr = new String[4];
		for (int i = 0; i < unitArray.length; i++) {
			if (unit.equals(unitArray[i][0])) {
				gmsr[0] = unitArray[i][1];
				gmsr[1] = unitArray[i][2];
				gmsr[2] = unitArray[i][3];
				gmsr[3] = unitArray[i][4];
			}
		}
		return gmsr;
	}

	/**
	 * @param unit
	 * @param exponent
	 * @return
	 */
	BigDecimal aggregateDimension(String[] unit, int[] exponent) {
		String[] left = new String[] { "0", "0", "0", "1" };
		String[] right = new String[] { "0", "0", "0", "1" };

		// aggregating left units
		for (int i = 0; i < midindex; i++) {
			String[] gms = solveDimension(unit[i]);
			BigDecimal g = new BigDecimal(gms[0]).multiply(new BigDecimal(exponent[i]));
			BigDecimal m = new BigDecimal(gms[1]).multiply(new BigDecimal(exponent[i]));
			BigDecimal s = new BigDecimal(gms[2]).multiply(new BigDecimal(exponent[i]));
			BigDecimal r = new BigDecimal(gms[3]).pow(Math.abs(exponent[i]));
			left[0] = new BigDecimal(left[0]).add(g).toString(); // g
			left[1] = new BigDecimal(left[1]).add(m).toString(); // m
			left[2] = new BigDecimal(left[2]).add(s).toString(); // s
			if (exponent[i] >= 0)
				left[3] = new BigDecimal(left[3]).multiply(r).toString(); // ratio needed to convert
			else
				left[3] = new BigDecimal(left[3]).divide(r, 100, RoundingMode.FLOOR).toString(); // ratio needed
			// to
			// convert

		}

		for (int i = midindex; i < unit.length; i++) {
			String[] gms = solveDimension(unit[i]);
			BigDecimal g = new BigDecimal(gms[0]).multiply(new BigDecimal(exponent[i]));
			BigDecimal m = new BigDecimal(gms[1]).multiply(new BigDecimal(exponent[i]));
			BigDecimal s = new BigDecimal(gms[2]).multiply(new BigDecimal(exponent[i]));
			BigDecimal r = new BigDecimal(gms[3]).pow(Math.abs(exponent[i]));
			right[0] = new BigDecimal(right[0]).add(g).toString();
			right[1] = new BigDecimal(right[1]).add(m).toString();
			right[2] = new BigDecimal(right[2]).add(s).toString();
			if (exponent[i] >= 0)
				right[3] = new BigDecimal(right[3]).multiply(r).toString(); // ratio needed to convert
			else
				right[3] = new BigDecimal(right[3]).divide(r, 100, RoundingMode.FLOOR).toString(); // ratio
			// needed to
			// convert
		}

		// comapre total gms of the left side with of the right
		for (int i = 0; i < 3; i++) {
			if (!left[i].equals(right[i]))
				return null;
		}

		return new BigDecimal(left[3]).divide(new BigDecimal(right[3]), 100, RoundingMode.FLOOR);
	}

	BigDecimal aggregatePrefix(String[] prefix, int[] exponent, StringBuilder[] phrase) {
		int left = 0;
		int right = 0;

		// aggregating prefixs on left side
		for (int i = 0; i < midindex; i++) {
			if (prefix[i] != null) {
				for (int j = 0; j < prefixArray.length; j++) {
					if (prefix[i].equals(prefixArray[j][0])) {
//                  //if a phrase contains a () clause, its prefix
//                  //must be powered by its exponent         
						if (phrase[i].toString().contains("("))
							left += Integer.parseInt(prefixArray[j][1]) * exponent[i];
						else {
							if (exponent[i]>=0)
								left += Integer.parseInt(prefixArray[j][1]);
							else 
								left -= Integer.parseInt(prefixArray[j][1]);
						}
					}
				}
			}
		}

		// aggregating prefixs on right side
		for (int i = midindex; i < prefix.length; i++) {
			if (prefix[i] != null)
				for (int j = 0; j < prefixArray.length; j++) {
					if (prefix[i].equals(prefixArray[j][0])) {
						if (phrase[i].toString().contains("("))
							right += Integer.parseInt(prefixArray[j][1]) * exponent[i];
						else {
							if (exponent[i]>=0)
								right += Integer.parseInt(prefixArray[j][1]);
							else
								right -= Integer.parseInt(prefixArray[j][1]);
						}
					}
				}
		}

		return new BigDecimal(left - right);
	}

	void convert(String input) {

		StringBuilder line = new StringBuilder(input);

		String constant = parseConstant(line);
		int[] exponent = parseExponent(line);
		StringBuilder[] phrase = parsePhrase(line);
		StringBuilder[] bottom = parseBottom(phrase);
		String[] unit = parseUnit(bottom);
		if (unit == null) {
			System.out.println("단위가 존재하지 않습니다");
			return;
		}
		String[] prefix = parsePrefix(bottom);
		BigDecimal totalConnstant = new BigDecimal(constant);
		BigDecimal totalRatio = aggregateDimension(unit, exponent);
		if (totalRatio == null) {
			System.out.println("변환 차원이 맞지 않습니다");
			return;
		}

		int totalPrefix = aggregatePrefix(prefix, exponent, phrase).intValue();
		BigDecimal totalMultiplier = new BigDecimal(1);
		for (int i = 0; i < Math.abs(totalPrefix); i++)
			if (totalPrefix >= 0)
				totalMultiplier = totalMultiplier.multiply(new BigDecimal(10));
			else
				totalMultiplier = totalMultiplier.divide(new BigDecimal(10), 100, RoundingMode.FLOOR);
		BigDecimal answer = totalRatio.multiply(totalConnstant).multiply(totalMultiplier);
		DecimalFormat decimalFormat = new DecimalFormat();
		decimalFormat.setMaximumFractionDigits(100);
		decimalFormat.setRoundingMode(RoundingMode.DOWN);
		System.out.println(decimalFormat.format(answer));
		System.out.println("변환에 성공하였습니다.");
	}
}

class UserUnit {
	UnitConverter uc = new UnitConverter();
	String[][] unitArray = uc.readUnitFile("unit.txt");
	File file = new File("unit.txt");
	BufferedReader br;
	FileWriter fw;

	void addUnit(String input) {
		try {
			br = new BufferedReader(new FileReader(file));
			String temptext = "";
			String filetext = "";
			while ((temptext = br.readLine()) != null) {
				filetext += (temptext + "\r\n");
			}
			filetext += input;
			fw = new FileWriter("unit.txt");
			fw.write(filetext);
			fw.close();
			System.out.println("사용자 지정 단위가 추가되었습니다");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void editUnit(String input) {
		int i = 0;
		int check = 0;

		String[] units = new String[unitArray.length];
		for (int j = 0; j < unitArray.length; j++) {
			units[j] = unitArray[j][0];
		}

		for (i = 0; i < units.length; i++) {
			if (input.equals(units[i])) {
				check = 1;
				System.out.println("단위를 수정해 주십시오.");
				Scanner sc = new Scanner(System.in);
				String edit = sc.nextLine();
				String temptext = "";
				String filetext = "";
				try {
					br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
					// FileOutputStream(file)));

					for (int j = 0; j < i; j++) {
						temptext = br.readLine();
						filetext += temptext + "\r\n";
					}

					filetext = filetext + edit + "\r\n";
					String delete = br.readLine();

					while ((temptext = br.readLine()) != null) {
						filetext += (temptext + "\r\n");
					}
					fw = new FileWriter("unit.txt");
					fw.write(filetext);
					fw.close();
					br.close();
					System.out.println("해당 사용자 단위가 수정되었습니다.");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		if (check == 0)
			System.out.println("사용자 단위가 존재하지 않습니다.");
	}

	void deleteUnit(String input) {
		int i = 0;
		int check = 0;

		String[] units = new String[unitArray.length];
		for (int j = 0; j < unitArray.length; j++) {
			units[j] = unitArray[j][0];
		}

		for (i = 0; i < units.length; i++) {
			if (input.equals(units[i])) {
				check = 1;
				String temptext = "";
				String filetext = "";
				try {
					br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

					for (int j = 0; j < i; j++) {
						temptext = br.readLine();
						filetext += temptext + "\r\n";
					}
					String delete = br.readLine();

					while ((temptext = br.readLine()) != null) {
						filetext += (temptext + "\r\n");
					}
					fw = new FileWriter("unit.txt");
					fw.write(filetext);

					fw.close();
					br.close();
					System.out.println("해당 사용자 단위를 삭제했습니다.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (check == 0)
			System.out.println("사용자 단위 삭제가 취소되었습니다");
	}

}

/**
 * Start main menu
 */
class JucMenu {
	Scanner scanner = new Scanner(System.in);

	public void showManual() {
		System.out.println("   Converter               Manual               JUC\n");
		System.out.println("NAME\n");
		System.out.println("   JUC -- JAVA Unit Converter\n");
		System.out.println("SPECIFICATIONS\n");
		System.out.println("   in OS X 10.14.3 , Windows 10, JRE ver 11.8.0 , Javac 11.0.2 Tested\n");
		System.out.println("   When  text error occured(Maybe UTF-8 supported, but CP949 not supported text).\n");
		System.out.println("   You should fix eclipse Text File Encoding setting!!\n");
		System.out.println(
				"   To change the setting, go to project properities setting and change Text File Encoding to UTF-8.\n");
		System.out.println("HOW TO USE CONVERTER\n");
		System.out.println("   1. To use converter, you should follow this type.\n");
		System.out.println("      (NUMBER) (UNIT) = ? (UNIT)\n");
		System.out.println("   2. Unit should contain only one or less prefix.\n");
		System.out.println("      ex) km         O");
		System.out.println("          kkkm      X\n");
		System.out.println("   3. If your input wasn't right, program will print the warning message.\n");
		System.out.println("      ex) 1m=?kg");
		System.out.println("               변환 차원이 맞지 않습니다.");
		System.out.println("      ex) 1M=?km");
		System.out.println("               단위가 존재하지 않습니다.\n");
		System.out.println("   4. If warning messages are printed, you should type q to return to the menu.\n");
		System.out.println("   5. If converter worked, converted number and success message will be printed.\n");
		System.out.println("      ex) 1km=?m");
		System.out.println("          1000");
		System.out.println("               변환에 성공하였습니다.\n");
		System.out.println("   6. If success messages are printed, you should type q to return to the menu.\n");
		System.out.println("   7. If you don't follow this manual, normal operation is not guaranteed.\n");
		System.out.println("HOW TO USE USER UNIT MANAGER\n");
		System.out.println("   Program's base unit : (MASS BASIC UNIT)g , (LENGTH BASIC UNIT)m , (TIME BASIC UNIT)s\n");
		System.out.println("   1. To add user unit\n");
		System.out.println("      (a) You should follow this type.\n");
		System.out.println("         (USER UNIT)   (MASS ORDER)   (LENGTH ORDER)   (TIME ORDER)   (BASIC NUMBER)\n");
		System.out.println("      (b) You should type \"tab\" between every words.\n");
		System.out.println("      (c) Basic number should be interpreted like this.\n");
		System.out.println("          (BASIC NUMBER)(USER UNIT) = 1 (MASS ORDER)*(LENGTH ORDER)*(TIME ORDER)\n");
		System.out.println(
				"      (d) If success or warning messages are printed,  you should type q to return to the menu.\n");
		System.out.println("   2. To modify user unit\n");
		System.out.println("      (a) You should search unit's name.\n");
		System.out.println("         (USER UNIT)\n");
		System.out.println("      (b) If searched unit exists, you should follow this type.\n");
		System.out.println("         (USER UNIT)   (MASS ORDER)   (LENGTH ORDER)   (TIME ORDER)   (BASIC NUMBER)\n");
		System.out.println("      (c) Basic number should be interpreted like this.\n");
		System.out.println("         (BASIC NUMBER)(USER UNIT) = 1 (MASS ORDER)*(LENGTH ORDER)*(TIME ORDER)\n");
		System.out.println(
				"      (d) If success or warning messages are printed,  you should type q to return to the menu.\n");
		System.out.println("   3. To delete user unit\n");
		System.out.println("      (a) You should search unit's name.\n");
		System.out.println("         (USER UNIT)\n");
		System.out.println(
				"      (b) If success or warning messages are printed,  you should type q to return to the menu.\n");
		System.out.println("COPYRIGHT\n");
		System.out.println("   Copyright 2019 JUC All Rights Reserved \n");

	}

	void MainMenu() {
		String repeat = "q";
		while (repeat.equals("q")) {
			boolean doloop = true;
			System.out.println("1. 단위 변환");
			System.out.println("2. 사용자 단위 설정");
			System.out.println("3. 메뉴얼 출력");
			System.out.println("4. 종료");

			String choice_Menu = scanner.next();
			if (choice_Menu.length() == 0)
				continue;
			scanner.nextLine();
			if (choice_Menu == null) {
				return;
			} else {
				switch (choice_Menu) {
				case "1": {
					String input;
					while ((input = scanner.nextLine()).length() == 0) {
					}
					input = input.replaceAll(" ", "");
					UnitConverter uc = new UnitConverter();
					uc.convert(input);
					break;
				}
				case "2": {
					while (doloop) {
						System.out.println("1. 사용자 단위 추가");
						System.out.println("2. 사용자 단위 수정");
						System.out.println("3. 사용자 단위 삭제");
						String choice_Manage = scanner.next();
						UserUnit uu = new UserUnit();
						scanner.nextLine();
						if (choice_Manage.length() == 0)
							continue;
						if (choice_Manage == null) {
							return;
						} else {
							switch (choice_Manage) {
							case "1": {
								String input = scanner.nextLine();
								uu.addUnit(input);
								doloop = false;
								break;
							}
							case "2": {
								System.out.println("수정할 단위의 이름을 입력하세요.");
								String input = scanner.nextLine();
								uu.editUnit(input);
								doloop = false;
								break;
							}
							case "3": {
								System.out.println("삭제할 단위의 이름을 입력하세요.");
								String input = scanner.nextLine();
								uu.deleteUnit(input);
								doloop = false;
								break;
							}
							default: {
								break;
							}
							}
						} // switch
					} // else
					break;
				}
				case "3": {
					showManual();
					break;
				}
				case "4": {
					return;
				}
				default: {
					continue;
				}
				}
				while (true) {
					repeat = scanner.next();
					if (repeat.equals("q"))
						break;
					else
						continue;
				}
			}
		}
	}
}

public class Converter {

	public static void main(String[] args) {
		JucMenu menu = new JucMenu();
		menu.MainMenu();
	}
}