package pl.daveon.core;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.pl.PolishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.vectorhighlight.BaseFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.ScoreOrderFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


/**
 * @author bkaczmarek
 *
 *
 *
 */
public class DaveonSearcher
{
	public static final String	DATE_PRESENATION 		= "E yyyy-MM-dd HH:mm:ss";
	public static final String	DATE_TIMEZONE 			= "Europe/Belgrade";
	public static final String	LOC_ENCODING 			= "ISO-8859-1";

	public final static int		OPT_RELEVANCE			= 0;

	public final static int		OPT_LASTMODIFIED		= 1;

	public final static int		OPT_LASTMODIFIEDREVERT	= 2;

	public final static String	partsSeparator			= "#dupa##dupa#";

	public final static String	beginTag				= "<span class=\"highlt\">";

	public final static String	endTag					= "</span>";

	private final static int 	HITS_FRAGMENTS_LENGTH	=	200*2;

	private 			String 	summary					= "";

	public static void main(String args[])
	{
		try
		{
			String[] results = null;
			if (args[2].trim().toLowerCase().equals("null") || args[2].trim().length() == 0) args[2] = null;
			results = new DaveonSearcher().searchIndex(new File(args[0]), args[2], args[1],"cos", 1, 5);

			if (results == null)
			{
				System.out.println("Podczas wyszukiwania wyst¹pi³ b³¹d.");
			} else
				if (results.length == 0)
				{
					System.out.println("Nie znaleziono wyników pasuj¹cych do kryterium");
				} else
				{
					for (int i = 2; i < results.length; i++)
					{
						System.out.println(results[i]);
					}
					System.out.println("Zapytanie googlowe: " + args[1]);
					System.out.println("Trafionych : " + results[0]);
					System.out.println("Czas wykonywania [ms] : " + results[1]);
				}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);

		}
	}

	public String[] searchIndex(File _indexDir, String _defaultField, String _googleQuery,
			String prefixUrl, int pager, int resultsPerPage)
	{
		return searchIndex(_indexDir,
				_defaultField,
				_googleQuery,
				prefixUrl,
				pager,
				resultsPerPage,
				DaveonSearcher.OPT_RELEVANCE);
	}
	
	public enum KEYE
	{
		CONTEXT(DaveonIndexer.CONTEXT_KEY), FILETYPE(DaveonIndexer.FT_KEY), LINK(DaveonIndexer.LINK_KEY), INURL(DaveonIndexer.LINK_KEY), URL(DaveonIndexer.LINK_KEY);
		
		String _fld = "CONTEXT";
		KEYE(String field)
		{
			this._fld = field;
		}
		
		public String[] processKeyQueries(String srequest)
		{
			Pattern pt = null;
			String uprsrequest = srequest.toUpperCase(Locale.getDefault());
			
			if(srequest==null || srequest.isEmpty())
				return null;
			else
			{
				if(uprsrequest.contains(KEYE.FILETYPE.toString()))
				{
					pt = Pattern.compile("(?i)(.{0,1}"+KEYE.FILETYPE+":[a-z,]+){1}");
					Matcher mt = pt.matcher(uprsrequest);
					if(mt.find())
						System.out.println(mt.group());
				}
					
				/*nalezy sprawdzic czy w tekscie jest request szukania po wybranych polach (LINK|INURL|URL, CONTEXT, FILETYPE)*/
				return null;
			}
		}
		
		public boolean isPositive()
		{
			return true;
		}
		
		public String toString()
		{
			return this._fld;
		}

	}

	/**
	 * Metoda odpowiedzialna za przeszukiwanie i zwracaj¹ca wyniki
	 *
	 * @return null - jeœli wyst¹pi³ wyj¹tek/b³¹d, String[0] jeœli nie ma ¿adnych trafieñ, String[] wyników jeœli
	 *         s¹ jakieœ trafienia. Ka¿dy ze zwracanych String'ów ma format:
	 *         linkDoDokumentu//partsSeparator////begingTag//trafionyTekst//endTag//, gdzie //separator// gdzie
	 *         //partsSeparator//, //beginTag// oraz //endTag// to zmienne, okreœlaj¹ce w jaki sposób ma byæ
	 *         zwrócony wynik.
	 * @author dmaciejewski
	 */

