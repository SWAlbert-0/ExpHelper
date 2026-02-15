package fjnu.edu.common.utils.excle;

import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;


public class ExcelUtil {
	String exclFileName;// 带路径的文件名
	File excelFile = null;// excel文件
	FileInputStream exclFileStream = null;// excel文件输入流
	XSSFWorkbook workBook = null;// excel工作簿
	ExcelUtilHlper hlper = null;// 助手类

	public ExcelUtil(String exclFileName) throws IOException {// 构造方法
		this.exclFileName = exclFileName;
		excelFile = new File(exclFileName);
		if (!excelFile.exists()) {
			workBook = new XSSFWorkbook();
		} else {
			exclFileStream = new FileInputStream(excelFile);
			workBook = new XSSFWorkbook(exclFileStream);
		}

		hlper = new ExcelUtilHlper(workBook, excelFile, exclFileStream);
	};

	/**
	 * 返回工作单名字列表
	 */
	public ArrayList<String> getSheetNames() {
		ArrayList<String> sheetNames = new ArrayList<String>();
		int size = workBook.getNumberOfSheets();// 获取工作单数量
		for (int i = 0; i < size; i++) {
			sheetNames.add(workBook.getSheetName(i));
		}
		return sheetNames;
	}

	/**
	 * 返回指定名字的工作单，若不存在则创建一个新的工作单
	 * 
	 * @param sheetName
	 *            :工作单名字
	 */
	public XSSFSheet getSheet(String sheetName) {
		XSSFSheet curSheet = null;
		ArrayList<String> sheetNames = getSheetNames();
		if (sheetNames.contains(sheetName)) {
			int idx = workBook.getSheetIndex(sheetName);
			curSheet = workBook.getSheetAt(idx);
		} else {
			curSheet = workBook.createSheet(sheetName);
		}
		return curSheet;
	}

	/**
	 * 清除指定工作单的内容，工作单本身还存在
	 * 
	 * @param sheetName
	 *            ：工作单名字
	 * @throws IOException
	 */
	public void clearSheet(String sheetName) throws IOException {
		XSSFSheet curSheet = getSheet(sheetName);
		int rowNum = getMaxRowNum(sheetName);
		for (int i = 0; i < rowNum; i++) {
			XSSFRow curRow = curSheet.getRow(i);
			if (curRow != null) {
				curSheet.removeRow(curRow);
			}
		}
		hlper.writeFile();
	}

	/**
	 * 删除一个指定的工作单
	 * 
	 * @param sheetName
	 *            ：工作单名字
	 */
	public void removeSheet(String sheetName) {
		ArrayList<String> sheetNames = getSheetNames();
		if (sheetNames.contains(sheetName)) {
			int idx = workBook.getSheetIndex(sheetName);
			workBook.removeSheetAt(idx);
		}
	}

	/**
	 * 打开excel文件
	 * 
	 * @throws IOException
	 */
	public void open() throws IOException {
		Runtime.getRuntime().exec("soffice  " + exclFileName);
	}

	/**
	 * 返回工作单的最大行号,含标题行
	 * 
	 * @param sheetName
	 *            ：工作单的名字
	 */
	public int getMaxRowNum(String sheetName) {
		XSSFSheet curSheet = getSheet(sheetName);
		int maxRowNum = hlper.getMaxRowNum(curSheet);
		return maxRowNum;
	}

	/**
	 * 返回指定工作单最大列号，含标题行
	 * 
	 * @param sheetName
	 *            ：工作单名字
	 * @return
	 */
	public int getMaxColNum(String sheetName) {
		XSSFSheet curSheet = getSheet(sheetName);
		int maxColNum = hlper.getMaxColNum(curSheet);
		return maxColNum;
	}

	/**
	 * 返回数据行的行数
	 * 
	 * @param sheetName
	 *            ：工作单名字
	 * @return
	 */
	public int getDataRowNum(String sheetName) {
		int dataRowNum = getMaxRowNum(sheetName) - 1;
		return dataRowNum;
	}

	/**
	 * 读指定worksheet的第rowNum行第colNum列的内容，以字符串形式返回，
	 * 若读不出或用户输入的数据类型与该列定义的数据类型不一致，均返回null
	 * 
	 * @param sheetName
	 *            ：工作单的名字
	 * @param rowIdx
	 *            ：行号
	 * @param colIdx
	 *            ：列号
	 */
	public String read(String sheetName, int rowIdx, int colIdx) {
		XSSFSheet sheet = getSheet(sheetName);
		String value = hlper.read(sheet, rowIdx, colIdx);
		return value;
	}

