package pl.daveon.core.tool;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
/**
 * @author bkaczmarek
 *
 *
 *
 */
public class DaveonConcurrentMergeScheduler extends ConcurrentMergeScheduler {

	private static final Logger		statisticsLogger	= Logger.getLogger(DaveonConcurrentMergeScheduler.class);
	private long _constructionTime 			= 0;
	private int 	mergeNumber				= 1;

	public DaveonConcurrentMergeScheduler() {
		super();
		_constructionTime = System.currentTimeMillis();
	}
	
	public static void printRuntimeStats()
	{
		 Runtime runtime = Runtime.getRuntime();
		 int mb = 1024*1024;
		 
		 statisticsLogger.info("##### Heap utilization statistics [MB] #####");
		 statisticsLogger.info("Used Memory:"+ (runtime.totalMemory() - runtime.freeMemory()) / mb);
		 statisticsLogger.info("Free Memory:"+ runtime.freeMemory() / mb);
		 statisticsLogger.info("Total Memory:" + runtime.totalMemory() / mb);
		 statisticsLogger.info("Max Memory:" + runtime.maxMemory() / mb);
	}

	 @Override
	 public void merge(IndexWriter writer) throws IOException {
		 super.merge(writer);

		 	final long _mergeTime = System.currentTimeMillis();
			float diff = (_mergeTime - this._constructionTime )/1000;//ilosc sekund
			int numDocs = 0;

			try
			{
				numDocs = writer.numDocs();
			}
			catch (IllegalStateException ace)
			{
				return;
			}
			statisticsLogger.info("Merge: #"+mergeNumber++);
			float avarege = Math.round(numDocs / diff);

			statisticsLogger.info("Merging-> numDocs: \t" + writer.numDocs());
			statisticsLogger.info("Merging-> TotalSegment(s):" + writer.segString());
			statisticsLogger.info("Merging-> RamSize: \t" + writer.ramSizeInBytes());
			statisticsLogger.info("Merging-> docs/sec: \t" + avarege);
			
			printRuntimeStats();
	 }

}
