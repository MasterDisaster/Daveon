package pl.daveon.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import pl.daveon.core.tool.DaveonConcurrentMergeScheduler;
import pl.daveon.slave.CyclesCrawler;
import pl.daveon.slave.DaveonDocumentState;
import pl.daveon.slave.DocIndexingThred;
import pl.daveon.slave.IDaveonIndexation;
import pl.daveon.slave.IStearingThred;
import pl.daveon.slave.STATE;
import pl.daveon.slave.WORKMODE;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;


/**
 * @author bkaczmarek
 *
 *
 *
 */

public class DaveonIndexer implements IDaveonIndexation{



	private final ReentrantLock indexationLock 	= new ReentrantLock();
	private boolean _isRunning		=	false;

	private pl.daveon.slave.WORKMODE _wm			=	WORKMODE.FULLINDEXING_MODE;

	public static final String CONTEXT_KEY				= "CONTEXT";
	public static final String CONTEXT_KEY_STATS		= "STATS";
	public static final String LINK_KEY					= "LINK";
	public static final String MOD_KEY					= "MODIFIED";
	public static final String FT_KEY					= "FILETYPE";
	public static final String METADATA					= "METADATA";
	public static final String CONTENTTYPE				= "CONTENTTYPE";
	public static final String DOC_NAME					= "DOCNAME";
	public static final String CCSUM					= "CCSUM";
	
	private 			long 	_crawlerMinimumDocs		= 10;


	/**
	 * UNLIMITED to bylo Integer.MAX_VALUE
	 * LIMITED to bylo 10.000
	 */
	private Integer MAX_FILED_TOKEN_COUNT = 100000;
	/**
	 * default is 16 MB
	 */
	public double  BUFFER_TO_FLUSH_SIZE_MB = 500.0;

	private final  String		_mmapcache			= "/../_indexcache";
	private static Logger		logger				= Logger.getLogger(DaveonIndexer.class);


	private String		_rootLocation;
	private String		_rootLocationURLForm;
//	private String		_linkTo;
	/**
	 * can be overrided - pass new value to calss constructor
	 */
	public String		_indexLocation			= "_codeindex";

	private IndexWriter	ramWriter				= null;

	private int			_ProcessingCoresCount	= Runtime.getRuntime().availableProcessors();
	/**
	 * zmienna trzyma wskazanie na fs do plikow dla klasy @CyclesCrowler
	 */
	private String 		_crowlerData			= "";
	private ArrayList<DaveonDocumentState> 		_preIndexedFilesMeta = null;
	private HashMap<String, String> commitUserData = null;



	/**
	 * @param rootLocation
	 * @param linkTo
	 * @param linkFrom
	 * @param docsNumber
	 * @param indexLocation
	 */
	public DaveonIndexer(String rootLocation, String indexLocation)
	{
		this.setRootLocation(rootLocation);
		this.setIndexLocation(indexLocation);
		
		 Properties properties = new Properties();
	     InputStream is = null;
	     try
	        {
		        is = DaveonConcurrentMergeScheduler.class.getResourceAsStream("merge.configuration.xml");
		        try {
					properties.loadFromXML(is);
				} 
		        catch (InvalidPropertiesFormatException e1) {e1.printStackTrace();} 
		        catch (IOException e1) {e1.printStackTrace();}
		        
			 try {
				 this._crawlerMinimumDocs = Long.valueOf(properties.getProperty("_Crawler_MinimumDocsToEngage","10"));	    		
		        } 
			 catch (Exception e) {e.printStackTrace();}
	        }
	        finally
	        {
	        	if(is!=null)
					try {
						is.close();
					} catch (IOException e) {
						logger.error("brak mozliwosci odczytania konfiguracji: "+e.getMessage());
					}
	        }
	        
	}

	public DaveonIndexer(String rootLocation, String indexLocation, WORKMODE wm)
	{
		this(rootLocation, indexLocation);
		this._wm	= wm;
		
	}

