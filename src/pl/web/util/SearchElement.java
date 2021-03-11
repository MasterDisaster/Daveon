package pl.web.util;

public class SearchElement
{
	private String	urlHref		= "";

	private String	url			= "";

	private String	desc		= "";

	private String	modified	= "";

	private String	docTitle	= "";

	private String  fileType	= "";

	private String  fileName	= "";

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getDesc()
	{
		return desc;
	}

	public void setDesc(String desc)
	{
		this.desc = desc;
	}

	public String getUrlHref()
	{
		return urlHref;
	}

	public void setUrlHref(String urlHref)
	{
		this.urlHref = urlHref;
	}

	public String getModified()
	{
		return modified;
	}

	public void setModified(String modified)
	{
		this.modified = modified;
	}

	public String getDocTitle() {
		return docTitle;
	}

	public void setDocTitle(String docTitle) {
		this.docTitle = docTitle;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
