package pl.web.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DaveonListener
	implements ServletContextListener
{
	private static final String INDEX_DIR_NAME_INIT_PARAM 	= "index-dir";
	private static final String DOCUMENTS_PREFIX			= "prefix-url";
	private static final String URL_SUBSTITUTE				= "substitute-url";

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent sce)
	{
		String indexDir = sce.getServletContext().getInitParameter(INDEX_DIR_NAME_INIT_PARAM);
		String prefixURL= sce.getServletContext().getInitParameter(DOCUMENTS_PREFIX);
		String subsURL	= sce.getServletContext().getInitParameter(URL_SUBSTITUTE);
		Daveon.indexDir = indexDir;
		Daveon.prefixURL= prefixURL;
		Daveon.prefixURLreplacement=subsURL;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent sce)
	{
	}
}
