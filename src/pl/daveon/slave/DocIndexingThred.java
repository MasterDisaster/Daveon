package pl.daveon.slave;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.poi.POITextExtractor;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xwpf.model.XWPFCommentsDecorator;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.tika.detect.CompositeDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.detect.TextDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.POIFSContainerDetector;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import pl.daveon.core.DaveonIndexer;

/**
 * @author bkaczmarek
 *
 *
 *
 */
public class DocIndexingThred extends Thread implements IStearingThred {

	private static Logger		logger				= Logger.getLogger(DocIndexingThred.class);
	private final char[] 		_SEPARATOR			= new char[]{' '};
//	public static final String	_urlPrefix			= new File("").toURI().getScheme()+":/";
//	public static final int		_urlPrefixLn		= _urlPrefix.length();
	private ArrayList<DaveonDocumentState> _allds		= null;
	private int					FI_UNWANTED_PATH_ELEMENT_SIZE	= 0;
	private static final long	FL_CURRTIME					= new Date().getTime();
	private static final DateFormat _df 					= new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	public static long strToDateToLong(String dateS)
	{
		long res = FL_CURRTIME;
		try {
			String s = dateS.replaceAll("([a-zA-Z])", " ").trim();
			res = _df.parse(s).getTime();
		} catch (ParseException e) {}
		catch(Exception e) {}
		return res;
	}


	private class DocMeta
	{
		private String _docBody		= "";
//		private String _docModified	= "";
		private String _docMeta		= "";
		private String _docCT		= "";
		private final Pattern newLinesPat	=	Pattern.compile("(\r\n)+|(\r+)|(\n+)");
		private final String stripping	=	"((\r\n)?\t+(\r\n))+|(\r?\n){2,}|([\r\n]+)";
		
		public DocMeta()
		{
			_docMeta = "";
		}

		public DocMeta(StringBuffer body)
		{
			setDocBody(body);
		}
		
		public DocMeta(String body)
		{
			this(new StringBuffer(body));
		}

		public String getDocBody() {
			return _docBody;
		}
		
