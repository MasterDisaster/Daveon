package pl.web.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
//import java.net.URI;
//import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

//import javax.servlet.http.HttpServletRequest;

import pl.daveon.core.*;

public class Daveon
{
	public static int		RESULTS_PER_PAGE	= 10;

	public static int		PAGES_LEFT			= 10;

	public static int		PAGES_RIGHT			= 10;

	public static String	indexDir			= "";
	public static String 	prefixURL			= "";
	public static String 	prefixURLreplacement= "";

	private String			query				= "";

	public String getQuery()
	{
		return query;
	}

	public void setQuery(String query)
	{
		this.query = query;
	}

	public static SearchResult<SearchElement> search(HttpServletRequest request)
	{
		long startTime = System.currentTimeMillis();
		String query = request.getParameter("query");
		String pager = request.getParameter("pager");
		String sortType = request.getParameter("radios");

		int sortTypeValue = DaveonSearcher.OPT_RELEVANCE;

		try
		{
			sortTypeValue = new Integer(sortType).intValue();
		}
		catch (NumberFormatException e1)
		{
		}

		if (pager == null || pager.equals("")) pager = "1";
		String client = request.getRemoteHost() + " (" + request.getRemoteAddr() + ")";

		SearchResult<SearchElement> result = new SearchResult<SearchElement>();
		result.setPager(Integer.parseInt(pager));
		result.setSortType("" + sortTypeValue);

		ArrayList<SearchElement> resultElements = new ArrayList<SearchElement>();
		if (query != null && !query.trim().equals(""))
		{
			String encodedQuery = "";
			try
			{
				byte[] oldValue = query.getBytes(pl.daveon.core.DaveonSearcher.LOC_ENCODING);
				encodedQuery = new String(oldValue, "UTF-8");

				result.setQuery(encodedQuery);
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}

			DaveonSearcher searcher = new DaveonSearcher();

			String[] searchResults = searcher.searchIndex(new File(indexDir),
														pl.daveon.core.DaveonIndexer.CONTEXT_KEY,
														encodedQuery,
														prefixURL,
														Integer.parseInt(pager),
														Daveon.RESULTS_PER_PAGE,
														sortTypeValue);
			result.setIndexInfoAdd(searcher.getSummary());
			if (searchResults != null)
			{
				if (searchResults.length > 0)
				{
					result.setCount(searchResults[0]);
				}
				if (searchResults.length > 1)
				{
					long time = Long.parseLong(searchResults[1]) / 1000;
					result.setTime("" + time + " sek.");
				}

				for (int i = 2; i < searchResults.length; i++)
				{
					String res = searchResults[i];
					if (res != null)
					{
						String[] searchResultsTmp = searchResults[i].split(DaveonSearcher.partsSeparator);

						SearchElement elem = new SearchElement();

						String urlASCII = searchResultsTmp[0];
						elem.setUrl(searchResultsTmp[0].replaceFirst(prefixURL, prefixURLreplacement));
						elem.setUrlHref(urlASCII);
						elem.setDesc(searchResultsTmp[1]);
						elem.setModified(searchResultsTmp[2]);

						elem.setFileType(searchResultsTmp[3]);
						elem.setFileName(searchResultsTmp[4]);
						elem.setDocTitle(searchResultsTmp[5]);

						resultElements.add(elem);
					}
				}
				result.setElements(resultElements);
			}

			long stopTime = System.currentTimeMillis();
			SimpleDateFormat sdf = new SimpleDateFormat(DaveonSearcher.DATE_PRESENATION);
//			System.out.println("requestTime:" + sdf.format(new Date()) + ", CLIENT:" + client + ", CzasWyszuk:"
//					+ (stopTime - startTime) + "[ms], QUERY:" + encodedQuery+", Page: "+pager+", wynikow w sumie: "+result.getCount());
			System.out.println("requestTime:" + sdf.format(new Date()) + ", CLIENT:" + client + ", CzasWyszuk:"
					+ (stopTime - startTime) + "[ms], Page: "+pager+", wynikow w sumie: "+result.getCount());
		}
		return result;
	}

//	public static void main(String[] args)
//	{
//		String urlASCII = "http://svndicf.lukas/svn/docrepos/K3/20_AMSTransition/40_TA/60_WorkingMaterials/Szkolenia w�asne.doc";
//		try
//		{
//			System.out.println("1." + urlASCII);
//			urlASCII = new URI(urlASCII).toASCIIString();
//			System.out.println("2." + urlASCII);
//		}
//		catch (URISyntaxException e)
//		{
//			e.printStackTrace();
//		}
//
//	}
}
