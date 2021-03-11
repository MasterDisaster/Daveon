<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="pl.web.util.Daveon"%>
<%@page import="pl.daveon.core.DaveonSearcher"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link href="style/daveon.css" rel="stylesheet" type="text/css">
<TITLE>Daveon</TITLE>
</head>

<style>
.resultTR{
background: rgb(252,252,252); /* Old browsers */
/* IE9 SVG, needs conditional override of 'filter' to 'none' */
background: url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/Pgo8c3ZnIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgdmlld0JveD0iMCAwIDEgMSIgcHJlc2VydmVBc3BlY3RSYXRpbz0ibm9uZSI+CiAgPGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVjZ2ctZ2VuZXJhdGVkIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9IjAlIiB5MT0iMCUiIHgyPSIwJSIgeTI9IjEwMCUiPgogICAgPHN0b3Agb2Zmc2V0PSI5MCUiIHN0b3AtY29sb3I9IiNmY2ZjZmMiIHN0b3Atb3BhY2l0eT0iMSIvPgogICAgPHN0b3Agb2Zmc2V0PSIxMDAlIiBzdG9wLWNvbG9yPSIjZWFlZGZjIiBzdG9wLW9wYWNpdHk9IjEiLz4KICA8L2xpbmVhckdyYWRpZW50PgogIDxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9InVybCgjZ3JhZC11Y2dnLWdlbmVyYXRlZCkiIC8+Cjwvc3ZnPg==);
background: -moz-linear-gradient(top, rgba(252,252,252,1) 90%, rgba(234,237,252,1) 100%); /* FF3.6+ */
background: -webkit-gradient(linear, left top, left bottom, color-stop(90%,rgba(252,252,252,1)), color-stop(100%,rgba(234,237,252,1))); /* Chrome,Safari4+ */
background: -webkit-linear-gradient(top, rgba(252,252,252,1) 90%,rgba(234,237,252,1) 100%); /* Chrome10+,Safari5.1+ */
background: -o-linear-gradient(top, rgba(252,252,252,1) 90%,rgba(234,237,252,1) 100%); /* Opera 11.10+ */
background: -ms-linear-gradient(top, rgba(252,252,252,1) 90%,rgba(234,237,252,1) 100%); /* IE10+ */
background: linear-gradient(to bottom, rgba(252,252,252,1) 90%,rgba(234,237,252,1) 100%); /* W3C */
filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#fcfcfc', endColorstr='#eaedfc',GradientType=0 ); /* IE6-8 */
}

.resultTRreverse {
background: rgb(238,238,238); /* Old browsers */
/* IE9 SVG, needs conditional override of 'filter' to 'none' */
background: url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/Pgo8c3ZnIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgdmlld0JveD0iMCAwIDEgMSIgcHJlc2VydmVBc3BlY3RSYXRpbz0ibm9uZSI+CiAgPGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVjZ2ctZ2VuZXJhdGVkIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9IjAlIiB5MT0iMCUiIHgyPSIwJSIgeTI9IjEwMCUiPgogICAgPHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2VlZWVlZSIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgICA8c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNlZmVmZWYiIHN0b3Atb3BhY2l0eT0iMSIvPgogIDwvbGluZWFyR3JhZGllbnQ+CiAgPHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjEiIGhlaWdodD0iMSIgZmlsbD0idXJsKCNncmFkLXVjZ2ctZ2VuZXJhdGVkKSIgLz4KPC9zdmc+);
background: -moz-linear-gradient(top, rgba(238,238,238,1) 0%, rgba(239,239,239,1) 100%); /* FF3.6+ */
background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,rgba(238,238,238,1)), color-stop(100%,rgba(239,239,239,1))); /* Chrome,Safari4+ */
background: -webkit-linear-gradient(top, rgba(238,238,238,1) 0%,rgba(239,239,239,1) 100%); /* Chrome10+,Safari5.1+ */
background: -o-linear-gradient(top, rgba(238,238,238,1) 0%,rgba(239,239,239,1) 100%); /* Opera 11.10+ */
background: -ms-linear-gradient(top, rgba(238,238,238,1) 0%,rgba(239,239,239,1) 100%); /* IE10+ */
background: linear-gradient(to bottom, rgba(238,238,238,1) 0%,rgba(239,239,239,1) 100%); /* W3C */
filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#eeeeee', endColorstr='#efefef',GradientType=0 ); /* IE6-8 */
}
</style>

<script type="text/javascript">
	var submitted=false;
	
	function pager(pagerValue)
	{
		self.document.f.pager.value = pagerValue;
		self.document.f.submit();
	}
	
	function getRadioValue(radioObj)
	{
  	    var radioLength = radioObj.length;

		for (var i=0; i < radioLength; i++)
		   {
			 if (radioObj[i].checked)
				{
			      return radioObj[i].value;
		      	}
		   }
		   return "";
	}

	function submitForm()
	{
		if (submitted == true) {
			alert ("form already submitted") ;
			return false;
		}
		else {
			self.document.f.btnG.disabled = true;		
			self.document.f.pager.value=1;
			submitted = true ;
			return true;			
		}	
	}
