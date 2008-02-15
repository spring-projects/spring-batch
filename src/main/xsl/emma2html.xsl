<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" omit-xml-declaration="yes" encoding="UTF-8"
		indent="yes" />
	<xsl:param name="project" />
	<xsl:param name="version" />
	<xsl:template match="/summary">
		<html>
			<head>
				<meta content="text/html; charset=UTF-8"
					http-equiv="Content-Type" />
				<title><xsl:value-of select="$project" /> Coverage Report</title>
				<style TYPE="text/css">
					@import url("../css/maven-base.css");

					@import url("../css/maven-theme.css");

					@import url("../css/site.css");
				</style>
			</head>
			<body class="composite">
				<div id="banner">
					<a href="http://www.springframework.org/"
						id="bannerLeft">
						<xsl:value-of select="$project" />
					</a>
					<span id="bannerRight">
						<img src="../images/shim.gif" alt="" />
					</span>
					<div class="clear">
						<hr />
					</div>
				</div>
				<h2>
					OVERALL COVERAGE SUMMARY for
					<xsl:value-of select="$project" />
					(
					<xsl:value-of select="$version" />
					)
				</h2>
				<table class="bodyTable">
					<tr class="a">
						<th>name</th>
						<th>class, %</th>
						<th>method, %</th>
						<th>block, %</th>
						<th>line, %</th>
					</tr>
					<xsl:apply-templates select="project" />
				</table>
				<p></p>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="project">
		<tr class="b"> 
			<td>
				<xsl:value-of select="@name" />
			</td>
			<td>
				<xsl:apply-templates
					select="coverage[@type='class, %']" />
			</td>
			<td>
				<xsl:apply-templates
					select="coverage[@type='method, %']" />
			</td>
			<td>
				<xsl:apply-templates
					select="coverage[@type='block, %']" />
			</td>
			<td>
				<xsl:apply-templates select="coverage[@type='line, %']" />
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="coverage">
		<xsl:value-of select="@value" />
	</xsl:template>
</xsl:stylesheet>
