package pl.daveon.core;

import pl.daveon.slave.WORKMODE;

/**
 * @author bkaczmarek
 *
 *
 *
 */
public class DaveonCrawler {

	public static void main(String[] args) {
		
		org.apache.log4j.PropertyConfigurator.configure("properties/log4j.properties");


		String docRootLocation 		= args[0];
		String indexlocation 		= (args[1]==null || args[1].isEmpty()) ? "_index":args[1];
		String crawlingInfoLocation = (args[2]==null || args[2].isEmpty()) ? "svn.cycles":args[2];
		String threadsCnt			= args.length>3 ? threadsCnt=args[3]:"";


		DaveonIndexer li = new DaveonIndexer(docRootLocation, indexlocation, WORKMODE.CROWLING_MODE);
		if (!threadsCnt.isEmpty())
				li.setProcessingCoresCount(Integer.valueOf(threadsCnt));
		try {
			/*tryb CROWLING_MODE przeznaczony do aktualizacji indexu*/
			li.setCrawlerLocation(crawlingInfoLocation);

			li.engageIndexing();


		} catch (Exception e) {
			System.out.println("LucyCrawler has Finished with errors: "+e.getMessage());
			System.exit(e.hashCode());
		} catch (Error e) {
			System.out.println("LucyCrawler has Finished with errors: "+e.getMessage());
			System.exit(e.hashCode());
		}

		System.out.println("LucyCrawler finished with success.");
	}

}