//	@SuppressWarnings("unchecked")
	public String[] searchIndex(File _indexDir, String _defaultField, String _googleQuery, String prefixUrl,
			int pager, int  resultsPerPage, int searchOption)
	{
		Date startDate = new Date();
		String[] out;
		PolishAnalyzer analyzer = new PolishAnalyzer(Version.LUCENE_43);


		try
		{

			String entireQuery = _googleQuery.trim();
			BooleanQuery.setMaxClauseCount(500000);

			IndexReader reader = null;
			IndexSearcher searcher = null;

			QueryParser queryParser = null;
			Query query = null;

			ScoreDoc[] hits = null;

			try{

				reader = DirectoryReader.open(FSDirectory.open(_indexDir));
				/**/
//				reader.addReaderClosedListener(new ReaderClosedListener(){
//
//					@Override
//					public void onClose(IndexReader arg0) {
//						System.out.println("Firs reader is being closed");						
//					}});
				/*informacje dodatkowe: */
				long numberOfDocs = reader.numDocs();
				this.summary = "";
	
				SegmentInfos sis = new SegmentInfos();
				Map<String, String> additionalD = null;
				try
				{
					sis.read(FSDirectory.open(_indexDir));
					additionalD = sis.getUserData();
				}
				catch (IOException ioe)
				{
					additionalD = null;
				}
				
				sis = null;
				if(additionalD!=null && additionalD.size()>0)
				{
					Iterator it = additionalD.entrySet().iterator();
					while(it.hasNext())
					{
						Map.Entry pair = (Map.Entry)it.next();
						String key = (String) pair.getKey();
						String val = (String) pair.getValue();
						this.summary+=key+": "+val+" ";
					}
				}
				this.summary+="	Dokumentow w indexie: <b>"+String.format("%,8d%n", numberOfDocs)+"</b>";
	//			System.out.println(getSummary());
				/**/
				searcher = new IndexSearcher(reader);
				try{
	
					queryParser = new QueryParser(Version.LUCENE_43, DaveonIndexer.CONTEXT_KEY, analyzer);
					query = queryParser.parse(entireQuery);
	
				}
				catch (ParseException ex)
				{
//					System.out.println("Podano niewlasciwa skladnie zapytania : " + ex.getMessage());
					System.out.println("Podano niewlasciwa skladnie zapytania.");
					return new String[] { "" + 0, "" + 0 };
				}
	
				switch (searchOption)
				{
					case DaveonSearcher.OPT_RELEVANCE:
						hits = searcher.search(query, 1000).scoreDocs;
						break;
					case DaveonSearcher.OPT_LASTMODIFIEDREVERT:
						hits = searcher.search(query, null, 1000, new Sort(new SortField(DaveonIndexer.MOD_KEY, SortField.Type.STRING, false))).scoreDocs;
						break;
					case DaveonSearcher.OPT_LASTMODIFIED:
						hits = searcher.search(query, null, 1000, new Sort(new SortField(DaveonIndexer.MOD_KEY, SortField.Type.STRING, true))).scoreDocs;
						break;
					default:
						hits = searcher.search(query, 1000).scoreDocs;
				}

			}
			finally{
				if(reader!=null)
					reader.close();
//				System.out.println("Closing searcher. Will it invoke close to READER?");
//				searcher.
//				searcher.close();
			}

			try
			{
			reader =  DirectoryReader.open(FSDirectory.open(_indexDir));
			/**/
//			reader.addReaderClosedListener(new ReaderClosedListener(){
//
//				@Override
//				public void onClose(IndexReader arg0) {
//					System.out.println("Second reader is being closed");						
//				}});
			/**/

			DateFormat format = new SimpleDateFormat(DaveonSearcher.DATE_PRESENATION);
			format.setTimeZone(TimeZone.getTimeZone(DaveonSearcher.DATE_TIMEZONE));

			searcher = new IndexSearcher(reader);
			int foundResultsNumber = (hits != null) ? hits.length : 0;


			out = new String[resultsPerPage + 1 + 2];
			
			//index:0 - okresla ilosc wynikow - ilosc trafien zapytania
			out[0] = "" + foundResultsNumber;

			//okresla index od ktorego bêd¹ zbierane informacje o wynikach - czyli elementy SearchElement
			final int dataOffset = 2;

				if (foundResultsNumber > 0)
				{

					FastVectorHighlighter highlighter = getHighlighter();
					FieldQuery fieldQuery = highlighter.getFieldQuery(query);

					int recordsCounter = 0;
					int recordsPager = 0;

					for (int i = 0; i<hits.length; i++)
					{
						if (recordsCounter >= resultsPerPage)
						{
							/*jesli przekroczymy recordsCounter tzn ze przekroczylismy ilos wynikow jaka ma byc zebrana i pokazana na danej stronie
							 * wtedy break z petli - czyli okresla nam/ogranicza
							 * ilosc elementow do wyswietlenia na stronie */
							break;
						}

						ScoreDoc hit = hits[i];
//						ScoreDoc hit = hits[recordsCounter + i + (pager*resultsPerPage)];
//						System.out.println("My pager is: "+pager);
						
//						if (recordsPager >= pager && recordsCounter < (recordsPager + resultsPerPage))
//						{

						if (recordsPager >= ((pager-1)*resultsPerPage) && recordsCounter <= resultsPerPage)
						{
//							System.out.println("Adding scoreDoc at hit_ID: "+i);

							int doc_id = hit.doc;
							Document doc = searcher.doc(doc_id);

							String ftype = doc.get(DaveonIndexer.FT_KEY);
							String fname = doc.get(DaveonIndexer.DOC_NAME);
							String ftitle = fname;//doc.get(LucyIndexer.DOC_TITLE);

							String link = prefixUrl+doc.get(DaveonIndexer.LINK_KEY);
							link = URLDecoder.decode(link,DaveonSearcher.LOC_ENCODING);

//							long modified = 0;
//							try
//							{
//								String modval = doc.get(DaveonIndexer.MOD_KEY);
//								modified = Long.valueOf(modval);
//
//							}
//							catch(NumberFormatException nfe)
//							{
//								System.err.println(nfe.getMessage());
//								modified = new Date().getTime();
//							}
//							String formatted = format.format(new Date(modified));
							String frag = highlighter.getBestFragment(fieldQuery, searcher.getIndexReader(), doc_id, DaveonIndexer.CONTEXT_KEY, DaveonSearcher.HITS_FRAGMENTS_LENGTH);

							out[recordsCounter + dataOffset] = link + partsSeparator
//									+ ((frag != null) ? frag : (beginTag + positive.toString() + endTag))
									+ ((frag != null) ? frag : (beginTag + entireQuery + endTag))
//									+ partsSeparator + formatted
									+ partsSeparator + ftype
									+ partsSeparator + fname
									+ partsSeparator + ftitle;

							recordsCounter++;
						}
						recordsPager++;
					}
				}
			}
			finally
			{
				if(reader !=null )
					reader.close();
//				System.out.println("Closing searcher#2");
//				searcher.close();
			}

			//index:1 - okresla czas poswiecony na zgromadzenie wynikow zapytania
			out[1] = new Date().getTime() - startDate.getTime() + "";
			return out;
		}

		catch (Exception ex)
		{
			ex.printStackTrace();
			return new String[] { "" + 0, "" + 0 };
		}
		catch (Error er)
		{
			er.printStackTrace();
			return new String[] { "" + 0, "" + 0 };
		}
	}

	public String getSummary()
	{
		return this.summary;
	}

	public 	FastVectorHighlighter getHighlighter() {
		FragListBuilder fragListBuilder = new SimpleFragListBuilder();
		FragmentsBuilder fragmentBuilder = new ScoreOrderFragmentsBuilder(BaseFragmentsBuilder.COLORED_PRE_TAGS, BaseFragmentsBuilder.COLORED_POST_TAGS);
		return new FastVectorHighlighter(true, true, fragListBuilder, fragmentBuilder);
	}

}