	/**
	 * 返回第一行的标题行，若不存则返回空列表
	 * 
	 * @param sheetName
	 *            :工作单名字
	 * @return
	 */
	public ArrayList<String> readTitleRow(String sheetName) {
		XSSFSheet sheet = getSheet(sheetName);
		ArrayList<String> titleRow = hlper.readTitleRow(sheet);
		return titleRow;
	}

	/**
	 * 读取从起始行至结束行之间指定一列的数据
	 * 
	 * @param sheetName
	 *            :工作单名字
	 * @param colIdx
	 *            :列下标
	 * @param frmRowIdx
	 *            :起始行下标
	 * @param toRowIdx
	 *            :结束行下标
	 * @return
	 */
	public ArrayList<String> readDataCol(String sheetName, int colIdx,
			int frmRowIdx, int toRowIdx) {
		XSSFSheet sheet = getSheet(sheetName);
		ArrayList<String> colData = hlper.readDataCol(sheet, colIdx, frmRowIdx,
				toRowIdx);
		return colData;
	}

	/**
	 * 返回指定工作单中所有数据行（不含标题行）
	 * 
	 * @param sheetName
	 *            ：工作单的名字
	 * @return
	 */
	public ArrayList<ArrayList<String>> readDataRows(String sheetName) {
		XSSFSheet curSheet = getSheet(sheetName);
		ArrayList<ArrayList<String>> dataRows = hlper.readDataRows(curSheet);
		return dataRows;
	}

	/**
	 * 返回Excel数据，包括标题行数据和所有数据行数据
	 * 
	 * @param sheetName
	 *            :工作单名字
	 * @return
	 */
	public ExclData readExcl(String sheetName) {
		XSSFSheet sheet = getSheet(sheetName);
		ArrayList<String> titleRow = hlper.readTitleRow(sheet);
		ArrayList<ArrayList<String>> dataRows = hlper.readDataRows(sheet);
		ExclData exclData = new ExclData(titleRow, dataRows);
		return exclData;
	}

	/**
	 * 将数据元素列表中的数据写入到指定区域
	 * 
	 * @param sheetName
	 *            :工作单名字
	 * @param frmRowIdx
	 *            :起始行下标
	 * @param toRowIdx
	 *            :结束行下标
	 * @param frmColIdx
	 *            :起始列下标
	 * @param toColIdx
	 *            :结束列下标
	 * @param dataElems
	 *            :数据元素列表
	 * @throws IOException
	 */
	public void writeAreaData(String sheetName, int frmRowIdx, int toRowIdx,
			int frmColIdx, int toColIdx, ArrayList<ArrayList<String>> dataElems)
			throws IOException {
		XSSFSheet sheet = getSheet(sheetName);
		hlper.writeAreaData(sheet, frmRowIdx, toRowIdx, frmColIdx, toColIdx,
				dataElems);
	}

	/**
	 * 将表格写入excel指定工作单，如存在原工作单，则先清除内容后再写入
	 * 
	 * @param sheetName
	 *            ：工作单名字
	 * @param titleRow
	 *            ：标题行
	 * @param dataRows
	 *            ：数据行
	 * @throws IOException
	 */
	public void writeTbl(String sheetName, ArrayList<String> titleRow,
			ArrayList<ArrayList<String>> dataRows) throws IOException {
		removeSheet(sheetName);
		XSSFSheet curSheet = getSheet(sheetName);
		hlper.writeTbl(curSheet, titleRow, dataRows);
	}

	/**
	 * 写入Excel数据
	 * 
	 * @param sheetName
	 *            ：工作单
	 * @param exclData
	 *            ：excel数据，包括标题行数据和所有数据行数据
	 * @throws IOException
	 */
	public void write(String sheetName, ExclData exclData) throws IOException {
		removeSheet(sheetName);
		XSSFSheet curSheet = getSheet(sheetName);
		ArrayList<String> titleRow = exclData.getTitleRow();
		ArrayList<ArrayList<String>> dataRows = exclData.getDataRows();
		hlper.writeTbl(curSheet, titleRow, dataRows);
	}
	/**
	 * 获取单元格风格对象，每调用一次返回一个新对象
	 * @return
	 */
	public XSSFCellStyle getCellStyle(){
		XSSFCellStyle style = workBook.createCellStyle();
		return style;
	}
	
	/**
	 * 将内存中的内容写入Excel文件
	 * @throws IOException
	 */
	public void writeFile() throws IOException {
		hlper.writeFile();
	}


	public void close() throws IOException {
		workBook.close();
		excelFile = null;
		if (exclFileStream != null) {
			exclFileStream.close();
		}
	}

	public String getExclFileName() {
		return exclFileName;
	}

}