		/**
		 * zwraca zamieniony na string wektor pozycji wystepowan nowych lini
		 */
		public String transpodedStats()
		{
			PatternTokenizer pa = new PatternTokenizer(new StringReader(getDocBody()), newLinesPat, 0);
			StringBuilder sb = new StringBuilder("");
			try
			{
				OffsetAttribute ta = (OffsetAttribute)pa.addAttribute(OffsetAttribute.class);
				try {
					pa.reset();
				} catch (IOException e) { 
					logger.error("tokenizer reset exception:"+e.getMessage());
					e.printStackTrace(); 
				}
				int i = 0;
				while(pa.incrementToken())
					sb.append(++i+":"+ta.startOffset()+";");
			}
			finally
			{
				
				try {
					if (pa!=null)
						pa.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return sb.toString();
		}
		
		public void clearDocBody()
		{
			_docBody = null;
		}
		
		public void setDocBody(StringBuffer body) {
			try
			{
				_docBody = body.toString().replaceAll("[<]+", "&lt;");
				_docBody = _docBody.replaceAll("[>]+", "&gt;");
				_docBody = _docBody.replaceAll(stripping, "\n");
				_docBody = _docBody.replaceAll("[\t]+", "").trim();
			}
			catch(OutOfMemoryError oom)
			{
				_docBody = null;
				logger.error("OutOfMemmoryError on file's size:"+body.length());
			}
		}

//		public String getDocModified() {
//			return _docModified;
//		}
//
//		public long getDocModifiedL()
//		{
//            return strToDateToLong(_docModified);
//		}
//
//		public void setDocModified(String docModified) {
//			_docModified = docModified;
//		}

		public String getDocMeta() {
			return _docMeta;
		}

		public void addDocMeta(String metaKey, String metaValItem) {
			_docMeta += metaKey+"\\t"+metaValItem+"|";
		}

		public String getDocCT() {
			return _docCT;
		}

		public void setDocCT(String _docct) {
			_docCT = _docct;
		}

		public boolean isContextEmpty()
		{
			return _docBody!=null && _docBody.length()<=2;
		}
	}

	private final BlockingQueue<String> _queue;
	private String id 			= "NOT-SET";
	private boolean runningFree = true;
	private int _docsProcessed 	= 0;
	public static boolean _suspended = false;
	private Document _doc		= new Document();
//	private AtomicInteger _ai	= new AtomicInteger(0);
//	private final static int	IS_COMPLETE = 1;
	private boolean is_started	= false;

	private CompositeDetector detector 	= null;
	private AutoDetectParser parser	= null;
	private ParseContext 	_parseContext = null;
	private IDaveonIndexation _ili= null;

//	private final ReentrantLock lock = new ReentrantLock();

	public synchronized void stopFreeRun()
	{
			runningFree = false;
	}
	
	public synchronized void suspendWork()
	{
			_suspended = true;
	}
	
	public synchronized void UnSuspendWork()
	{
		DocIndexingThred._suspended = false;
	}
	
	public DocIndexingThred(BlockingQueue<String> queue, String _id, IDaveonIndexation ili) throws InterruptedException {
		super();
		if(queue == null || ili== null) throw new InterruptedException("BlockingQueue/ILucyIndexation is not set!");
		this._ili = ili;
		this._queue = queue;
		this.id = _id+".shell("+super.toString()+")";
		if(ili.getWorkMode()==WORKMODE.CROWLING_MODE)
			this._allds = ili.getPreIndexedFilesMeta();

		this.FI_UNWANTED_PATH_ELEMENT_SIZE = getILI().getRootLocationURLForm().length();

		this.detector= setupDefaultDectector();
		this.parser = new AutoDetectParser();
		this.parser.setDetector(getDetector());
		this._parseContext = new ParseContext();
//        System.out.println("My locale is:"+Locale.getDefault());
//		this._parseContext.set(Locale.class, Locale.getDefault());
//		this.
	}

	public DaveonDocumentState getDocumentMeta(String doc)
	{
		DaveonDocumentState lds = new DaveonDocumentState(doc);
		int index = this._allds.indexOf(lds);
		if (index<0)
			return null;
		else
			return this._allds.get(index);


	}

	public String getMimeType(final File p_document, Metadata metadata)
	{
	    try
	    {
	        TikaInputStream inputStream = TikaInputStream.get(p_document, metadata);
	        return getDetector().detect(inputStream, metadata).toString();
	    }
	    catch (Throwable t)
	    {
	        logger.error("Error while determining mime-type of " + p_document);
	    }
	    return null;
	}

	private CompositeDetector getDetector()
	{
		return this.detector;
	}

	private final CompositeDetector setupDefaultDectector()
	{
	        List<Detector> detectors = new ArrayList<Detector>();

	        detectors.add(new TextDetector());
	        // Microsoft stuff
	        detectors.add(new POIFSContainerDetector());
	        // mime magic detection as fallback
//	        detectors.add(new ZipContainerDetector());
	        detectors.add(MimeTypes.getDefaultMimeTypes());

	    return new CompositeDetector(detectors);
	}

	@Override
	public void run() {
//		logger.info("Zglaszam sie: " +id);
		is_started = true;
		try {
			
//				String s = null;
				while (runningFree) {
//					if(!DocIndexingThred._suspended)
//					{
					String s = _queue.take();//take jest BLOKUJACE
					
						/*jesli watkow jest wiecej niz pracy to watki ktore nie otrzymaly pracy
						 * poprzez @take beda wisialy w tym miejscu i nigdy nie dojdzie
						 * do wywoalnia ponizszej lini: doWork()*/
						doWork(s);
//					}
					if(!runningFree)
							throw new InterruptedException("Clean finish.");
				}

				/*
				 *work has ended - incrementing ThreadPresenceCounter
				 * it informs by an increment, that thread has finished its work - IndexWriter is not used
				*/
		    }
		catch ( InterruptedException ie )
		{
			ThreadPresenceCounter.THREAD_PRESENCE_COUNTER.incrementAndGet();
			logger.debug("Thread: "+id+" interrupted with message: "+ie.getMessage());
		}
		logger.info("Thread: "+id+"->"+_docsProcessed+" exiting");
	}

	/**
	 * @return true jesli wystartowany, false w innym przypadku
	 */
	public boolean isStarted()
	{
		return is_started;
	}


	@Override
	public String toString()
	{
		return id;
	}

	public void doWork(String s)
	{
		switch(getILI().getWorkMode()){
		case CROWLING_MODE:
			{
				try {
					processDocFileUpdate(s);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		case FULLINDEXING_MODE:
			{
				processDocFile(s);
				break;
			}
		}
		_docsProcessed++;
	}

	public String[] getLocalPathToURL(File localFile)
	{
		String localPath 	= "";
		try {
			localPath = localFile.toURI().toURL().toExternalForm().toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		String reposPath = localPath.subSequence(this.FI_UNWANTED_PATH_ELEMENT_SIZE-1, localPath.length()).toString();
		reposPath = reposPath.replaceAll("\\+", "%2b");

		return new String[]{reposPath, getMD5(reposPath)};
	}

	public static String getMD5(String val)
	{
		return DigestUtils.md5Hex(val);
	}


	public void processDocFileUpdate(String _toIndexFile) throws IOException, NullPointerException
	{
		File mfile = new File(_toIndexFile);
		DaveonDocumentState lds = getDocumentMeta(_toIndexFile);
		if (mfile.exists())
		{
			if(lds.getState()==STATE.STORED)
				getILI().removeDocument(new Term(DaveonIndexer.CCSUM, lds.getMD5()));

			processDocFile(_toIndexFile);
			/*Jesli plik istnieje to trzeba:
			 * 1. sprawdzic czy jest juz w indexie
			 * 1.a - jesli jest to usunac i dodac jego nowa wartosc
			 * 1.a - jesli nie ma to po porstu dodac*/

		}else
			{
			if(lds.getState()==STATE.STORED)
			{
				getILI().removeDocument(new Term(DaveonIndexer.CCSUM, lds.getMD5()));
				logger.debug("Document ("+lds.getMD5()+") removed.");
			}

			/*jesli nie ma pliku to trzeba sprawdzic czy jest w indexie:
			 * - jesli jest w indexie usunac*/
			}
	}



	enum TYPEDFILE {
		DOCX("DOCX"), DOC("DOC"),XLSX("XLSX"),XLS("XLS"),
		PDF("PDF"), PPT("PPT"), PPTX("PPTX"),FILE("FILE"),
		JPG("JPG"), JPEG("JPED"), ZIP("ZIP"), TAR("TAR"), RAR("RAR"), JAR("JAR"), CLASS("CLASS"),
		GZ("GZ"), GZIP("GZIP"), GIF("GIF"), ICO("ICO"), PNG("PNG"), EXE("EXE"), WAR("WAR"),EAR("EAR"),
		TIF("TIF"),TIFF("TIFF"), TXT("TXT"), SQL("SQL"), LOG("LOG"), BAT("BAT"),
		DBF("DBF"),LIB("LIB");

		String _name = "";

		TYPEDFILE(String otype){
//			for(int ord =0; ord<otype.length(); ord++)
//			{
//				value += (long)(bts[ord] << ord*8);
//			}
//			this._type = value;
			this._name = otype;
		}

	}

	public void processDocFile(String _toIndexFile) //throws Exception
	{
		DocMeta textualData = null;
		String fnm = "";
		String ftype = "";
//		String getName  = "";

		File mfile = new File(_toIndexFile);
		String fileName = mfile.getName();
		Field contentType = null;

			if (!fileName.startsWith("~$"))
			{
				String[] splits 	= fileName.split("\\.");
				try
				{
					ftype 		= (splits!=null && splits.length>1) ? splits[splits.length-1].toUpperCase():"FILE";
					fnm 		= (splits!=null) ? fileName.substring(0, fileName.length()-(ftype.length()+1)):fileName;
				}
				catch (StringIndexOutOfBoundsException sioobe)
				{
					fnm= fileName;
					ftype = "FILE";
				}
				splits = null;

				TYPEDFILE unknownTypedF = null;
				try
				{
					unknownTypedF = TYPEDFILE.valueOf(ftype);
				}
				catch(IllegalArgumentException  e)
				{
					unknownTypedF = TYPEDFILE.FILE;
				}
				try
				{
				switch(unknownTypedF){
					case DOCX: {
						textualData = retrieveXMSWordText(mfile);
						break;
						}
					case DOC:{
						textualData = retrieveMSWordText(mfile);
						break;
					}
					case XLSX:{
						textualData = retrieveXMSExcelText(mfile);
						break;
					}
					case XLS:{
						textualData = retrieveMSExcelText(mfile);
						break;
					}
					case PDF:{
						textualData = retrievePDFText(mfile);
						break;
					}
					case JPG: 	break;
					case PNG: 	break;
					case JPEG: 	break;
					case DBF: 	break;
					case TAR: 	break;
					case LIB: 	break;
					case ZIP: 	break;
					case RAR: 	break;
					case JAR: 	break;
					case CLASS: break;
					case GZ: 	break;
					case GZIP: 	break;
					case GIF: 	break;
					case ICO: 	break;
					case EXE: 	break;
					case WAR: 	break;
					case EAR: 	break;
					case TIF: 	break;
					case TIFF: 	break;
					case FILE:	;/*{logger.debug("processing by default/autodetection");}*/
					default:{textualData = allOthersIndexing(mfile, fileName); break;}
					}
				}
				catch(IOException  e)
				{
					textualData = null;
				}

			}

			if (textualData != null && !textualData.isContextEmpty() )
			{

				this._doc.removeField(DaveonIndexer.CONTEXT_KEY);
				this._doc.removeField(DaveonIndexer.CONTEXT_KEY_STATS);
				this._doc.removeField(DaveonIndexer.LINK_KEY);
				this._doc.removeField(DaveonIndexer.FT_KEY);
				this._doc.removeField(DaveonIndexer.DOC_NAME);
				this._doc.removeField(DaveonIndexer.CONTENTTYPE);
				this._doc.removeField(DaveonIndexer.CCSUM);

				logger.debug("indexing: "+fileName);

				VecTextField context 		= new VecTextField(DaveonIndexer.CONTEXT_KEY, textualData.getDocBody(), Field.Store.YES);

				MetaField	contextStats	= new MetaField(DaveonIndexer.CONTEXT_KEY_STATS, textualData.transpodedStats());
				
				StringField ftypeField		= new StringField(DaveonIndexer.FT_KEY, ftype, Store.YES);
				VecTextField fnmField		= new VecTextField(DaveonIndexer.DOC_NAME, fnm, Field.Store.YES);

	        	contentType = new StringField(DaveonIndexer.CONTENTTYPE, textualData.getDocCT(), Field.Store.YES);

				String[] _fileCheckData = getLocalPathToURL(mfile);
				StringField checkSum	= new StringField(DaveonIndexer.CCSUM, _fileCheckData[1], Field.Store.YES);

				VecTextField link = new VecTextField(DaveonIndexer.LINK_KEY, _fileCheckData[0], Field.Store.YES);

				this._doc.add(context);
				this._doc.add(contextStats);
				this._doc.add(link);
				this._doc.add(ftypeField); // extension
				this._doc.add(fnmField);//file name - without extension
				this._doc.add(checkSum);
				if(contentType!=null)
					this._doc.add(contentType);

				try {
					getILI().addDocument(this._doc, this);
					
					textualData.clearDocBody();
					textualData = null;
					
				} catch (CorruptIndexException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else
				logger.debug("not indexing:"+ fileName);
	}
	
	private DocMeta allOthersIndexing(File mfile, String fileName) throws IOException
	{
		DocMeta textualData = null;
		Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, fileName);
        String mim = getMimeType(mfile, metadata);
        StringWriter sw = null;
        if(mim!=null)
        {
			InputStream is = null;
			metadata.set(Metadata.CONTENT_TYPE, mim);
	        try {
	            is = new BufferedInputStream(new FileInputStream(mfile));
	            sw = new StringWriter();
	            textualData = new DocMeta();

	            BodyContentHandler handler = new BodyContentHandler(sw);
	            try
	            {
	            	parser.parse(is, handler, metadata, this._parseContext);
	            }
	            catch (StringIndexOutOfBoundsException si)
	            {
	            	logger.error("parser exception"+si.getMessage());
	            }
	            catch (OutOfMemoryError oome)
	            {
	            	synchronized(DocIndexingThred.class){
	            	getILI().forceFlush();
	            	logger.error("OutOfMemoryError: "+oome.getMessage()+": file:"+mfile.getPath());
	            	}
	            }
	            textualData.setDocCT(mim);
//	            textualData.setDocModified(metadata.get(TikaCoreProperties.MODIFIED));

	            if(sw!=null){
		        	textualData.setDocBody(sw.getBuffer());
				}
	        }
	        catch (IOException e) 	{textualData=null;}
	        catch (TikaException e) {textualData=null;}
	        catch (SAXException e) 	{textualData=null;}
	        finally {
	        	if (sw!=null) 
	        			sw.close();
	        	
	            if (is != null) {
	                try {
	                    is.close();
	                } catch(IOException e) {
	                	textualData=null;
	                }
	            }
	        }
        }
        return textualData;
	}
	
	
	/**
	 * @return zwraca interfej ILucyIndexation w skrocie ILI
	 */
	private IDaveonIndexation getILI() {
		return this._ili;
	}

	public DocMeta retrieveXMSWordText(File _file)
	{
		DocMeta result = null;
		try
		{
			result = retrieveXMSWordText(_file.getPath());
		}
		catch (Exception e)
		{
			logger.error(":<#EXCPT#>:" + _file.toString() + ":<#EXCPT#>:" + e.getLocalizedMessage());
		}
		catch (Error error)
		{
			logger.error(":<#ERROR#>:" + _file.toString() + ":<#ERROR#>:" + StripStackTrace(error,10));
		}
		return result;
	}

	public DocMeta retrieveMSWordText(File _file)
	{
		DocMeta result = null;
		try
		{
			result = retrieveOLEDocumentsText(_file.getPath());
		}
		catch (Exception e)
		{
			logger.error(":<#EXCPT#>:" + _file.toString() + ":<#EXCPT#>:" + e.getLocalizedMessage());
		}
		catch (Error error)
		{
			logger.error(":<#ERROR#>:" + _file.toString() + ":<#ERROR#>:" + StripStackTrace(error,10));
		}
		return result;
	}

	private DocMeta retrieveXMSWordText(String _fileName) throws IOException, Error
	{
//		String		 title	  = "";
		StringBuilder textBody = new StringBuilder("");

//		boolean fetchHyperlinks = false;
		XWPFDocument document = null;
		try
		{
			document =	new XWPFDocument(POIXMLDocument.openPackage(_fileName));
		}
		catch(InvalidOperationException ioe)
		{
			document = null;
			logger.error(_fileName +": "+ ioe.getLocalizedMessage());
		}
		if(document == null) return null;
		
//	    XWPFHeaderFooterPolicy hfPolicy = document.getHeaderFooterPolicy();

//	    extractHeaders(textBody, hfPolicy);

	    Iterator<XWPFParagraph> i = document.getParagraphsIterator();
	    while(i.hasNext()) {
	        XWPFParagraph paragraph = i.next();

//	        try {
//	                CTSectPr ctSectPr = null;
//	                if (paragraph.getCTP().getPPr()!=null) {
//	                        ctSectPr = paragraph.getCTP().getPPr().getSectPr();
//	                }

//	                XWPFHeaderFooterPolicy headerFooterPolicy = null;

//	                if (ctSectPr!=null) {
//	                        headerFooterPolicy = new XWPFHeaderFooterPolicy(document, ctSectPr);
//	                        extractHeaders(textBody, headerFooterPolicy);
//	                }

	                // Do the paragraph text
	                for(XWPFRun run : paragraph.getRuns()) {
	                	textBody.append(run.toString());
//	                   if(run instanceof XWPFHyperlinkRun && fetchHyperlinks) {
//	                      XWPFHyperlink link = ((XWPFHyperlinkRun)run).getHyperlink(document);
//	                      if(link != null)
//	                    	  textBody.append(" &lt;" + link.getURL() + "&gt;");
//	                   }
	                }

	                // Add comments
	                XWPFCommentsDecorator decorator = new XWPFCommentsDecorator(paragraph, null);
	                textBody.append(decorator.getCommentText()).append('\n');
	                // Do endnotes and footnotes
	                String footnameText = paragraph.getFootnoteText();
	           if(footnameText != null && footnameText.length() > 0) {
	        	   textBody.append(footnameText + "\n");
	           }

//	                if (ctSectPr!=null && headerFooterPolicy.getFirstPageHeader() != null) {
//	                	title=headerFooterPolicy.getFirstPageHeader().getText();
//	                }
//	        } catch (IOException e) {
//	                throw new POIXMLException(e);
//	        } catch (XmlException e) {
//	                throw new POIXMLException(e);
//	        }
	    }
	    // Then our table based text
	    Iterator<XWPFTable> j = document.getTablesIterator();
	    while(j.hasNext()) {
	    	textBody.append(j.next().getText()).append('\n');
	    }

		return new DocMeta(textBody.toString());
	}


	public DocMeta retrieveOLEDocumentsText(String _fileName) throws Exception, Error
	{
		StringBuilder text = new StringBuilder();

	         POITextExtractor extractor = ExtractorFactory.createExtractor(new File(_fileName));
	         POITextExtractor metadataExtractor = extractor.getMetadataTextExtractor();
	         text.append(extractor.getText());
	         text.append(metadataExtractor.getText());

		return text.length()>0 ? new DocMeta(text.toString()):null;
	}

//
//    private String extractHeaders(StringBuffer text, XWPFHeaderFooterPolicy hfPolicy) {
//    	String PossibleTitle = "";
//        if(hfPolicy.getFirstPageHeader() != null) {
//        	PossibleTitle = hfPolicy.getFirstPageHeader().getText();
//        }
//        if (PossibleTitle.isEmpty())
//        if(hfPolicy.getEvenPageHeader() != null) {
//        	PossibleTitle = hfPolicy.getEvenPageHeader().getText();
//        }
//        if (PossibleTitle.isEmpty())
//        if(hfPolicy.getDefaultHeader() != null) {
//        	PossibleTitle = hfPolicy.getDefaultHeader().getText();
//        }
//
//        return PossibleTitle.isEmpty() ? null:PossibleTitle;
//
//    }



	public DocMeta retrievePDFText(File _file)
	{
		DocMeta result = null;
		try
		{

			result = retrievePDFText(_file.getPath());
		}
		catch (Exception e)
		{
			logger.error(":<#EXCPT#>:" + _file.toString() + ":<#EXCPT#>:" + e.getLocalizedMessage());
		}
		catch (Error error)
		{
			logger.error(":<#ERROR#>:" + _file.toString() + ":<#ERROR#>:" + StripStackTrace(error,10));
		}

		return result;
	}

	public DocMeta retrievePDFText(String _fileName) throws Exception, Error
	{
		String textBody = "";
		PDDocument pddoc = null;
		try
		{

			PDFTextStripper stripper = new PDFTextStripper();
			pddoc = PDDocument.load(_fileName);
			textBody = stripper.getText(pddoc);
		}
		finally
		{
			pddoc.close();
		}

		return new DocMeta(textBody);
	}

	public DocMeta retrieveXMSExcelText(File _file)
	{
		DocMeta result = null;
		try
		{
			result = retrieveXMSExcelText(_file.getPath());
//			result = costam(_file.getPath());// retrieveXMSExcelText(_file.getPath());
		}
		catch (Exception e)
		{
			logger.error(":<#EXCPT#>:" + _file.toString() + ":<#EXCPT#>:" + e.getStackTrace().toString());
		}
		catch (Error error)
		{
			logger.error(":<#ERROR#>:" + _file.toString() + ":<#ERROR#>:" + StripStackTrace(error,10));
		}
		return result;
	}

	public DocMeta retrieveMSExcelText(File _file)
	{
		DocMeta result = null;
		try
		{
			result = retrieveOLEDocumentsText(_file.getPath());
		}
		catch (Exception e)
		{

			logger.error(":<#EXCPT#>:" + _file.toString() + ":<#EXCPT#>:" + e.getStackTrace().toString());
		}
		catch (Error error)
		{
			logger.error(":<#ERROR#>:" + _file.toString() + ":<#ERROR#>:" + StripStackTrace(error,10));
		}
		return result;
	}

	public final String StripStackTrace(Error _e, int limit)
	{
		StringBuffer res = new StringBuffer(_e.getMessage()+System.getProperty("line.separator"));
		if(_e!= null)
		{
			StackTraceElement[] ste =  _e.getStackTrace();
			if(ste!=null && ste.length>0)
			{
				int i = 0;
				while((i < limit) && (i < ste.length))
					res.append(ste[i++].toString()+System.getProperty("line.separator"));
			}
		}
		return res.toString();
	}


	   protected class SheetTextExtractor implements SheetContentsHandler {
		      private final StringBuilder output;
		      private boolean firstCellOfRow = true;

		      protected SheetTextExtractor(StringBuilder output) {
		         this.output = output;
		      }

		      public void startRow(int rowNum) {
		         firstCellOfRow = true;
		      }

		      public void endRow() {
		         output.append('\n');
		      }

		      public void cell(String cellRef, String formattedValue) {
		         if(firstCellOfRow) {
		            firstCellOfRow = false;
		         } else {
		            output.append('\t');
		         }
		         output.append(formattedValue);
		      }

		      public void headerFooter(String text, boolean isHeader, String tagName) {
		         // We don't include headers in the output yet, so ignore
		      }
		   }

	   /**
	    * Processes the given sheet
	    */
	   @Deprecated
	   public void processSheet(SheetContentsHandler sheetContentsExtractor, StylesTable styles,
			   ReadOnlySharedStringsTable strings, InputStream sheetInputStream, boolean donotuse) throws IOException, SAXException {

		   DataFormatter formatter = new DataFormatter();
	       boolean formulasNotResults = false;
	       InputSource sheetSource = new InputSource(sheetInputStream);
	       SAXParserFactory saxFactory = SAXParserFactory.newInstance();
	       try
	       {
	          SAXParser saxParser = saxFactory.newSAXParser();
	          XMLReader sheetParser = saxParser.getXMLReader();
	          ContentHandler handler = new XSSFSheetXMLHandler( styles, strings, sheetContentsExtractor, formatter, formulasNotResults);
	          sheetParser.setContentHandler(handler);
	          sheetParser.parse(sheetSource);
	       }
	       catch(ParserConfigurationException e)
	       {
	          throw new RuntimeException("SAX parser appears to be broken - " + e.getMessage());
	       }
	   }
	   
	   
	 public DocMeta retrieveXMSExcelText(String _fileName) throws Exception, Error
	 {
		 	StringBuilder textBody = new StringBuilder();
			FileInputStream fis = null;
			try
			{

				fis = new FileInputStream(_fileName);
				OPCPackage container = OPCPackage.open(fis);
				XSSFReader xssfReader = new XSSFReader(container);
				
				SharedStringsTable sst = xssfReader.getSharedStringsTable();
				XMLReader parser = fetchSheetParser(sst, textBody);
		        
				Iterator<InputStream> sheets = xssfReader.getSheetsData();
				while(sheets.hasNext()) {
					textBody.append("\r\n\r\n");
					InputStream sheet = sheets.next();
					InputSource sheetSource = new InputSource(sheet);
					parser.parse(sheetSource);
					sheet.close();
				}
			}
			finally
			{
				fis.close();
			}
			return new DocMeta(textBody.toString());
	 }
	 
	 public XMLReader fetchSheetParser(SharedStringsTable sst, StringBuilder sb) throws SAXException {
			XMLReader parser =
				XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
			ContentHandler handler = new SheetHandler(sst, sb);
			parser.setContentHandler(handler);
			return parser;
		}
	 
	 
	 private static class SheetHandler extends DefaultHandler {
			private SharedStringsTable sst;
			private String lastContents;
			private boolean nextIsString;
			private StringBuilder buffer; 
			private SheetHandler(SharedStringsTable sst, StringBuilder sb) {
				this.sst = sst;
				this.buffer = sb;
			}
			
			public void startElement(String uri, String localName, String name,
					Attributes attributes) throws SAXException {
				// c => cell
				if(name.equals("c")) {
					// Print the cell reference
					// Figure out if the value is an index in the SST
					String cellType = attributes.getValue("t");
					if(cellType != null && cellType.equals("s")) {
						nextIsString = true;
					} else {
						nextIsString = false;
					}
				}
				// Clear contents cache
				lastContents = "";
			}
			
			public void endElement(String uri, String localName, String name)
					throws SAXException {
				// Process the last contents as required.
				// Do now, as characters() may be called more than once
				if(nextIsString) {
					int idx = Integer.parseInt(lastContents);
					lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
	            nextIsString = false;
				}

				// v => contents of a cell
				// Output after we've seen the string contents
				if(name.equals("v")) {
//					System.out.println(lastContents);
					buffer.append(lastContents+"\r\n");
				}
			}

			public void characters(char[] ch, int start, int length)
					throws SAXException {
				lastContents += new String(ch, start, length);
			}
		}

	 

	/**
	 * @param _fileName
	 * @return
	 * @throws Exception
	 * @throws Error
	 */
	 @Deprecated
	public DocMeta retrieveXMSExcelText(String _fileName, boolean donotuse) throws Exception, Error
	{
		StringBuilder textBody = new StringBuilder();
		FileInputStream fis = null;
		boolean includeSheetNames = true;
		try
		{

			fis = new FileInputStream(_fileName);
			OPCPackage container = OPCPackage.open(fis);
			XSSFReader xssfReader = new XSSFReader(container);
			
			ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(container);
	        
	        StylesTable styles = xssfReader.getStylesTable();
	        XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();

	        SheetTextExtractor sheetExtractor = new SheetTextExtractor(textBody);

	          while (iter.hasNext()) {
	              InputStream stream = iter.next();
	              if(includeSheetNames) {
	            	  textBody.append(iter.getSheetName());
	            	  textBody.append('\n');
	              }
	              processSheet(sheetExtractor, styles, strings, stream, donotuse);
	              stream.close();
	          }
		}
		finally
		{
			fis.close();
		}
		return new DocMeta(textBody.toString());
	}


	@Deprecated
	public String getTokens(Analyzer _analyzer, String _text) throws IOException
	{
		if (_text != null && !_text.isEmpty())
		{
			StringBuffer sb = new StringBuffer();

			TokenStream stream = null;
			try
			{
				stream = _analyzer.tokenStream(null, new StringReader(_text));
				CharTermAttribute ta =  (CharTermAttribute) stream.addAttribute(CharTermAttribute.class);
				String w = "";
				while(stream.incrementToken())
				{
					if (ta.length()<=0) continue;
					w= ta.toString();
					if(w==null || w.isEmpty() ) continue;
					sb.append(w);
					sb.append(this._SEPARATOR);
				}
			}
			finally
			{
				stream.close();
			}
			return sb.toString();
		}
		else
			return null;
	}

}