	/**
	 * @return zwracana Polityka Mergy - ta jest wielowatkowa
	 */
	public TieredMergePolicy getMergePolicy()
	{
		 	Properties properties = new Properties();
	        InputStream is = null;
			TieredMergePolicy tmp = new TieredMergePolicy();
	        try
	        {
		        is = DaveonConcurrentMergeScheduler.class.getResourceAsStream("merge.configuration.xml");
		        try {
					properties.loadFromXML(is);
				} catch (InvalidPropertiesFormatException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		        
			 try {
	            String _segments = properties.getProperty("SegmentsPerTier", "60.0");
	            String _maxMerges = properties.getProperty("MaxMergeAtOnce", "16");
	            String _maxMSMB = properties.getProperty("MaxMergedSegmentMB", "1000.0");
	            this.BUFFER_TO_FLUSH_SIZE_MB = Double.valueOf(properties.getProperty("BufferToFlushSize", "500.0"));
	            this.MAX_FILED_TOKEN_COUNT = Integer.valueOf(properties.getProperty("maxFieldTokenCount", "100000"));
	            String _isCompound = properties.getProperty("UseCompoundFile", "false");
	            
	    		tmp.setMaxMergeAtOnce(Integer.valueOf(_maxMerges));
	    		tmp.setSegmentsPerTier(Double.valueOf(_segments));
	    		tmp.setUseCompoundFile(Boolean.valueOf(_isCompound));
	    		tmp.setMaxMergedSegmentMB(Double.valueOf(_maxMSMB));
	    		
		        } catch (Exception e) {
		            e.printStackTrace();
		        }
	        }
	        finally
	        {
	        	if(is!=null)
					try {
						is.close();
					} catch (IOException e) {
						logger.error("brak mozliwosciodczytania konfiguracji: "+e.getMessage());
					}
	        }
		return tmp;
	}
	
	public TieredMergePolicy getOptimizePolicy()
	{
		 	Properties properties = new Properties();
	        InputStream is = null;
			TieredMergePolicy tmp = new TieredMergePolicy();
	        try
	        {
		        is = DaveonConcurrentMergeScheduler.class.getResourceAsStream("merge.configuration.xml");
		        try {
					properties.loadFromXML(is);
				} catch (InvalidPropertiesFormatException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		        
			 try {
	            String _optimize_segments = properties.getProperty("OPTIMIZE_SegmentsPerTier", "30.0");
	            String _optimize_maxMerges = properties.getProperty("OPTIMIZE_MaxMergeAtOnce", "8");
	            String _optimize_maxMSMB = properties.getProperty("OPTIMIZE_MaxMergedSegmentMB", "1000.0");
	            String _isCompound = properties.getProperty("OPTIMIZE_UseCompoundFile", "true");
	            
	    		tmp.setMaxMergeAtOnce(Integer.valueOf(_optimize_maxMerges));
	    		tmp.setSegmentsPerTier(Double.valueOf(_optimize_segments));
	    		tmp.setUseCompoundFile(Boolean.valueOf(_isCompound));
	    		tmp.setMaxMergedSegmentMB(Double.valueOf(_optimize_maxMSMB));
	    		
		        } catch (Exception e) {
		            e.printStackTrace();
		        }
	        }
	        finally
	        {
	        	if(is!=null)
					try {
						is.close();
					} catch (IOException e) {
						logger.error("brak mozliwosciodczytania konfiguracji: "+e.getMessage());
					}
	        }
		return tmp;
	}

	
	/**
	 * @return zwraca min ilosc dokumentow od ktorej wystartuje indexacja  domyslnie 10
	 */
	public long getMinimumCrawlerDocsToEngage()
	{
		return this._crawlerMinimumDocs;
	}

	/**
	 * @return Zwraca config IndexWriter'a
	 */
	public IndexWriterConfig  getIndexWriterConfig()
	{
		PolishAnalyzer pa = new PolishAnalyzer(Version.LUCENE_43);
		LimitTokenCountAnalyzer lmsa = new LimitTokenCountAnalyzer(pa, this.MAX_FILED_TOKEN_COUNT);

		/*wariacja mechanizmu otwierania indexu w swietle trybu pracy indexera ->WORKMODE*/
		IndexWriterConfig ramWriterCFG = new IndexWriterConfig(Version.LUCENE_43, lmsa).setOpenMode(getWorkMode()==WORKMODE.FULLINDEXING_MODE ? OpenMode.CREATE:OpenMode.APPEND);

		ramWriterCFG.setRAMBufferSizeMB(BUFFER_TO_FLUSH_SIZE_MB);
		ramWriterCFG.setMergePolicy(getMergePolicy());
		ramWriterCFG.setMergeScheduler(new DaveonConcurrentMergeScheduler());

		if(getProcessingCoresCount()>IndexWriterConfig.DEFAULT_MAX_THREAD_STATES)
			ramWriterCFG.setMaxThreadStates(getProcessingCoresCount());

		//info informacyjne dla uzytkownika do logow
		InformWorldAboutIndexingConfiguration(ramWriterCFG);

		return ramWriterCFG;
	}

	public IndexWriterConfig  getIndexWriterConfigForOptimize()
	{
		PolishAnalyzer pa = new PolishAnalyzer(Version.LUCENE_43);
		LimitTokenCountAnalyzer lmsa = new LimitTokenCountAnalyzer(pa, this.MAX_FILED_TOKEN_COUNT);

		/*wariacja mechanizmu otwierania indexu w swietle trybu pracy indexera ->WORKMODE*/
		IndexWriterConfig ramWriterCFG = new IndexWriterConfig(Version.LUCENE_43, lmsa).setOpenMode(getWorkMode()==WORKMODE.OPTIMIZE ? OpenMode.CREATE_OR_APPEND:OpenMode.APPEND);

		ramWriterCFG.setMergePolicy(getOptimizePolicy());
		ramWriterCFG.setMergeScheduler(new DaveonConcurrentMergeScheduler());

		if(getProcessingCoresCount()>IndexWriterConfig.DEFAULT_MAX_THREAD_STATES)
			ramWriterCFG.setMaxThreadStates(getProcessingCoresCount());

		//info informacyjne dla uzytkownika do logow
		InformWorldAboutIndexingConfiguration(ramWriterCFG);

		return ramWriterCFG;
	}
	
	/**
	 * wypisuje informacje o konfiguracji IndexWriter'a  - jest zalezna od WORKMODE
	 */
	private void InformWorldAboutIndexingConfiguration(IndexWriterConfig cfg)
	{
		logger.info("IndexWriter Configuration=================");
		logger.info(cfg.toString());
		logger.info("Using: "+getProcessingCoresCount()+" processing cores to gain concurrency.");
		logger.info("==========================================");
	}

	/**
	 * Ustala tak�e finalna glebokosc BlockingQueue
	 * @return zwraca IndeXwriter bazujacy na MMapDirectory i IndexWwriterConfig z getRAMIndexWriterConfig()
	 * @throws IOException
	 * @throws LockObtainFailedException
	 * @throws CorruptIndexException
	 */
	public IndexWriter fastIndexWriterOpen() throws CorruptIndexException, LockObtainFailedException, IOException
	{
		String indexLocation = getIndexCacheDirName();
		if(getWorkMode()==WORKMODE.FULLINDEXING_MODE)
			setupCacheLocation();
		else
			indexLocation = getIndexLocation();

		return new IndexWriter(FSDirectory.open(new File(indexLocation)), getIndexWriterConfig());
	}

	
	public IndexWriter IndexWriterForOptimize() throws CorruptIndexException, LockObtainFailedException, IOException
	{
		String indexLocation = getIndexLocation();
		if(getWorkMode()==WORKMODE.OPTIMIZE)
		{
			indexLocation = getIndexLocation();
			return new IndexWriter(FSDirectory.open(new File(indexLocation)), getIndexWriterConfigForOptimize());
		}
		else 
			return null;
	}

	/**
	 * Metoda ktora tworzy Map'e informujaco
	 * na temat ostatniego commit'a - jakies dodatkowe dane
	 * o procesie i zakonczeniu indexacji
	 */
	public HashMap<String, String> getIndexationInfo(){

		return this.commitUserData;
	}

	public void putCommitData(String key, String val)
	{
		if(key!=null && val!=null)
		{
			if (this.commitUserData==null) this.commitUserData = new HashMap<String, String>();
			if(this.commitUserData.containsKey(key)) commitUserData.remove(key);
			this.commitUserData.put(key, val);
		}
	}

	/**
	 * Ustawia/tworzy katalog w ktorym zapisuje sie generowany index
	 * @throws Exception
	 */
	private void setupCacheLocation() throws IOException{

		File mmapTempDir = new File(getIndexCacheDirName());
//		if (mmapTempDir.isDirectory())
//				mmapTempDir.delete();
		if(!mmapTempDir.exists())
			mmapTempDir.mkdir();

		if(!mmapTempDir.exists()) throw new IOException("Unable to create index storing location:"+getIndexCacheDirName());
	}


	private List<String> getDocumentsList()
	{
		File rootDir = new File(getRootLocation());
		StringBuilder sb = new StringBuilder();

		CyclesCrawler cc = null;
		List<String> result = null;

		if(getWorkMode()==WORKMODE.FULLINDEXING_MODE)
		{
			generateFilesList(rootDir, sb);
			if (sb.length()>0)
			{
				try {
					final OutputStream os = new FileOutputStream("fullIndexing.files.list");
					logger.info(WORKMODE.FULLINDEXING_MODE+": affected files list stored in: "+"fullIndexing.files.list");
					final PrintWriter pw = new PrintWriter(os);
					pw.write(sb.toString());
					pw.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				
				result = Arrays.asList(sb.toString().split("\r\n"));
			}
			else
				result = null;
		}
		else
		{
			try {
				cc = new CyclesCrawler(getCrawlerLocation(), getRootLocation());
				try {
					this._preIndexedFilesMeta = getDocumentsInfo(cc);
				} catch (IOException e) {
					e.printStackTrace();
				}

				putCommitData("REVISION", String.valueOf(cc.getRevision()));

				/*tutaj stworzenie listy informacji o plikach:
				 * --czy plik istnieje w indexie - jesli tak to wtedy jego update/dodane, w innym przypadku nic
				 * */
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			if(cc.size()>0)
				result = cc;
			else
				result = null;
		}

		return result;
	}

	public String[] getVirtualPathToURL(String localFile, int unwantedLength)
	{
		String localPath 	= "";
		try {
			localPath = new File(localFile).toURI().toURL().toExternalForm().toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		String reposPath = localPath.subSequence(unwantedLength-1, localPath.length()).toString();
		reposPath = reposPath.replaceAll("\\+", "%2b");

		return new String[]{reposPath, DocIndexingThred.getMD5(reposPath)};
	}

	private ArrayList<DaveonDocumentState> getDocumentsInfo(ArrayList<String> src) throws IOException
	{
		if(src!=null && src.size()>0)
		{
			HashSet<DaveonDocumentState> hslds = new HashSet<DaveonDocumentState>();
			for(String fp : src)
			{
				DaveonDocumentState lds = new DaveonDocumentState(fp);
				String[] st = getVirtualPathToURL(fp,getRootLocationURLForm().length());
				lds.setStrippedPath(st[0]);
				lds.setMD5(st[1]);
				hslds.add(lds);
			}

			return getPreIndexedDocumentsState(hslds);
		}
		else
			return new ArrayList<DaveonDocumentState>();
	}


	private ArrayList<DaveonDocumentState> getPreIndexedDocumentsState(HashSet<DaveonDocumentState> src) throws IOException
	{
		TopDocs td = null;
		IndexReader reader = null;
		ArrayList<DaveonDocumentState> result = new ArrayList<DaveonDocumentState>();
		try
		{
			try {
				reader = DirectoryReader.open(FSDirectory.open(new File(getIndexLocation())));
			} catch (CorruptIndexException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			IndexSearcher qsearch = new IndexSearcher(reader);

			for(DaveonDocumentState lds: src)
			{
				TermQuery query = new TermQuery(new Term(DaveonIndexer.CCSUM,lds.getMD5()));
				td = qsearch.search(query, 1);
				if(td.totalHits>0)
					lds.setState(STATE.STORED);
				else
					lds.setState(STATE.NOTSTORED);

				result.add(lds);
			}
		}
		finally
		{
			reader.close();
		}
		return result;
	}


	private void generateFilesList(File dir,StringBuilder filesList) {


		if (dir.isDirectory() && !dir.isHidden())
		{
			File[] children = dir.listFiles();
			for (File child : children)
			{
				generateFilesList(child, filesList);
			}
		}
		else
			if(!dir.isHidden())
				filesList.append(dir.getPath()+"\r\n");
	}

	/**
	 * @return sceizka na fs do listy plikow parsowanych przez CyclesCrowler'a
	 * w postaci okreslonej prze @CyclesCrawler.CROWLER_PATTERN
	 *
	 * warto� to�sama z RootLocation - po utworzeniu LucyIndexer'a mozna t� warto�c zmienic poprzez #setRootLocation
	 */
	public String getCrawlerLocation()
	{
		return this._crowlerData;
	}

	/**
	 * @param crowlerDataPath - sciezka do plikow dla CyclesCrowler'a
	 */
	public void setCrawlerLocation(String crowlerDataPath) throws FileNotFoundException
	{
		if(new File(crowlerDataPath).exists())
			this._crowlerData = crowlerDataPath;
		else
			throw new FileNotFoundException("Location! NotFound: setCrawlerLocation: "+crowlerDataPath);
	}

	/**
	 * Inicjuje obiekty (nie startuje watk�w) klasy indexujacej
	 */
	private DocIndexingThred[] initializeIndexingThreads(ArrayBlockingQueue<String> _queue, int threadsCnt) throws InterruptedException
	{
		DocIndexingThred[] dits = new DocIndexingThred[threadsCnt];
		for(int i=0; i<dits.length; i++)
		{
			dits[i] = new DocIndexingThred(_queue, "DIT:"+String.valueOf(i), this);
			logger.info("DocIndexingThred("+dits[i].toString()+") thread started - awaiting queue fillup.");
		}
		return dits;
	}

	/**
	 * startuje watki indexujace - moze by wykonane tylko i wylacznie po initializeIndexingThreads
	 * @param dits
	 * @throws NullPointerException
	 */
	private void startAllDocIndexingThred(DocIndexingThred[] dits) throws NullPointerException
	{
		if (dits==null) throw new NullPointerException("Indexing Threads array is empty/not initialized");
		for(DocIndexingThred dit : dits)
			dit.start();
	}

	/**
	 * Metoda blokowana przez ReetrantLock - n eida rady wykonac jej dwa razy
	 * w momencie gdy zostala juz raz invokowana
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws LockObtainFailedException
	 * @throws CorruptIndexException
	 * @throws Exception
	 * @throws Error
	 */
	public void engageIndexing() throws InterruptedException, CorruptIndexException, LockObtainFailedException, IOException
	{
		if(this.indexationLock.tryLock())
		{
			try
			{
					logger.info("Wersja LucyIndexer: 20.07.2013 at core "+Version.LUCENE_43);
					logger.info("Tryp pracy indexera: "+getWorkMode());
					logger.info("Rozpoczecie indexowania galezi: " + getRootLocation());

					List<String> fList = getDocumentsList();
					String communicateAction = "No data attached (Documents list is empty).";

					if (fList!=null && this.getWorkMode()==WORKMODE.CROWLING_MODE && fList.size() < getMinimumCrawlerDocsToEngage())
					{
						communicateAction = "Minimum documents treshold for "+getWorkMode()+" is not met (min is:"+getMinimumCrawlerDocsToEngage()+" current is:"+fList.size()+")";
						fList.clear();
						fList = null;
					}
					
					if (fList== null)
					{
						logger.info(communicateAction);
					}
					else
					{
						logger.info("File List generated. Found "+fList.size()+" potential files for indexing.");

						DocIndexingThred[] dits = null;
						int queueDepth = (fList.size() < getProcessingCoresCount()) ? fList.size(): getProcessingCoresCount();
						ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(queueDepth);
						
						try
						{
							dits = initializeIndexingThreads(queue, queueDepth);
							startAllDocIndexingThred(dits);
						}
						catch (InterruptedException ie)
						{}
						catch (NullPointerException np)
						{}

						logger.info("Starting queue fill. Blocking at "+getProcessingCoresCount()+" items depth.");

						this.ramWriter = fastIndexWriterOpen();

						//okresla godzine startu pracy indexerow

						Date startDate = new Date();
						/*
						 * wstawiane kolejne wartosci do kolejki @queue sa blokowane
						 * gdy osiagna wilkosc kolejki - put() jest metoda blokujaca
						 * */
						for(String f : fList)
						{
							queue.put(f);
//							logger.info("puting on market:"+f);
						}

						logger.info("List iteration complete. Stopping Threads..");
						for(DocIndexingThred dit : dits)
							dit.stopFreeRun();

						//

						/*
						 * Pierwsze zabezpieczenie przed wymknieciem sie w�tk�w.
						 * ZALETA: nie zabija procka na 100% - i koncowe dokuemmenty indeksuja
						 * sie w miare sprawnie
						 */
						for (DocIndexingThred f : dits)
						{
					         f.join(20*1000);
					    }
						logger.info("Threads REJOIN complete.");

						/*
						 * WADA: Mocno obcia�a procesor
						 *
						 * Drugie zabezpieczenie przed wycikajacym w�tkiem.
						 *
						 * kiedy stopFreeRun() jest woany to nie jest to jednoznacze z zakonczeniem zadania
						 * i nie oznacza to ze IndexWriter jest zwolniony- moze wciaz byc uzywany
						 * bo trwa np dlugo operacja dodawania do indexu jakiegos duuzego pliku/jego procesowanie
						 * dlatego tzreba sprawdzi ile w�tk�w wyszo z petli oczekujacej w kolejce BlockingQueue
						 * */
//						int threadsfinished = 0;
//						synchronized(ThreadPresenceCounter.THREAD_PRESENCE_COUNTER)
//						{
//							while(ThreadPresenceCounter.THREAD_PRESENCE_COUNTER.get()!=getProcessingCoresCount()){
//								threadsfinished = ThreadPresenceCounter.THREAD_PRESENCE_COUNTER.get();
//								logger.trace(threadsfinished+" threads has finished. Awaiting "+(getProcessingCoresCount()-threadsfinished)+" other threads...");
//							}
//						}

						closeRamWriter(startDate);
					}
			}
			finally
			{
				this.indexationLock.unlock();
			}
		}
		else
			logger.info("Lock obtained");
	}


	private void forcelyClose()
	{
		boolean done = true;
		try {
			done = tryRamWriterCommit(this.ramWriter, null);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally
		{
			if(ramWriter != null || done==false)
				try {
					ramWriter.close(true);
				} catch (CorruptIndexException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	private void closeRamWriter(Date _start) throws CorruptIndexException, IOException {

		boolean closeIsForced = true;
		try
		{
			int docsInRam = ramWriter.numRamDocs();

			DateFormat format = new SimpleDateFormat(DaveonSearcher.DATE_PRESENATION);
			format.setTimeZone(TimeZone.getTimeZone(DaveonSearcher.DATE_TIMEZONE));
			
			putCommitData("Aktualnosc indexu", "<b>"+String.valueOf(format.format(new Date()))+"</b>");

			closeIsForced = tryRamWriterCommit(ramWriter, getIndexationInfo());

			logger.info("Finalizacja .. commit "+docsInRam+" docs from IndexWriter's ram.");
			logger.info("");
			logger.info("PODSUMOWANIE: ");
			logger.info("Rozpoczecie indexowania    : " + _start);
			logger.info("Zakonczenie indexowania    : " + new Date());
			logger.info("Liczba dokument�w w indexie  : " + ramWriter.numDocs());
			logger.info("");
		}
		finally{
			if(ramWriter != null && closeIsForced==false)
				ramWriter.close(true);
		}

	}

	private boolean tryRamWriterCommit(IndexWriter iw, HashMap<String, String> commitData) throws CorruptIndexException, IOException {
		boolean unabletocommit = false;
		try
		{
			iw.setCommitData(commitData); 
			iw.prepareCommit();
		}
		catch(OutOfMemoryError e){
			unabletocommit = true;
			iw.close();
			}
		finally
		{
			if(!unabletocommit)
			{
				iw.commit();
				iw.waitForMerges();
			}
			else
				iw.close(false);
		}
		return unabletocommit;
	}



	public String getRootLocation() {
		return this._rootLocation;
	}

	public String getRootLocationURLForm() {
		return this._rootLocationURLForm;
	}

	public void setRootLocation(String rootLocation) {
		this._rootLocation = rootLocation;
		try {
			this._rootLocationURLForm = new File(_rootLocation).toURI().toURL().toExternalForm();
		} catch (MalformedURLException e) {
		this._rootLocationURLForm = _rootLocation;
			e.printStackTrace();
		}
	}

	public String getIndexLocation() {
		return _indexLocation;
	}
	public String getIndexCacheDirName() {
		return getIndexLocation()+ this._mmapcache;
	}

	public void setIndexLocation(String indexLocation) {
		this._indexLocation = indexLocation;
	}

	public int getProcessingCoresCount() {
		return _ProcessingCoresCount;
	}
	public void setProcessingCoresCount(int thredsCnt) {
		this._ProcessingCoresCount = thredsCnt;
	}

	public synchronized boolean isIndexationRuning()
	{
		return this._isRunning;
	}

	/**
	 * @return zwraca tryb inicjalizacji indexer'a
	 */
	public WORKMODE getWorkMode() {
		return _wm;
	}


	/**
	 * Mechanizm ReentrantLock'a gwarantuje ze tryb pracy nie zmieni sie
	 * w trakcie indeksacji
	 * @param mwm - nowa tryb WORKMODE
	 * @return zwraca @true jesli tryb prac WORKMODE moze byc zmieniony
	 * - indexacja nie zostala jeszcze rozpoczeta lub @false jesli
	 * 	indexacja jest w toku
	 */
	public boolean setWorkMode(WORKMODE mwm) {

		boolean lockObtained = this.indexationLock.isLocked();

		if(!lockObtained)
			this._wm = mwm;

		return !lockObtained;
	}

	@Override
	public void addDocument(Document _doc, IStearingThred callingThread) throws CorruptIndexException, IOException {
		
		try
		{
			this.ramWriter.addDocument(_doc);
		}
		catch (OutOfMemoryError oofm)
		{
			ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
			for(final ThreadInfo info : threads)
			    logger.info(info);
			
			callingThread.suspendWork();
//			LucyConcurrentMergeScheduler.printRuntimeStats();
			forcelyClose();
			logger.info("All threads suspended(not atomic). Execute thread dump! (kill -3 PID)");
		}
//		if(ramWriter.ramSizeInBytes() > ramWriter.)
	}

	public void removeDocument(Term _docTerm)throws CorruptIndexException, IOException {
		this.ramWriter.deleteDocuments(_docTerm);
	}

	public void updateDocument(Term _docTerm, Document _doc)throws CorruptIndexException, IOException{
		this.ramWriter.updateDocument(_docTerm, _doc);
	}

	public ArrayList<DaveonDocumentState> getPreIndexedFilesMeta()
	{
		return this._preIndexedFilesMeta;
	}
	

	/**
	 * @param args
	 *	0 - "D:\tomcat\LucyDocs"
		1 -	"D:\tomcat\apache-tomcat-7.0.39\_index"
		2 - "10"
		3 - "http://localhost/"
	 *
	 */
	public static void main(String args[])
	{
		org.apache.log4j.PropertyConfigurator.configure("properties/log4j.properties");


		String docRootLocation 	= args[0];
		String indexlocation 	= (args[1]==null || args[1].isEmpty()) ? "_codeindex":args[1];
		String threadsCnt		= args.length>2 ? threadsCnt=args[2]:"";


		DaveonIndexer li = new DaveonIndexer(docRootLocation, indexlocation);
		if (!threadsCnt.isEmpty())
				li.setProcessingCoresCount(Integer.valueOf(threadsCnt));
		try {
			/*tryb CROWLING_MODE przeznaczony do aktualizacji indexu*/
//			li.setWorkMode(WORKMODE.CROWLING_MODE);
//			li.setCrawlerLocation("D:\\tomcat\\svn.cycles");

			li.engageIndexing();


		} catch (Exception e) {
			e.printStackTrace();
		} catch (Error e) {
			e.printStackTrace();
		}

		System.out.println("LucyIndexer has Finished");
	}

	@Override
	public void forceFlush() {
		try {
			this.ramWriter.commit();
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
