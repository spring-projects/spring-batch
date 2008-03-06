<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="yes" encoding="UTF-8"
		indent="yes" />
	<xsl:param name="project" />
	<xsl:template match="/report/data/all">
		<xsl:element name="project">
			<xsl:attribute name="name"><xsl:value-of select="$project" />
			</xsl:attribute>
			<xsl:apply-templates select="coverage" />
		</xsl:element>
	</xsl:template>
	<xsl:template match="coverage">
		<xsl:copy-of select="." />
	</xsl:template>
</xsl:stylesheet>