</script>
<%
	long startTime = System.currentTimeMillis();

	pl.web.util.SearchResult res = pl.web.util.Daveon.search(request);
	java.lang.String queryValue = res.getQuery();
	if (queryValue == null) queryValue = "";
%>

<body>
<CENTER>

<FORM name="f" action="index.jsp" onsubmit="return submitForm()">
	<input type="hidden" name="pager" value="<%=res.getPager()%>" />
<TABLE cellSpacing="0" cellPadding="0" width="100%" >
	<TBODY>
		<TR vAlign="top">
			<TD align="center">
			<div class="Daveon">&nbsp;</div>
			</TD>
		</TR>
		<TR vAlign="top">
			<TD noWrap align="center"><INPUT type="hidden" value="pl"
				name="hl"> <div><INPUT style="background-color:#FFFFF0;border-style:outset;height:120%;border-image:url(images/searchHeaderBar.gif) 10 10 stretch;" title="Szukaj z Daveon" maxLength=2048
				size=100 name=query value="<%= queryValue.replaceAll("\"","&quot;") %>"><BR /></div>
			</TD>
		</TR>				

		<TR vAlign="top" style="top:-10px;">
			<TD noWrap align="center">
			<div >
				<button id="gbqfba" aria-label="Szukaj z Daveon" name="btnG" class="gbqfba">
					<span id="gbqfsa">Szukaj z Daveon</span>
				</button>
			</div>
			</TD>
		</TR>

		<TR vAlign="middle">
			<TD noWrap align="center" class="options" >
				<INPUT type="radio" name="radios" onClick="this.form.action='index.jsp';this.form.submit()"  value="<%=DaveonSearcher.OPT_RELEVANCE %>" <%= (res.getSortType().equals("" + DaveonSearcher.OPT_RELEVANCE) ? "checked" : "") %> > Najlepiej pasujące do zapytania</INPUT>
				<INPUT type="radio" name="radios" onClick="this.form.action='index.jsp';this.form.submit()"  value="<%=DaveonSearcher.OPT_LASTMODIFIED %>" <%= (res.getSortType().equals("" + DaveonSearcher.OPT_LASTMODIFIED) ? "checked" : "") %> > Najświeższe (last modified) </INPUT>
				<INPUT type="radio" name="radios" onClick="this.form.action='index.jsp';this.form.submit()"  value="<%=DaveonSearcher.OPT_LASTMODIFIEDREVERT %>" <%= (res.getSortType().equals("" + DaveonSearcher.OPT_LASTMODIFIEDREVERT) ? "checked" : "") %> > Najstarsze (odwrócone last modified) </INPUT> 
				<BR/>
			</TD>
		</TR>
				
		<TR vAlign=top height="30px">
			<TD noWrap align="center">&nbsp;</TD>
		</TR>
		<TR>
			<TD>
			<TABLE cellSpacing=0 cellPadding=0 border="0" class="result">
				<TBODY>
					<TR>
						<TH>
						<span class="resultNoBold" style="float: left; margin-left:5px;"><a href="https://svn.lukas/svn/docrepos/K2-Docs/TA/Applications/Lucene.tutorial.docx">Daveon Help</a></span>
						<span class="resultNoBold">Liczba rekordów: </span><b><%=res.getCount()%></b>
						<span class="resultNoBold">Czas: </span><%=res.getTime()%> 
						<span class="resultNoBold">Zapytanie: </span><%=queryValue%>
						<span class="resultNoBold"><%=res.getIndexInfoAdd()%></span>
						</TH>
					</TR>
					<TR vAlign=top height="30px">
						<TD noWrap align="center">&nbsp;</TD>
					</TR>

					<TR vAlign=top height="30px">
						<TD noWrap align="center" >
						<%
							if (res.getPager() > 1)
							{
						%> <a href="javascript:pager(<%=res.getPager() - 1%>)" class="navi_word">Poprzednia</a>
						<%
						}
						%> <%
 	int count = Integer.parseInt(res.getCount());

 	// sprawdzam ile bedzie numerkow/zakladek do klikniecia 
 	int pages = count / Daveon.RESULTS_PER_PAGE;
 	int showedPages = 0;

 	if (count > Daveon.RESULTS_PER_PAGE)
 	{ // jesli jest stron>1 to je dzielimy na zakladki 
 		int counter = 1;

 		for (int i = 0; i < count; i++)
 		{
 			if (i % Daveon.RESULTS_PER_PAGE == 0)
 			{ // pokazujemy kazda strone modulo zalozona ilosc
 		// jesli aktualnie wybrana strona jest poza lewym zakresem to jej nie pokazujemy  
 		if ((counter >= res.getPager() - Daveon.PAGES_LEFT)
 				&& (counter <= res.getPager() + Daveon.PAGES_RIGHT))
 		{
 			if (counter == res.getPager())
 			{
 %> <%=counter%> <%
 			} else
 			{
 %> <a href="javascript:pager(<%=counter%>)" class="navi_word"><%=counter%></a> <%
 		}
 		}
 		counter++;
 		showedPages++;
 			}
 		}
 	}
 %> <%
 	if (res.getPager() < pages + 1)
 	{
 %> <a href="javascript:pager(<%=res.getPager() + 1%>)" class="navi_word">Następna</a> <%
 }
 %>
						</TD>
					</TR>
					<%
						boolean isEven = false;
						for (java.util.Iterator it = res.getElements().iterator(); it.hasNext();)
						{
							com.lukas.SearchElement elem = (com.lukas.SearchElement) it.next();
							isEven=!isEven;
					%>
					<TR vAlign=top class="<% if(isEven) {%>resultTR<%}else{%>resultTRreverse<%}%>">
						<TD class="resultTD" height="60px">
							<p style="margin:10px 10px">
								<span style="float:left;">
									<a class="ftype" <%if(elem.getFileType().length()>4){%>style="width: <%= (elem.getFileType().length()*8) %>px;"<%}%> ><%=elem.getFileType().toUpperCase()%></a>
									<a style="height: 29px;font-size: 15px;line-height: 20px;font-family: Arial;border-radius: 2px;" href="<%=elem.getUrlHref()%>"> <%	if(elem.getDocTitle().equalsIgnoreCase(elem.getFileName()))
																			{%><%=elem.getFileName()%><%}
																			else {
																			%><%=elem.getFileName()%>: Tytuł: <%=elem.getDocTitle()%><%}%></a>
								<span>
							<!--/p-->
							<br/>
							<!--p-->
							<span style="float: left;"><a class="superlink" href="<%=elem.getUrlHref()%>"><%=elem.getUrl()%></a></span>
							<!--/p-->
							<br/>
							<!--p style="float: left;margin:5px;"-->
								<%=elem.getDesc()%>
							<!--/p-->
							<br/><!--br/-->
							<!--p style="float: left;margin:5px;"-->
							<span class="resultdate" style="float: left" ><%=elem.getModified() %></span>
							</p>
							<!--/p-->
						</TD>
					</TR>
					<%
					}
					%>
				</TBODY>
			</TABLE>
			</TD>
		</TR>
		<%
			long stopTime = System.currentTimeMillis();
			long time = (stopTime - startTime) / 1000;
		%>
		<TR vAlign="bottom">
			<TD align="center">
			<%
							if (res.getPager() > 1)
							{
						%> <a href="javascript:pager(<%=res.getPager() - 1%>)" class="navi_word">Poprzednia</a>
						<%
						}
						%> <%
 	count = Integer.parseInt(res.getCount());

 	// sprawdzam ile bedzie numerkow/zakladek do klikniecia 
 	pages = count / Daveon.RESULTS_PER_PAGE;
 	//int showedPages = 0;

 	if (count > Daveon.RESULTS_PER_PAGE)
 	{ // jesli jest stron>1 to je dzielimy na zakladki 
 		int counter = 1;

 		for (int i = 0; i < count; i++)
 		{
 			if (i % Daveon.RESULTS_PER_PAGE == 0)
 			{ // pokazujemy kazda strone modulo zalozona ilosc
 		// jesli aktualnie wybrana strona jest poza lewym zakresem to jej nie pokazujemy  
 		if ((counter >= res.getPager() - Daveon.PAGES_LEFT)
 				&& (counter <= res.getPager() + Daveon.PAGES_RIGHT))
 		{
 			if (counter == res.getPager())
 			{
 %> <%=counter%> <%
 			} else
 			{
 %> <a href="javascript:pager(<%=counter%>)" class="navi_word"><%=counter%></a> <%
 		}
 		}
 		counter++;
 		showedPages++;
 			}
 		}
 	}
 %> <%
 	if (res.getPager() < pages + 1)
 	{
 %> <a href="javascript:pager(<%=res.getPager() + 1%>)" class="navi_word">Następna</a> <%
 }
 %>
			</TD>
		</TR>
		
		<TR vAlign="bottom">
			<TD align="center">
			<div class="footer">&copy; 2013 Błażej Kaczmarek &nbsp;&nbsp;&nbsp;&nbsp;
			(Czas generowania strony: <%=time%> sek.)</div>
			</TD>
		</TR>
	</TBODY>
</TABLE>

</FORM>
</CENTER>
</body>
</html>
