package pl.daveon.slave;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;

//import org.apache.lucene.index.IndexWriter;
/**
 * @author bkaczmarek
 *
 *
 *
 */
public interface  IDaveonIndexation {

	public void addDocument(Document _doc, IStearingThred callingThread)throws CorruptIndexException, IOException ;
	public void removeDocument(Term _docTerm)throws CorruptIndexException, IOException ;
//	public void updateDocument(Term _docTerm, Document _doc)throws CorruptIndexException, IOException ;
	public void forceFlush();
	/**
	 * @return zwraca warotsc okreslajaca lokalizacje na FS dokumentow do indexacji w postaci czystej
	 * (patrz @getRootLocationURLForm)
	 */
	public String getRootLocation();
	/**
	 * Jesli w wartosci zwracnej przez @getRootLocation sa spacje to wtedy takie znaki
	 * zamieniane sa na %20 i tym samym efektywna dugosc zmiennej jest inna niz orginalna
	 *
	 * @return zwraca wartosc z @getRootLocation zamieniona na adres URL
	 */
	public String getRootLocationURLForm();
	public WORKMODE getWorkMode();
	public String getIndexLocation();
//	public String[] getLocalPathToURL(String localFile);
	public ArrayList<DaveonDocumentState> getPreIndexedFilesMeta();

}
