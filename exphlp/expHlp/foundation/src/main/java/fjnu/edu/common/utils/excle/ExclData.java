package fjnu.edu.common.utils.excle;

import java.util.ArrayList;

public class ExclData {
	//标题行
	private ArrayList<String> titleRow = new ArrayList<String>();
	//数据行
	private ArrayList<ArrayList<String>> dataRows = new ArrayList<ArrayList<String>>();

	public ExclData(ArrayList<String> titleRow,
			ArrayList<ArrayList<String>> dataRows) {
		this.titleRow = titleRow;
		this.dataRows = dataRows;
	}

	public ArrayList<String> getTitleRow() {
		return titleRow;
	}

	public void setTitleRow(ArrayList<String> titleRow) {
		this.titleRow = titleRow;
	}

	public ArrayList<ArrayList<String>> getDataRows() {
		return dataRows;
	}

	public void setDataRows(ArrayList<ArrayList<String>> dataRows) {
		this.dataRows = dataRows;
	}

}
