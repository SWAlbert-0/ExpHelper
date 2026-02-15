package fjnu.edu.common.utils.excle;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ExcelUtilHlper {
	XSSFWorkbook workBook = null;// 工作薄
	File excelFile = null;
	FileInputStream exclFileStream = null;

	public ExcelUtilHlper(XSSFWorkbook workBook, File excelFile,
			FileInputStream exclFileStream) {
		this.workBook = workBook;
		this.excelFile = excelFile;
		this.exclFileStream = exclFileStream;
	}

	/**
	 * 返回工作单的最大行号,含标题行
	 * 
	 * @param curSheet
	 *            ：工作单的名字
	 */
	public int getMaxRowNum(XSSFSheet curSheet) {
		int maxRowNum = 0;
		int idx = curSheet.getLastRowNum();// curSheet.getPhysicalNumberOfRows();
		if (idx >= 0) {
			XSSFRow curRow = curSheet.getRow(0);
			try {
				curRow.getCell(0);
				maxRowNum = idx + 1;
			} catch (Exception e) {
				maxRowNum = 0;
			}
		}
		return maxRowNum;
	}

	/**
	 * 将指定单元格中的内容根据其数据类型转换成字符串
	 * 
	 * @param cell
	 *            ：单元格
	 */
	public String readCell(Cell cell) {
		String value = null;
		if (cell != null) {
			CellType dataType = cell.getCellType();
			switch (dataType) {
			case BOOLEAN:
				value = String.valueOf(cell.getBooleanCellValue());
				break;
			case NUMERIC:
				double doubleVal = cell.getNumericCellValue();
				value = String.valueOf(doubleVal);
				break;
			case STRING:
				value = cell.getStringCellValue();
				break;
			default:
				try {
					value = cell.getStringCellValue();
				} catch (Exception e) {
					value = "";
				}
				break;
			}
		}
		return value;
	}

	/**
	 * 将指定内容按单元格的数据类型写入
	 * 
	 * @param cell
	 *            ：单元格
	 * @param curVal
	 *            ：写入值
	 */
	public void writeCell(Cell cell, String curVal) {
		CellType dataType = cell.getCellType();
		switch (dataType) {
		case BOOLEAN:
			cell.setCellValue(Boolean.parseBoolean(curVal));
			break;
		case NUMERIC:
			cell.setCellValue(Double.parseDouble(curVal));
			break;
		case STRING:
			cell.setCellValue(curVal);
			break;
		default:
			try {
				cell.setCellValue(curVal);
			} catch (Exception e) {
				cell.setCellValue("");
			}
			break;
		}
	}

	/**
	 * 读取指定工作单中由行列下标所确定的单元格的内容，若读取不到返回null
	 * 
	 * @param curSheet
	 *            ：工作单
	 * @param rowIdx
	 *            ：行下标
	 * @param colIdx
	 *            ：列下标
	 */
	public String read(XSSFSheet curSheet, int rowIdx, int colIdx) {
		String value = null;
		XSSFRow row = curSheet.getRow(rowIdx);
		if (row != null) {
			Cell cell = row.getCell(colIdx);
			value = readCell(cell);
		}
		return value;
	}

	public int getMaxColNum(XSSFSheet curSheet) {
		int maxColNum = 0;
		int maxRowNum = getMaxRowNum(curSheet);
		for (int i = 0; i < maxRowNum; i++) {
			int curColNum = 0;
			XSSFRow curRow = curSheet.getRow(i);
			if (curRow != null) {
				int idx = curRow.getLastCellNum();
				try {
					curRow.getCell(0);
					curColNum = idx;
				} catch (Exception e) {
					curColNum = 0;
				}
			}
			if (curColNum > maxColNum) {
				maxColNum = curColNum;
			}
		}
		return maxColNum;
	}

	/**
	 * 返回第一行的标题行，若不存则返回空列表
	 * 
	 * @param curSheet
	 *            :工作单名字
	 * @return
	 */
	public ArrayList<String> readTitleRow(XSSFSheet curSheet) {
		ArrayList<String> titleRow = new ArrayList<String>();
		int maxRowNum = getMaxRowNum(curSheet);
		int maxColNum = getMaxColNum(curSheet);
		if (maxRowNum > 0) {
			XSSFRow curRow = curSheet.getRow(0);
			for (int j = 0; j < maxColNum; j++) {
				Cell cell = null;
				String value = null;
				try {
					cell = curRow.getCell(j);
				} catch (Exception e) {

				}
				if (cell != null) {
					value = readCell(cell);
				} else {
					value = "";
				}
				titleRow.add(value);
			}
		}
		return titleRow;

	}

	/**
	 * 返回一列数据，但不包括列标题
	 * 
	 * @param curSheet
	 *            :当前工作单
	 * @param colIdx
	 *            ：列下标
	 * @return
	 */
	public ArrayList<String> readDataCol(XSSFSheet curSheet, int colIdx,
			int frmRowIdx, int toRowIdx) {
		ArrayList<String> colData = new ArrayList<String>();
		int maxColNum = getMaxColNum(curSheet);
		int maxRowNum = getMaxRowNum(curSheet);
		boolean colCond = colIdx >= 0 && colIdx < maxColNum;// 列约束
		boolean rowCond = (frmRowIdx >= 1) && (frmRowIdx <= toRowIdx)
				&& (toRowIdx < maxRowNum);// 列约束
		if (colCond && rowCond) {
			for (int i = frmRowIdx; i <= toRowIdx; i++) {
				XSSFRow curRow = curSheet.getRow(i);
				Cell cell = null;
				String value = null;
				try {
					cell = curRow.getCell(colIdx);
				} catch (Exception e) {
					value = "";
				}
				if (cell != null) {
					value = readCell(cell);
				} else {
					value = "";
				}
				colData.add(value);
			}
		}
		return colData;
	}

	/**
	 * 返回指定工作单所有数据行的内容，但不包括标题行
	 * 
	 * @param curSheet
	 *            ：当前工作单
	 */
	public ArrayList<ArrayList<String>> readDataRows(XSSFSheet curSheet) {
		ArrayList<ArrayList<String>> dataRows = new ArrayList<ArrayList<String>>();
		int maxRowNum = getMaxRowNum(curSheet);
		int maxColNum = getMaxColNum(curSheet);
		for (int i = 1; i < maxRowNum; i++) {
			ArrayList<String> dataRow = new ArrayList<String>();
			XSSFRow curRow = curSheet.getRow(i);
			for (int j = 0; j < maxColNum; j++) {
				Cell cell = null;
				String value = null;
				try {
					cell = curRow.getCell(j);
					//cell.setCellType(CellType.STRING);
				} catch (Exception e) {
					value = "";
				}
				if (cell != null) {
					value = readCell(cell);
				} else {
					value = "";
				}
				dataRow.add(value);
			}
			dataRows.add(dataRow);
		}
		return dataRows;
	}

	public void writeFile() throws IOException {
		String fileName = excelFile.getAbsolutePath();
		FileOutputStream fileOut = new FileOutputStream(fileName);
		workBook.write(fileOut);
		fileOut.close();
	}

	/**
	 * 将数据元素列表中的数据写入到指定区域
	 * 
	 * @param curSheet
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
	public void writeAreaData(XSSFSheet curSheet, int frmRowIdx, int toRowIdx,
			int frmColIdx, int toColIdx, ArrayList<ArrayList<String>> dataElems)
			throws IOException {

		boolean rowCond = (frmRowIdx >= 1) && (frmRowIdx <= toRowIdx);// 行下标约束
		boolean colCond = (frmColIdx >= 0) && (frmColIdx <= toColIdx);
		Cell cell = null;
		XSSFRow row = null;
		int dataRowSize = dataElems.size();
		int maxColNum = getMaxColNum(curSheet);
		if (rowCond && colCond) {// 满足行和列下标条件

			for (int k = 0; k < frmRowIdx; k++) {
				row = curSheet.getRow(k);
				if (row == null) {
					row = curSheet.createRow(k);
				}
				int colNum = Math.max(maxColNum, toColIdx - frmColIdx + 1);
				for (int m = 0; m < colNum; m++) {
					cell = row.getCell(m);
					if (cell == null) {
						cell = row.createCell(m);
					}
				}
			}

			// 写行数据
			int wrtEndRowIdx = Math.min(dataRowSize + frmRowIdx - 1, toRowIdx);
			int dataRowIdx = 0;
			for (int i = frmRowIdx; i <= wrtEndRowIdx; i++) {
				row = curSheet.getRow(i);
				if (row == null) {
					row = curSheet.createRow(i);
				}
				int dataColNum = dataElems.get(dataRowIdx).size();
				int wrtEndColIdx = Math.min(dataColNum + frmColIdx - 1,
						toColIdx);
				int dataColIdx = 0;
				for (int j = frmColIdx; j <= wrtEndColIdx; j++) {
					cell = row.getCell(j);
					if (cell == null) {
						cell = row.createCell(j);
					}
					String curVal = dataElems.get(dataRowIdx).get(dataColIdx);
					cell.setCellValue(curVal);
					dataColIdx++;
				}
				dataRowIdx++;
			}
			writeFile();
		}
	}

	public void writeTbl(XSSFSheet curSheet, ArrayList<String> titleRow,
			ArrayList<ArrayList<String>> dataRows) throws IOException {
		Cell cell = null;
		XSSFRow row = null;
		int colSize = titleRow.size();
		if (colSize > 0) {
			// 创建标题行
			row = curSheet.createRow(0);
			for (int j = 0; j < colSize; j++) {
				// 创建新列
				cell = row.createCell(j);
				String curVal = titleRow.get(j);
				// writeCell(cell, curVal);
				cell.setCellValue(curVal);
			}
		}
		int dataRowSize = dataRows.size();
		if (dataRowSize > 0) {
			// 写行数据
			for (int i = 0; i < dataRowSize; i++) {
				row = curSheet.createRow(i + 1);
				for (int j = 0; j < colSize; j++) {
					cell = row.createCell(j);
					String curVal = dataRows.get(i).get(j);
					// writeCell(cell, curVal);
					cell.setCellValue(curVal);
				}
			}
		}
		if (colSize > 0 || dataRowSize > 0) {
			writeFile();
		}
	}

}
