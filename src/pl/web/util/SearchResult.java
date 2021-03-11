package pl.web.util;

import java.util.ArrayList;

public class SearchResult<E>
{
	private ArrayList<E> elements = new ArrayList<E>();
	private String time = "";
	private String count = "0";
	private String query = "";
	private String sortType = "";
	private int pager = 1;
	private String indexTime = "";

	public ArrayList<E> getElements()
	{
		return elements;
	}

	public void setElements(ArrayList<E> elements)
	{
		this.elements = elements;
	}

	public String getTime()
	{
		return time;
	}

	public void setTime(String time)
	{
		this.time = time;
	}

	public String getCount()
	{
		return count;
	}

	public void setCount(String count)
	{
		this.count = count;
	}

	public String getQuery()
	{
		return query;
	}

	public void setQuery(String query)
	{
		this.query = query;
	}

	public int getPager()
	{
		return pager;
	}

	public void setPager(int pager)
	{
		this.pager = pager;
	}

	public String getSortType()
	{
		return sortType;
	}

	public void setSortType(String sortType)
	{
		this.sortType = sortType;
	}

	public String getIndexInfoAdd() {
		return indexTime;
	}

	public void setIndexInfoAdd(String indexTime) {
		this.indexTime = indexTime;
	}

}
