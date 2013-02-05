<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- Edited by XMLSpy® -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
  <html>
  <body>
  <xsl:for-each select="//breakage/bundle">
     <h2>API Breakage Analysis for component <xsl:value-of select="@componentID"/></h2>
     <xsl:for-each select="category">
	<h3>Errors and warnings related to <xsl:value-of select="@value"/>:  <xsl:value-of select="count(api_problems/api_problem)"/></h3>
		<table border="1" width="80%">
		<tr>
			<td>Severity</td>
			<td>Line</td>
			<td>Message</td>
		</tr>

	      <xsl:for-each select="api_problems/api_problem">
		<tr>
			<xsl:if test="@severity = 0">
				<td bgcolor="#00cc00"><xsl:value-of select="@severity"/></td>
			</xsl:if>
			<xsl:if test="@severity = 1">
				<td bgcolor="#cccc00"><xsl:value-of select="@severity"/></td>
			</xsl:if>
			<xsl:if test="@severity = 2">
				<td bgcolor="#cc0000"><xsl:value-of select="@severity"/></td>
			</xsl:if>
		<td><xsl:value-of select="@linenumber"/></td>
		<td><xsl:value-of select="@message"/></td>
		
		</tr>
	      </xsl:for-each>
		</table>
      </xsl:for-each>
      </xsl:for-each>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>


