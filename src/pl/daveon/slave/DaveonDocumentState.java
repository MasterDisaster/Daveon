package pl.daveon.slave;

/**
 * @author bkaczmarek
 *
 *
 *
 */
public class DaveonDocumentState
{
	private STATE _docState = STATE.UNDEF;
	private String _md5 	= "";
	private String _strippedPath = "";
//	private int 	docID	=	-1;
	private String orgVal	= "";

	public DaveonDocumentState(String doc){
		this.orgVal = doc;
	}

	public DaveonDocumentState(String md5, String path, String orgpath, int docID)
	{
		this(orgpath);
		if(docID>0)
			this._docState=STATE.STORED;
		else
			this._docState=STATE.NOTSTORED;

		this._md5 = md5;
		this._strippedPath = path;
	}

	public boolean equals(Object obj)
	{

		return (obj instanceof DaveonDocumentState) && (this.orgVal == ((DaveonDocumentState)obj).getOrgVal());
//		return (obj instanceof LucyDocumentState) && (this._md5 == ((LucyDocumentState)obj).getMD5())
//		&& (_strippedPath == ((LucyDocumentState)obj).getStrippedPath())
//		&& (docID ==  ((LucyDocumentState)obj).getDocID()) && (_docState==((LucyDocumentState)obj).getState());
	}

	public STATE getState() {
		return _docState;
	}
	public void setState(STATE state) {
		_docState = state;
	}
	public String getMD5() {
		return _md5;
	}
	public void setMD5(String _md5) {
		this._md5 = _md5;
	}
	public String getStrippedPath() {
		return _strippedPath;
	}
	public void setStrippedPath(String strippedPath) {
		this._strippedPath = strippedPath;
	}
//	public int getDocID() {
//		return docID;
//	}
//	public void setDocID(int docID) {
//		this.docID = docID;
//	}

	public String getOrgVal() {
		return orgVal;
	}
}
