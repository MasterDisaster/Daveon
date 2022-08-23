package pl.daveon.slave;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author bkaczmarek
 *
 *
 *
 */
public class CyclesCrawler extends ArrayList<String>{

	public final String CROWLER_PATTERN = "^(docrepos.update.[0-9]{8}-[0-9]{4}\\.rev-)([0-9]{5,})(\\.log)$";

	/**
	 *
	 */
	private static final long serialVersionUID = 4035325530162657437L;
	private int _latestRevision 	= 0;


	/**
	 * @param cyclesDir - katalog z plikami svn'owymi
	 * @throws FileNotFoundException
	 */
	public CyclesCrawler(String cyclesDir, String rootLocation) throws FileNotFoundException
	{
		super();
		smackTheFile(cyclesDir, rootLocation);
	}

	/**
	 * @param filename - katalog z plikami update'u svn'owego
	 * @return - liste plikow zmienionych przez svn'a - Dodanych (A), Usunietych(D), Zmodyfikowanych/Aktualizowanych (U)
	 * W przypadku usunietych - jesli pliku nie ma na FS to po prostu trzba go usunac z indexu. sprawdzanie
	 * czy pik istnieje i tak nalezy wykona przy probie jego indeksacji
	 */
	private void smackTheFile(String filename, String rootLocation) throws FileNotFoundException
	{
		File fFile = new File(filename);
		File[] filesList = null;
		this.clear();

		if(fFile.exists() && fFile.isDirectory())
			filesList = fFile.listFiles(new FilenameFilter(){

				@Override
				public boolean accept(File dir, String name) {

					String regex = CROWLER_PATTERN;
					Pattern pt = Pattern.compile(regex);
					Matcher mt = pt.matcher(name);
					boolean res = mt.find();
					if(res)
					{
						int val = 0;
						try
						{
							val = Integer.valueOf(mt.group(2));
						}
						catch(NumberFormatException nfe)
						{
							val = -1;
						}
						_latestRevision = Math.max(getRevision(), val);
					}

					return res;
				}
			});
		else
			throw new FileNotFoundException("Location/Directory doesn't exists: "+filename);

		fFile = null;
		if (filesList!=null)
		{
			for(File mf : filesList)
			{
				Scanner scanner = null;
				try {

					scanner = new Scanner(new FileReader(mf));
				}
				catch (FileNotFoundException e){e.printStackTrace();}

				Pattern p = Pattern.compile("^([ADU]{1}\\s{1})(.*)(\\.[^.]+)$", Pattern.CASE_INSENSITIVE);
				try
				{
			      while ( scanner.hasNextLine() )
			      {
			    	String _tmp= scanner.nextLine();

			  		Matcher m = p.matcher(_tmp);
			  		boolean found = m.find();
			  		if(found)
			  		{
				  		String _var = m.group(2)+m.group(3);
				  		if (!this.contains(_var))
				  			this.add(rootLocation+"/"+_var.trim());
			  		}
			      }
			    }
			    finally {
			      scanner.close();
			    }
			}
		}
	}


	/**
	 * @return zwraca numer rewizji ostatniego pliku update'u svn'owego
	 */
	public int getRevision() {
		return _latestRevision;
	}


	public static void main(String[] args)
	{

		CyclesCrawler cc = null;
		try {
			if(args!=null && args.length>1)
			{
				cc = new CyclesCrawler(args[0], args[1]);
				
				System.out.println("Rewizja: "+cc.getRevision());
				System.out.println("DocumentsCount: "+cc.size());
			}
			else
				System.out.println("Zbyt ma³o parametrów.\r\n args[0] - Katalog z plikami werjsciowymi.\r\nargs[1] - Documents root");
		} 
		
		catch (FileNotFoundException e) 
		{
			System.out.println("Exception in CyclesCrawler: "+e.getMessage());
			System.exit(e.hashCode());
		}
		


	}

}
